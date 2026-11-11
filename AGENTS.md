# PagKit: AI Agent & Developer Internal Engineering Guide (AGENTS.md)

This document is the authoritative technical reference and architectural playbook for **AI Agents** and core contributors working on the **PagKit** repository. It contains architectural invariants, source topology, lifetime management rules, and debugging protocols.

---

## 1. Project Mission & Design Philosophy

**PagKit** encapsulates Tencent's native `libpag` animation engine (`4.5.102`) into idiomatic Kotlin Multiplatform (KMP) and Compose Multiplatform (CMP) for **Android** and **iOS**.

### Core Invariants:
1. **Zero Leaked Native Pointers**: The `commonMain` API must NEVER expose Android JNI pointers, ObjC handles, or platform classes (`PAGView`, `UIView`, `Bitmap`, etc.). Everything in `commonMain` is pure Kotlin / CMP primitives (`PagState`, `PagSource`, `PagFile`, `PagComposition`, `PagLayer`, `PagImage`, `PagTextData`).
2. **Declarative State with Imperative Precision**: Support declarative Compose UI bindings (`state.isPlaying`, `state.progress`) while providing immediate imperative commands (`state.play()`, `state.pause()`, `state.stop()`, `state.seekTo()`, `state.flush()`).
3. **Deterministic Memory & Weak-Ref Safety**: Native animation engines hold weak references to listeners. Kotlin/Native ARC / GC requires explicit strong retention on the Kotlin side to avoid silent drops or dangling pointers.

---

## 2. Source Code Topology

```
PagKit/
├── pagkit/                                    # Core Library Module
│   ├── build.gradle.kts                       # Multiplatform setup, cinterop, maven-publish
│   ├── libpag-ios/libpag.xcframework/         # Official v4.5.102 binary (8MB without dSYMs)
│   └── src/
│       ├── commonMain/kotlin/io/github/pagkit/
│       │   ├── compose/
│       │   │   ├── PagView.kt                 # expect fun PagView & PagImageView
│       │   │   └── PagState.kt                # State machine & rememberPagState()
│       │   ├── composition/
│       │   │   ├── PagComposition.kt          # expect class PagComposition
│       │   │   ├── PagFile.kt                 # expect class PagFile
│       │   │   ├── PagLayer.kt                # expect open class PagLayer
│       │   │   ├── PagImage.kt                # expect class PagImage
│       │   │   └── PagDecoder.kt              # expect class PagDecoder
│       │   ├── cache/
│       │   │   └── PagDiskCache.kt            # expect object PagDiskCache
│       │   └── model/
│       │       ├── PagTypes.kt                # PagSource, PagScaleMode, PagLayerType
│       │       ├── PagTextData.kt             # Cross-platform text editing data class
│       │       ├── PagMarker.kt               # Timeline cue point marker (startTimeUs, durationUs, comment)
│       │       └── PagFont.kt                 # expect class PagFont (custom TTF/OTF registration)
│       ├── androidMain/kotlin/io/github/pagkit/
│       │   ├── compose/PagView.android.kt     # AndroidView + org.libpag.PAGView / PAGImageView
│       │   ├── composition/*.android.kt       # JNI wrappers
│       │   ├── cache/PagDiskCache.android.kt  # OkHttp + Android cacheDir + NativePAGDiskCache
│       │   └── PagKitAndroid.kt               # Context initializer
│       ├── iosMain/kotlin/io/github/pagkit/
│       │   ├── compose/PagView.ios.kt         # UIKitView + Metal PAGView / PAGImageView
│       │   ├── composition/*.ios.kt           # Kotlin/Native cinterop wrappers
│       │   └── cache/PagDiskCache.ios.kt      # Darwin engine + NSCachesDirectory + NativePAGDiskCache
│       ├── nativeInterop/cinterop/
│       │   └── libpag.def                     # modules = libpag, package = org.libpag.ios
│       └── commonTest/kotlin/io/github/pagkit/PagKitTest.kt
├── sample/
│   ├── shared/                                # CMP Shared UI (4 Showcase Tabs)
│   │   └── src/commonMain/kotlin/io/github/pagkit/sample/ui/
│   │       ├── BasicPlaybackTab.kt            # Play/pause/stop, seekTo, ScaleMode, repeatCount
│   │       ├── DynamicEditTab.kt              # replaceText, replaceImage, alpha, scale
│   │       ├── CompositionLabTab.kt           # PagComposition.make, addLayer, swapLayerAt, startTimeUs
│   │       └── NetworkAndListTab.kt           # Url download, PagDiskCache, PagImageView grid
│   ├── androidApp/                            # Android runner (:sample:androidApp)
│   └── iosApp/                                # Xcode runner (project.yml + xcodegen)
└── .github/workflows/publish.yml              # macOS-15 CI for GitHub Packages
```

---

## 3. Critical Implementation Invariants (Do Not Violate)

