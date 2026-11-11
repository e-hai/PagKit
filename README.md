# PagKit: Tencent libpag for Compose Multiplatform (Android + iOS)

高性能 Tencent `libpag` 动效引擎的 **Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP)** 双端原生互操作封装库，提供纯 Kotlin / 声明式 Composable API。

---

## 🌟 核心特性

- 🚀 **双端原生硬件加速**：
  - **Android**：集成 `com.tencent.tav:libpag:4.5.102`，通过 `AndroidView` 互操作包装原生 `PAGView` 与 `PAGImageView`；
  - **iOS**：集成官方预编译 `libpag.xcframework`（`v4.5.102`），通过 `cinterop` 与 `UIKitView` 零胶水直连 Metal 硬件加速渲染。
- 🎬 **双视图渲染支持**：
  - `PagView`：GPU 实时渲染，适用于大尺寸、复杂动效、实时图文编辑场景；
  - `PagImageView`：序列帧位图内存/磁盘缓存渲染，专为 `LazyColumn` / `LazyVerticalGrid` 列表高频复用优化。
- 📦 **多数据源统一加载 (`PagSource`)**：
  - `PagSource.FilePath`：从本地文件路径加载；
  - `PagSource.Asset`：从 Android Assets 或 iOS Bundle 资源加载；
  - `PagSource.Bytes`：从内存字节数组加载（无缝对接 Compose Resources 的 `Res.readBytes(...)`）；
  - `PagSource.Url`：从远程 HTTP/HTTPS URL 异步下载加载；
  - `PagSource.Composition`：直接挂载自定义多动效合成容器。
- 🎨 **动态图层编辑**：
  - 支持按索引或图层名替换占位图片（`replaceImage` / `replaceImageByName`）；
  - 支持替换文本图层（`replaceText`：文案内容、字号、填充颜色、描边粗细与颜色、仿粗体/斜体、对齐方式），支持 `pagState.flush()` 实时刷新。
- 🧩 **多 PAG 组合与时间轴编排 (`PagComposition`)**：
  - 支持 `PagComposition.make(w, h)` 创建独立合成画布；
  - 动态添加/移除/交换子图层；
  - 调整子图层的变换矩阵（`setTranslation` / `setScale` / `setRotation`）；
  - 支持时间轴偏移（`startTimeUs`）错峰播放、当前进度与可见性开关（`visible`）。
- ⚡ **离线优先与磁盘缓存 (`PagDiskCache`)**：
  - 自动管理 URL 网络动效的本地磁盘缓存；
  - 支持配置缓存上限大小（`setMaxDiskSize`）与一键清理缓存（`removeAll`）。

---

## 📁 项目工程结构

```
PagKit/
├── pagkit/                       # 核心 KMP/CMP 库模块
│   ├── libpag-ios/               # iOS 官方预编译 libpag.xcframework
│   ├── src/
│   │   ├── commonMain/           # 声明式 API (PagView, PagState, PagFile, PagComposition, PagSource...)
│   │   ├── androidMain/          # Android 平台实现 (包装 org.libpag.PAGView / PAGImageView)
│   │   ├── iosMain/              # iOS 平台实现 (cinterop 包装 Metal PAGView / PAGImageView)
│   │   └── commonTest/           # 跨端单元测试
│   └── build.gradle.kts
├── sample/
│   ├── shared/                   # 跨端示例 UI (包含 4 大功能实验室 Tab)
│   ├── androidApp/               # Android 演示宿主工程
│   └── iosApp/                   # iOS 演示宿主工程 (XcodeGen + SwiftUI)
├── gradle/
└── settings.gradle.kts
```

---

## 📦 安装与引入 (GitHub Packages)

### 1. 配置 Maven 仓库

在你的根目录 `settings.gradle.kts` 中添加 GitHub Packages Maven 源：

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

### 2. 引入依赖

在业务模块的 `build.gradle.kts` 中添加：

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.e-hai:pagkit:0.1.0")
        }
    }
}
```

---

## 💻 快速开始

### 1. 基础播放控制 (`PagView`)

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.pagkit.compose.PagView
import io.github.pagkit.compose.rememberPagState
import io.github.pagkit.model.PagScaleMode
import io.github.pagkit.model.PagSource

@Composable
fun SimplePagDemo(pagBytes: ByteArray) {
    // 声明状态并配置播放参数
    val state = rememberPagState(
        source = PagSource.Bytes(pagBytes),
        autoPlay = true,
        repeatCount = 0, // 0 表示无限循环，1 表示单次播放
        scaleMode = PagScaleMode.LetterBox
    )

    PagView(
        state = state,
        modifier = Modifier.fillMaxSize(),
        onAnimationStart = { println("开始播放") },
        onAnimationEnd = { println("播放结束") }
    )

    // 控制 API
    // state.play()
    // state.pause()
    // state.stop()
    // state.seekTo(0.5) // 跳转到 50% 进度
}
```

