plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

group = "io.github.e-hai"
version = project.findProperty("version")?.toString()?.takeIf { it != "unspecified" } ?: "0.1.0"

val xcframeworkPath = file("libpag-ios/libpag.xcframework")

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        val sliceDir = if (iosTarget.name.contains("Simulator")) {
            "ios-arm64_x86_64-simulator"
        } else {
            "ios-arm64"
        }
        val frameworkDir = file("$xcframeworkPath/$sliceDir")

        iosTarget.binaries.all {
            linkerOpts(
                "-F${frameworkDir.absolutePath}",
                "-framework", "libpag",
                "-lc++",
                "-rpath", frameworkDir.absolutePath
            )
        }

        iosTarget.compilations.getByName("main") {
            cinterops.create("libpag") {
                defFile("src/nativeInterop/cinterop/libpag.def")
                extraOpts(
                    "-compiler-option", "-F${frameworkDir.absolutePath}",
                    "-compiler-option", "-fmodules"
                )
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
        }

        androidMain.dependencies {
            api(libs.libpag.android)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "io.github.pagkit"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/e-hai/PagKit")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR") ?: "e-hai"
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
