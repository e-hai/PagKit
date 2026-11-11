package io.github.pagkit.compose

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.github.pagkit.PagKitAndroid
import io.github.pagkit.cache.PagDiskCache
import io.github.pagkit.composition.PagComposition
import io.github.pagkit.composition.PagFile
import io.github.pagkit.model.PagSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.libpag.PAGImageView
import org.libpag.PAGView

private fun bindPagViewActions(view: PAGView, state: PagState) {
    state.playAction = {
        if (view.progress >= 1.0) {
            view.progress = 0.0
        }
        view.play()
    }
    state.pauseAction = { view.pause() }
    state.stopAction = {
        view.stop()
        view.progress = 0.0
        view.flush()
    }
    state.seekAction = { prog ->
        view.progress = prog
        view.flush()
    }
    state.flushAction = { view.flush() }
}

private fun bindPagImageViewActions(view: PAGImageView, state: PagState) {
    state.playAction = {
        if (view.currentFrame() >= (view.numFrames() - 1).coerceAtLeast(0)) {
            view.setCurrentFrame(0)
        }
        view.play()
    }
    state.pauseAction = { view.pause() }
    state.stopAction = {
        view.pause()
        view.setCurrentFrame(0)
        view.flush()
    }
    state.seekAction = { prog ->
        val frame = (prog * (view.numFrames() - 1)).toInt().coerceAtLeast(0)
        view.setCurrentFrame(frame)
        view.flush()
    }
    state.flushAction = { view.flush() }
}

@Composable
actual fun PagView(
    state: PagState,
    modifier: Modifier,
    sync: Boolean,
    videoEnabled: Boolean,
    cacheEnabled: Boolean,
    cacheScale: Float,
    onCompositionLoaded: ((PagFile) -> Unit)?,
    onAnimationStart: (() -> Unit)?,
    onAnimationEnd: (() -> Unit)?,
    onAnimationCancel: (() -> Unit)?,
    onAnimationRepeat: (() -> Unit)?,
    onAnimationUpdate: ((progress: Double) -> Unit)?
) {
    val context = LocalContext.current
    val currentState by rememberUpdatedState(state)
    val currentOnLoaded by rememberUpdatedState(onCompositionLoaded)
    val currentOnStart by rememberUpdatedState(onAnimationStart)
    val currentOnEnd by rememberUpdatedState(onAnimationEnd)
    val currentOnCancel by rememberUpdatedState(onAnimationCancel)
    val currentOnRepeat by rememberUpdatedState(onAnimationRepeat)
    val currentOnUpdate by rememberUpdatedState(onAnimationUpdate)

    LaunchedEffect(Unit) {
        PagKitAndroid.init(context)
    }

    LaunchedEffect(state, state.source) {
        val src = state.source
        if (src == null) {
            state.isLoaded = false
            state.currentComposition = null
            return@LaunchedEffect
        }
        try {
            PagKitAndroid.init(context)
            val comp: PagComposition? = withContext(Dispatchers.IO) {
                when (src) {
                    is PagSource.Composition -> src.composition
                    is PagSource.FilePath -> PagFile.loadFromPath(src.path)
                    is PagSource.Asset -> PagFile.loadFromAssets(src.assetName)
                    is PagSource.Bytes -> PagFile.loadFromBytes(src.bytes)
                    is PagSource.Url -> PagDiskCache.loadFromUrl(src.url)
                }
            }
            if (comp != null) {
                state.currentComposition = comp
                state.isLoaded = true
                state.loadError = null
                if (comp is PagFile) {
                    currentOnLoaded?.invoke(comp)
                }
            } else {
                state.isLoaded = false
                state.loadError = IllegalArgumentException("Failed to load PAG from $src")
            }
        } catch (t: Throwable) {
            state.isLoaded = false
            state.loadError = t
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PAGView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setSync(sync)
                setVideoEnabled(videoEnabled)
                setCacheEnabled(cacheEnabled)
                setCacheScale(cacheScale)
                setMaxFrameRate(state.maxFrameRate)
                setScaleMode(state.scaleMode.nativeValue)
                setRepeatCount(state.repeatCount)

                val listener = object : PAGView.PAGViewListener {
                    override fun onAnimationStart(p0: PAGView?) {
                        currentState.isPlaying = true
                        currentOnStart?.invoke()
                    }

                    override fun onAnimationEnd(p0: PAGView?) {
                        currentState.isPlaying = false
                        currentOnEnd?.invoke()
                    }

                    override fun onAnimationCancel(p0: PAGView?) {
                        currentState.isPlaying = false
                        currentOnCancel?.invoke()
                    }

                    override fun onAnimationRepeat(p0: PAGView?) {
                        currentOnRepeat?.invoke()
                    }

                    override fun onAnimationUpdate(p0: PAGView?) {
                        val prog = this@apply.progress
                        currentState.progress = prog
                        currentOnUpdate?.invoke(prog)
                    }
                }
                currentState.nativeListener = listener
                addListener(listener)

                bindPagViewActions(this, state)
            }
        },
        update = { view ->
            bindPagViewActions(view, state)

            view.setSync(sync)
            view.setVideoEnabled(videoEnabled)
            view.setCacheEnabled(cacheEnabled)
            view.setCacheScale(cacheScale)
            view.setMaxFrameRate(state.maxFrameRate)
            if (view.scaleMode() != state.scaleMode.nativeValue) {
                view.setScaleMode(state.scaleMode.nativeValue)
                if (!view.isPlaying) {
                    view.flush()
                }
            }
            view.setRepeatCount(state.repeatCount)

            val comp = state.currentComposition
            if (comp != null) {
                if (view.composition != comp.nativeComposition) {
                    view.composition = comp.nativeComposition
                    if (state.isPlaying) {
                        view.play()
                    } else {
                        view.flush()
                    }
                } else if (view.isPlaying != state.isPlaying) {
                    if (state.isPlaying) {
                        view.play()
                    } else {
                        view.pause()
                    }
                }
            }
        },
        onRelease = { view ->
            view.stop()
            view.composition = null
            currentState.nativeListener = null
            currentState.playAction = null
            currentState.pauseAction = null
            currentState.stopAction = null
            currentState.seekAction = null
            currentState.flushAction = null
        }
    )
}

