package io.github.pagkit.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.pagkit.composition.PagComposition
import io.github.pagkit.composition.PagFile
import io.github.pagkit.model.PagScaleMode
import io.github.pagkit.model.PagSource

/**
 * 跨平台 PAG 播放器控制器与状态监听器。
 */
@Stable
class PagState(
    initialSource: PagSource? = null,
    initialAutoPlay: Boolean = true,
    initialRepeatCount: Int = 0, // 0 或负数表示无限循环，1 表示单次播放
    initialScaleMode: PagScaleMode = PagScaleMode.LetterBox,
    initialMaxFrameRate: Float = 60f
) {
    /** 绑定的数据源 */
    var source: PagSource? by mutableStateOf(initialSource)

    /** 当前是否正在播放中 */
    var isPlaying: Boolean by mutableStateOf(initialAutoPlay)
        internal set

    /** 当前播放进度 (0.0..1.0) */
    var progress: Double by mutableDoubleStateOf(0.0)
        internal set

    /** 循环次数设置（0 或负数表示无限循环） */
    var repeatCount: Int by mutableIntStateOf(initialRepeatCount)

    /** 缩放显示模式 */
    var scaleMode: PagScaleMode by mutableStateOf(initialScaleMode)

    /** 最大帧率限制 */
    var maxFrameRate: Float by mutableFloatStateOf(initialMaxFrameRate)

    /** 是否已完成资源加载 */
    var isLoaded: Boolean by mutableStateOf(false)
        internal set

    /** 加载失败异常（若成功则为 null） */
    var loadError: Throwable? by mutableStateOf(null)
        internal set

    /** 当前装载到视图的 PagComposition / PagFile 实例 */
    var currentComposition: PagComposition? by mutableStateOf(null)
        internal set

    /** 内部绑定的原生播放器句柄回调，供平台桥接注入控制命令 */
    internal var playAction: (() -> Unit)? = null
    internal var pauseAction: (() -> Unit)? = null
    internal var stopAction: (() -> Unit)? = null
    internal var seekAction: ((Double) -> Unit)? = null
    internal var flushAction: (() -> Unit)? = null

    /** 平台原生监听器强引用持有，防止弱引用被平台回收 */
    internal var nativeListener: Any? = null

    /** 开始播放 */
    fun play() {
        isPlaying = true
        playAction?.invoke()
    }

    /** 暂停播放 */
    fun pause() {
        isPlaying = false
        pauseAction?.invoke()
    }

    /** 停止播放并重置到起始位置 */
    fun stop() {
        isPlaying = false
        progress = 0.0
        stopAction?.invoke()
    }

    /** 拖动进度到指定位置 (0.0..1.0) */
    fun seekTo(progress: Double) {
        val clamped = progress.coerceIn(0.0, 1.0)
        this.progress = clamped
        seekAction?.invoke(clamped)
    }

    /** 强制触发当前帧立即渲染刷新（在动态图文替换后调用生效） */
    fun flush() {
        flushAction?.invoke()
    }
}

/**
 * 在 Composable 作用域内创建并记住 PagState。
 */
@Composable
fun rememberPagState(
    source: PagSource? = null,
    autoPlay: Boolean = true,
    repeatCount: Int = 0,
    scaleMode: PagScaleMode = PagScaleMode.LetterBox,
    maxFrameRate: Float = 60f
): PagState {
    val state = remember {
        PagState(
            initialSource = source,
            initialAutoPlay = autoPlay,
            initialRepeatCount = repeatCount,
            initialScaleMode = scaleMode,
            initialMaxFrameRate = maxFrameRate
        )
    }
    androidx.compose.runtime.SideEffect {
        if (source != null && state.source != source) {
            state.source = source
        }
    }
    return state
}