### 1. PagState Stability & Reconnection Rule
- **The Trap**: Writing `val state = remember(source) { PagState(...) }` causes `PagState` to be destroyed and recreated whenever `source` finishes async loading from `null` to `PagSource.Bytes`. This breaks bindings to any buttons holding the previous `state` instance.
- **The Law**: Always use `val state = remember { PagState(...) }` and synchronize dynamic source changes via `SideEffect`:
  ```kotlin
  SideEffect {
      if (source != null && state.source != source) {
          state.source = source
      }
  }
  ```

### 2. Dual-Track State Reconciliation (Update Block)
- **The Trap**: Only binding `state.playAction` is vulnerable if Compose triggers recomposition while playback state changes.
- **The Law**: In both `PagView.android.kt` and `PagView.ios.kt`:
  1. `bindPagViewActions(view, state)` must be invoked in both `factory` AND `update`.
  2. The `update` block must check `if (view.isPlaying != state.isPlaying)` to ensure self-healing bidirectional state sync.
  3. When `play()` is called after reaching the end (`progress >= 1.0`), progress must automatically reset to `0.0` before restarting.

### 3. iOS Weak Listener Retention
- **The Trap**: `PAGView.addListener:` holds a `__weak id<PAGViewListener>`. A Kotlin object passed as a listener will be GC'd immediately once the local scope exits.
- **The Law**: Always store the listener instance into `state.nativeListener` to keep a strong reference alive on the Kotlin side. Release it only inside `onRelease`:
  ```kotlin
  val listener = IosPagViewListener(...)
  currentState.nativeListener = listener
  view.addListener(listener)
  ```

### 4. Dirty Rendering Principle (Explicit Flush)
- **The Trap**: Changing `layer.alpha`, `layer.setScale()`, `layer.visible`, or calling `file.replaceText()` while the animation is paused does not automatically update the screen.
- **The Law**: Whenever modifying layer properties or calling `seekTo()`/`stop()`, immediately trigger `view.flush()` (or `state.flush()`).

### 5. Compose Multiplatform 1.8.0 iOS Plist Rule
- **The Trap**: CMP 1.8.0 `PlistSanityCheck` throws `kotlin.error()` during startup if `CADisableMinimumFrameDurationOnPhone` is missing from `Info.plist`.
- **The Law**: Every iOS host app MUST declare `<key>CADisableMinimumFrameDurationOnPhone</key><true/>`. In `MainViewController.kt`, defensive guard `enforceStrictPlistSanityCheck = false` is maintained.

---

## 4. cinterop & iOS Build Setup

### 1. `libpag.def`
```ini
language = Objective-C
modules = libpag
package = org.libpag.ios
```
> **Warning**: Do NOT add redundant `headers = ...` when using `modules = libpag` with `-fmodules`, or clang will error with `cinterop doesn't support having headers in -fmodules mode`.

### 2. Linker Settings (`pagkit/build.gradle.kts`)
```kotlin
iosTarget.binaries.all {
    linkerOpts(
        "-F${frameworkDir.absolutePath}",
        "-framework", "libpag",
        "-lc++",
        "-rpath", frameworkDir.absolutePath
    )
}
```

### 3. Framework Stripping Rule
`libpag.xcframework` contains `.dSYM` directories exceeding 118MB. To avoid GitHub's 100MB single-file hard limit, `.dSYM` files must remain deleted from the repository. The framework binaries themselves are only ~2MB (device) and ~5MB (simulator).

---

## 5. Agent Verification Commands

Always run these verification commands before delivering code modifications:

```bash
# 1. Run unit & simulator tests across KMP targets
./gradlew :pagkit:testDebugUnitTest :pagkit:iosSimulatorArm64Test

# 2. Build Android Sample App
./gradlew :sample:androidApp:assembleDebug

# 3. Link iOS Simulator Framework for CMP
./gradlew :sample:shared:linkDebugFrameworkIosSimulatorArm64

# 4. Build iOS Xcode Project
cd sample/iosApp && xcodegen generate
xcodebuild -project iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build CODE_SIGNING_ALLOWED=NO

# 5. Dry-run publish to local Maven
./gradlew :pagkit:publishToMavenLocal
```

---

## 6. How to Extend PagKit

- **Adding a new PAG filter / effect**:
  1. Add model declaration in `commonMain/.../PagTypes.kt`.
  2. Implement JNI call in `PagLayer.android.kt`.
  3. Implement cinterop call in `PagLayer.ios.kt`.
  4. Always add test cases in `PagKitTest.kt`.
- **Adding Audio Playback Hooks**:
  1. Leverage `PagComposition.audioBytes()` and `audioStartTime()`.
  2. Bridge to `AudioTrack` on Android and `AVAudioPlayer` on iOS.

---

## 7. How an AI Agent Should Integrate & Consume PagKit in Downstream Projects

When you (an AI Agent) are tasked with integrating **PagKit** into an external KMP / Compose Multiplatform project, strictly follow this step-by-step procedure.

### Step 1: Configure Maven Repository (`settings.gradle.kts`)
Inject the GitHub Packages Maven repository into the downstream project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/e-hai/PagKit")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR") ?: "e-hai"
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

### Step 2: Add Dependency (`build.gradle.kts`)
In the consumer's shared Compose module (e.g. `:composeApp:build.gradle.kts`):

