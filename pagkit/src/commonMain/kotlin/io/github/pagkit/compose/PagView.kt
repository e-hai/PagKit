package io.github.pagkit.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.pagkit.composition.PagFile

/**
 * 跨平台高性能 GPU 实时渲染动效组件（AndroidView<PAGView> + UIKitView<PAGView>）。
 * 适用于复杂动效、动态图文修改、时间轴交互等场景。
 */
@Composable
expect fun PagView(
    state: PagState,
    modifier: Modifier = Modifier,
    sync: Boolean = false,
    videoEnabled: Boolean = true,
    cacheEnabled: Boolean = true,
    cacheScale: Float = 1.0f,
    onCompositionLoaded: ((PagFile) -> Unit)? = null,
    onAnimationStart: (() -> Unit)? = null,
    onAnimationEnd: (() -> Unit)? = null,
    onAnimationCancel: (() -> Unit)? = null,
    onAnimationRepeat: (() -> Unit)? = null,
    onAnimationUpdate: ((progress: Double) -> Unit)? = null
)

/**
 * 跨平台轻量序列帧位图缓存动效组件（AndroidView<PAGImageView> + UIKitView<PAGImageView>）。
 * 适合在 LazyColumn / LazyGrid 列表中大量同屏复用的小尺寸动效。
 */
@Composable
expect fun PagImageView(
    state: PagState,
    modifier: Modifier = Modifier,
    cacheAllFramesInMemory: Boolean = false,
    renderScale: Float = 1.0f,
    onCompositionLoaded: ((PagFile) -> Unit)? = null,
    onAnimationStart: (() -> Unit)? = null,
    onAnimationEnd: (() -> Unit)? = null,
    onAnimationCancel: (() -> Unit)? = null,
    onAnimationRepeat: (() -> Unit)? = null,
    onAnimationUpdate: ((progress: Double) -> Unit)? = null
)