---

### 2. 图层动态编辑 (替换文字与图片)

```kotlin
@Composable
fun DynamicEditDemo(pagBytes: ByteArray) {
    val state = rememberPagState(source = PagSource.Bytes(pagBytes))

    PagView(
        state = state,
        modifier = Modifier.fillMaxSize(),
        onCompositionLoaded = { pagFile ->
            // 1. 替换文本图层
            if (pagFile.numTexts > 0) {
                pagFile.replaceText(
                    editableTextIndex = 0,
                    textData = PagTextData(
                        text = "VIP 会员专享",
                        fontSize = 32f,
                        fillColorArgb = 0xFFFFD700.toInt(), // 金色
                        fauxBold = true
                    )
                )
            }

            // 2. 替换图片占位
            if (pagFile.numImages > 0) {
                val newImage = PagImage.fromPath("/path/to/avatar.png")
                pagFile.replaceImage(0, newImage)
            }

            // 3. 立即刷新渲染
            state.flush()
        }
    )
}
```

---

### 3. 多 PAG 组合与时间轴编排 (`PagComposition`)

```kotlin
fun createMultiLayerComposition(file1Bytes: ByteArray, file2Bytes: ByteArray): PagComposition {
    val layer1 = PagFile.loadFromBytes(file1Bytes)!!
    val layer2 = PagFile.loadFromBytes(file2Bytes)!!

    val composition = PagComposition.make(width = 720, height = 720)

    // 变换图层 1：左移、缩放 0.8
    layer1.setScale(0.8f, 0.8f)
    layer1.setTranslation(-100f, 0f)

    // 变换图层 2：右移、延迟 600ms 播放
    layer2.setScale(0.8f, 0.8f)
    layer2.setTranslation(100f, 0f)
    layer2.startTimeUs = 600_000L // 600ms

    composition.addLayer(layer1)
    composition.addLayer(layer2)

    return composition
}

// 在 Compose 中挂载
val pagState = rememberPagState(source = PagSource.Composition(myComposition))
```

---

### 4. 列表高性能复用 (`PagImageView`)

```kotlin
@Composable
fun FastListDemo(urls: List<String>) {
    LazyColumn {
        items(urls) { url ->
            val itemState = rememberPagState(
                source = PagSource.Url(url),
                autoPlay = true,
                repeatCount = 0
            )
            PagImageView(
                state = itemState,
                modifier = Modifier.size(80.dp),
                cacheAllFramesInMemory = true // 开启序列帧内存缓存
            )
        }
    }
}
```

---

## 🛠️ 编译与验证

### 1. 跨平台单元测试
```bash
./gradlew :pagkit:testDebugUnitTest :pagkit:iosSimulatorArm64Test
```

### 2. Android 演示应用打包
```bash
./gradlew :sample:androidApp:assembleDebug
```

### 3. iOS 演示应用工程与构建
```bash
# 生成 iOS Simulator Framework
./gradlew :sample:shared:linkDebugFrameworkIosSimulatorArm64

# 生成 Xcode 工程并编译
cd sample/iosApp
xcodegen generate
xcodebuild -project iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO
```

---

## 🚀 发布到 GitHub Packages

### 方式一：GitHub Actions 自动化发布（推荐）

直接为项目打上版本 Tag 并推送至 GitHub：
```bash
git tag v0.1.0
git push origin v0.1.0
```
或前往 GitHub 仓库的 **Actions** -> **Publish to GitHub Packages** 点击 **Run workflow** 手动触发。CI 将自动构建全平台 Publication（包含 Android Release AAR、iOS arm64 / simulator klib、通用 metadata）并发布到 GitHub Packages。

### 方式二：本地命令行发布

配置具有 `write:packages` 权限的 GitHub Personal Access Token：
```bash
GITHUB_TOKEN=ghp_your_token ./gradlew :pagkit:publishAllPublicationsToGitHubPackagesRepository
```