```kotlin
kotlin {
    compilerOptions {
        // Suppress Kotlin 2.x Beta warning for expect/actual classes
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            implementation("io.github.e-hai:pagkit:0.1.3")
        }
    }
}
```

*(Alternatively, in a monorepo setup, use `api(project(":pagkit"))`)*.

### Step 3: Enforce Platform Prerequisites

#### A. iOS Host App (`Info.plist`)
**Mandatory**: To avoid Compose Multiplatform 1.8.0 startup crashes (`PlistSanityCheck`), inject:
```xml
<key>CADisableMinimumFrameDurationOnPhone</key>
<true/>
```

#### B. iOS Binary Linking (`libpag.xcframework`)
The consumer iOS app target must link and embed `libpag.xcframework`:
- **If using XcodeGen (`project.yml`)**:
  ```yaml
  targets:
    iosApp:
      dependencies:
        - framework: path/to/libpag.xcframework
          embed: true
  ```
- **If using Xcode GUI**: Drag `libpag.xcframework` into **Frameworks, Libraries, and Embedded Content** and select **Embed & Sign**.

#### C. Android ProGuard Rules (`proguard-rules.pro`)
```proguard
-keep class org.libpag.** { *; }
-keep class io.github.pagkit.** { *; }
```

---

## 8. Agent Code Recipes (Zero-Hallucination Snippets)

### Recipe A: Loading & Displaying a PAG Animation
```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.github.pagkit.compose.PagView
import io.github.pagkit.compose.rememberPagState
import io.github.pagkit.model.PagScaleMode
import io.github.pagkit.model.PagSource
// When using Compose Resources (e.g. commonMain/composeResources/files/my_anim.pag):
// import your.project.package.generated.resources.Res

@Composable
fun SimplePagPlayer(pagBytes: ByteArray?) {
    // Law: Always use rememberPagState; it safely re-syncs when pagBytes changes from null to ByteArray
    val state = rememberPagState(
        source = pagBytes?.let { PagSource.Bytes(it) },
        autoPlay = true,
        repeatCount = 0, // 0 = infinite loop, 1 = play once
        scaleMode = PagScaleMode.LetterBox
    )

    PagView(
        state = state,
        modifier = Modifier.fillMaxSize(),
        onAnimationStart = { /* ... */ },
        onAnimationEnd = { /* ... */ }
    )
}

// Typical usage with Compose Resources in parent Composable:
// var bytes by remember { mutableStateOf<ByteArray?>(null) }
// LaunchedEffect(Unit) { bytes = Res.readBytes("files/my_anim.pag") }
// SimplePagPlayer(bytes)
```

### Recipe B: Imperative Playback Controls
```kotlin
Row {
    Button(onClick = { state.play() }) { Text("Play") }
    Button(onClick = { state.pause() }) { Text("Pause") }
    Button(onClick = { state.stop() }) { Text("Stop & Reset") }
}

Slider(
    value = state.progress.toFloat(),
    onValueChange = { state.seekTo(it.toDouble()) },
    valueRange = 0f..1f
)
```

### Recipe C: Dynamic Text and Image Replacement
```kotlin
PagView(
    state = state,
    modifier = Modifier.fillMaxSize(),
    onCompositionLoaded = { pagFile ->
        // 1. Text replacement
        if (pagFile.numTexts > 0) {
            pagFile.replaceText(
                editableTextIndex = 0,
                textData = PagTextData(
                    text = "Welcome VIP",
                    fontSize = 36f,
                    fillColorArgb = 0xFFFFD700.toInt(), // Gold
                    fauxBold = true
                )
            )
        }

        // 2. Image replacement
        if (pagFile.numImages > 0) {
            val img = PagImage.fromBytes(avatarPngBytes)
            pagFile.replaceImage(0, img)
        }

        // Law: Must explicitly flush() to render changes immediately
        state.flush()
    }
)
```

### Recipe D: High-Performance List Recycling (`PagImageView`)
```kotlin
LazyColumn {
    items(urls) { url ->
        val itemState = rememberPagState(
            source = PagSource.Url(url),
            autoPlay = true,
            repeatCount = 0
        )
        // Law: For lists, use PagImageView with cacheAllFramesInMemory = true
        PagImageView(
            state = itemState,
            modifier = Modifier.size(80.dp),
            cacheAllFramesInMemory = true
        )
    }
}
```

---

## 9. Agent Self-Verification Checklist

Before presenting your solution to the user, verify that:
- [ ] You did **NOT** use `remember(source) { PagState(...) }`. (Used `rememberPagState(...)`).
- [ ] You called `state.flush()` after modifying `alpha`, `scale`, `visible`, `replaceText`, or `replaceImage`.
- [ ] You checked `if (pagFile.numTexts > 0)` before calling `replaceText(0, ...)`.
- [ ] You reminded the user about `<key>CADisableMinimumFrameDurationOnPhone</key><true/>` in iOS `Info.plist`.
- [ ] In lists (`LazyColumn`/`LazyRow`), you used `PagImageView` instead of `PagView` for memory safety.

