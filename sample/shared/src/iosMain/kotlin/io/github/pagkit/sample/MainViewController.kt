package io.github.pagkit.sample

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController(
    configure = {
        enforceStrictPlistSanityCheck = false
    }
) {
    SampleApp()
}