@Composable
actual fun PagImageView(
    state: PagState,
    modifier: Modifier,
    cacheAllFramesInMemory: Boolean,
    renderScale: Float,
    onCompositionLoaded: ((PagFile) -> Unit)?,
    onAnimationStart: (() -> Unit)?,
    onAnimationEnd: (() -> Unit)?,
    onAnimationCancel: (() -> Unit)?,
    onAnimationRepeat: (() -> Unit)?,
    onAnimationUpdate: ((progress: Double) -> Unit)?
) {
    val context = LocalContext.current
    val currentState by rememberUpdatedState(state)
    val currentOnLoaded by rememberUpdatedState(onCompositionLoaded)
    val currentOnStart by rememberUpdatedState(onAnimationStart)
    val currentOnEnd by rememberUpdatedState(onAnimationEnd)
    val currentOnCancel by rememberUpdatedState(onAnimationCancel)
    val currentOnRepeat by rememberUpdatedState(onAnimationRepeat)
    val currentOnUpdate by rememberUpdatedState(onAnimationUpdate)

    LaunchedEffect(Unit) {
        PagKitAndroid.init(context)
    }

    LaunchedEffect(state, state.source) {
        val src = state.source
        if (src == null) {
            state.isLoaded = false
            state.currentComposition = null
            return@LaunchedEffect
        }
        try {
            PagKitAndroid.init(context)
            val comp: PagComposition? = withContext(Dispatchers.IO) {
                when (src) {
                    is PagSource.Composition -> src.composition
                    is PagSource.FilePath -> PagFile.loadFromPath(src.path)
                    is PagSource.Asset -> PagFile.loadFromAssets(src.assetName)
                    is PagSource.Bytes -> PagFile.loadFromBytes(src.bytes)
                    is PagSource.Url -> PagDiskCache.loadFromUrl(src.url)
                }
            }
            if (comp != null) {
                state.currentComposition = comp
                state.isLoaded = true
                state.loadError = null
                if (comp is PagFile) {
                    currentOnLoaded?.invoke(comp)
                }
            } else {
                state.isLoaded = false
                state.loadError = IllegalArgumentException("Failed to load PAG from $src")
            }
        } catch (t: Throwable) {
            state.isLoaded = false
            state.loadError = t
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PAGImageView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setCacheAllFramesInMemory(cacheAllFramesInMemory)
                setRenderScale(renderScale)
                setScaleMode(state.scaleMode.nativeValue)
                setRepeatCount(state.repeatCount)

                val listener = object : PAGImageView.PAGImageViewListener {
                    override fun onAnimationStart(p0: PAGImageView?) {
                        currentState.isPlaying = true
                        currentOnStart?.invoke()
                    }

                    override fun onAnimationEnd(p0: PAGImageView?) {
                        currentState.isPlaying = false
                        currentOnEnd?.invoke()
                    }

                    override fun onAnimationCancel(p0: PAGImageView?) {
                        currentState.isPlaying = false
                        currentOnCancel?.invoke()
                    }

                    override fun onAnimationRepeat(p0: PAGImageView?) {
                        currentOnRepeat?.invoke()
                    }

                    override fun onAnimationUpdate(p0: PAGImageView?) {
                        val prog = this@apply.currentFrame().toDouble() / this@apply.numFrames().coerceAtLeast(1)
                        currentState.progress = prog
                        currentOnUpdate?.invoke(prog)
                    }
                }
                currentState.nativeListener = listener
                addListener(listener)

                bindPagImageViewActions(this, state)
            }
        },
        update = { view ->
            bindPagImageViewActions(view, state)

            view.setCacheAllFramesInMemory(cacheAllFramesInMemory)
            view.setRenderScale(renderScale)
            if (view.scaleMode() != state.scaleMode.nativeValue) {
                view.setScaleMode(state.scaleMode.nativeValue)
                if (!view.isPlaying) {
                    view.flush()
                }
            }
            view.setRepeatCount(state.repeatCount)

            val comp = state.currentComposition
            if (comp != null) {
                if (view.composition != comp.nativeComposition) {
                    view.setComposition(comp.nativeComposition, state.maxFrameRate)
                    if (state.isPlaying) {
                        view.play()
                    } else {
                        view.flush()
                    }
                } else if (view.isPlaying != state.isPlaying) {
                    if (state.isPlaying) {
                        view.play()
                    } else {
                        view.pause()
                    }
                }
            }
        },
        onRelease = { view ->
            view.pause()
            view.composition = null
            currentState.nativeListener = null
            currentState.playAction = null
            currentState.pauseAction = null
            currentState.stopAction = null
            currentState.seekAction = null
            currentState.flushAction = null
        }
    )
}
