@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.pagkit.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import io.github.pagkit.cache.PagDiskCache
import io.github.pagkit.composition.PagComposition
import io.github.pagkit.composition.PagFile
import io.github.pagkit.model.PagScaleMode
import io.github.pagkit.model.PagSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.libpag.ios.PAGImageView
import org.libpag.ios.PAGImageViewListenerProtocol
import org.libpag.ios.PAGView
import org.libpag.ios.PAGViewListenerProtocol
import platform.UIKit.UIViewContentMode
import platform.darwin.NSObject

private class IosPagViewListener(
    private val onStart: (() -> Unit)?,
    private val onEnd: (() -> Unit)?,
    private val onCancel: (() -> Unit)?,
    private val onRepeat: (() -> Unit)?,
    private val onUpdate: ((Double) -> Unit)?
) : NSObject(), PAGViewListenerProtocol {
    override fun onAnimationStart(pagView: PAGView?) {
        onStart?.invoke()
    }

    override fun onAnimationEnd(pagView: PAGView?) {
        onEnd?.invoke()
    }

    override fun onAnimationCancel(pagView: PAGView?) {
        onCancel?.invoke()
    }

    override fun onAnimationRepeat(pagView: PAGView?) {
        onRepeat?.invoke()
    }

    override fun onAnimationUpdate(pagView: PAGView?) {
        val prog = pagView?.getProgress() ?: 0.0
        onUpdate?.invoke(prog)
    }
}

private class IosPagImageViewListener(
    private val onStart: (() -> Unit)?,
    private val onEnd: (() -> Unit)?,
    private val onCancel: (() -> Unit)?,
    private val onRepeat: (() -> Unit)?,
    private val onUpdate: ((Double) -> Unit)?
) : NSObject(), PAGImageViewListenerProtocol {
    override fun onAnimationStart(pagView: PAGImageView) {
        onStart?.invoke()
    }

    override fun onAnimationEnd(pagView: PAGImageView) {
        onEnd?.invoke()
    }

    override fun onAnimationCancel(pagView: PAGImageView) {
        onCancel?.invoke()
    }

    override fun onAnimationRepeat(pagView: PAGImageView) {
        onRepeat?.invoke()
    }

    override fun onAnimationUpdate(pagView: PAGImageView) {
        val current = pagView.currentFrame().toLong()
        val total = pagView.numFrames().toLong().coerceAtLeast(1L)
        onUpdate?.invoke(current.toDouble() / total.toDouble())
    }
}

private fun PagScaleMode.toUIViewContentMode(): UIViewContentMode = when (this) {
    PagScaleMode.None -> UIViewContentMode.UIViewContentModeTopLeft
    PagScaleMode.Stretch -> UIViewContentMode.UIViewContentModeScaleToFill
    PagScaleMode.LetterBox -> UIViewContentMode.UIViewContentModeScaleAspectFit
    PagScaleMode.Zoom -> UIViewContentMode.UIViewContentModeScaleAspectFill
}

private fun bindPagViewActions(view: PAGView, state: PagState) {
    state.playAction = {
        if (view.getProgress() >= 1.0) {
            view.setProgress(0.0)
        }
        view.play()
    }
    state.pauseAction = { view.pause() }
    state.stopAction = {
        view.stop()
        view.setProgress(0.0)
        view.flush()
    }
    state.seekAction = { prog ->
        view.setProgress(prog)
        view.flush()
    }
    state.flushAction = { view.flush() }
}

private fun bindPagImageViewActions(view: PAGImageView, state: PagState) {
    state.playAction = {
        if (view.currentFrame() >= (view.numFrames() - 1UL)) {
            view.setCurrentFrame(0UL)
        }
        view.play()
    }
    state.pauseAction = { view.pause() }
    state.stopAction = {
        view.pause()
        view.setCurrentFrame(0UL)
        view.flush()
    }
    state.seekAction = { prog ->
        val maxFrame = (view.numFrames().toLong() - 1L).coerceAtLeast(0L)
        val frame = (prog * maxFrame).toLong().coerceIn(0L, maxFrame).toULong()
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
    val currentState by rememberUpdatedState(state)
    val currentOnLoaded by rememberUpdatedState(onCompositionLoaded)
    val currentOnStart by rememberUpdatedState(onAnimationStart)
    val currentOnEnd by rememberUpdatedState(onAnimationEnd)
    val currentOnCancel by rememberUpdatedState(onAnimationCancel)
    val currentOnRepeat by rememberUpdatedState(onAnimationRepeat)
    val currentOnUpdate by rememberUpdatedState(onAnimationUpdate)

    LaunchedEffect(state, state.source) {
        val src = state.source
        if (src == null) {
            state.isLoaded = false
            state.currentComposition = null
            return@LaunchedEffect
        }
        try {
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

    UIKitView(
        modifier = modifier,
        factory = {
            PAGView().apply {
                setSync(sync)
                setVideoEnabled(videoEnabled)
                setCacheEnabled(cacheEnabled)
                setCacheScale(cacheScale)
                setMaxFrameRate(state.maxFrameRate)
                setScaleMode(state.scaleMode.nativeValue.toUInt())
                setRepeatCount(state.repeatCount)

                val listener = IosPagViewListener(
                    onStart = {
                        currentState.isPlaying = true
                        currentOnStart?.invoke()
                    },
                    onEnd = {
                        currentState.isPlaying = false
                        currentOnEnd?.invoke()
                    },
                    onCancel = {
                        currentState.isPlaying = false
                        currentOnCancel?.invoke()
                    },
                    onRepeat = {
                        currentOnRepeat?.invoke()
                    },
                    onUpdate = { prog ->
                        currentState.progress = prog
                        currentOnUpdate?.invoke(prog)
                    }
                )
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
            val targetScale = state.scaleMode.nativeValue.toUInt()
            if (view.scaleMode() != targetScale) {
                view.setScaleMode(targetScale)
                if (!view.isPlaying()) {
                    view.flush()
                }
            }
            view.setRepeatCount(state.repeatCount)

            val comp = state.currentComposition
            if (comp != null) {
                if (view.getComposition() != comp.nativeComposition) {
                    view.setComposition(comp.nativeComposition)
                    if (state.isPlaying) {
                        view.play()
                    } else {
                        view.flush()
                    }
                } else if (view.isPlaying() != state.isPlaying) {
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
            view.setComposition(null)
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
    val currentState by rememberUpdatedState(state)
    val currentOnLoaded by rememberUpdatedState(onCompositionLoaded)
    val currentOnStart by rememberUpdatedState(onAnimationStart)
    val currentOnEnd by rememberUpdatedState(onAnimationEnd)
    val currentOnCancel by rememberUpdatedState(onAnimationCancel)
    val currentOnRepeat by rememberUpdatedState(onAnimationRepeat)
    val currentOnUpdate by rememberUpdatedState(onAnimationUpdate)

    LaunchedEffect(state, state.source) {
        val src = state.source
        if (src == null) {
            state.isLoaded = false
            state.currentComposition = null
            return@LaunchedEffect
        }
        try {
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

    UIKitView(
        modifier = modifier,
        factory = {
            PAGImageView().apply {
                setCacheAllFramesInMemory(cacheAllFramesInMemory)
                setRenderScale(renderScale)
                contentMode = state.scaleMode.toUIViewContentMode()
                setRepeatCount(state.repeatCount)

                val listener = IosPagImageViewListener(
                    onStart = {
                        currentState.isPlaying = true
                        currentOnStart?.invoke()
                    },
                    onEnd = {
                        currentState.isPlaying = false
                        currentOnEnd?.invoke()
                    },
                    onCancel = {
                        currentState.isPlaying = false
                        currentOnCancel?.invoke()
                    },
                    onRepeat = {
                        currentOnRepeat?.invoke()
                    },
                    onUpdate = { prog ->
                        currentState.progress = prog
                        currentOnUpdate?.invoke(prog)
                    }
                )
                currentState.nativeListener = listener
                addListener(listener)

                bindPagImageViewActions(this, state)
            }
        },
        update = { view ->
            bindPagImageViewActions(view, state)

            view.setCacheAllFramesInMemory(cacheAllFramesInMemory)
            view.setRenderScale(renderScale)
            view.contentMode = state.scaleMode.toUIViewContentMode()
            view.setRepeatCount(state.repeatCount)

            val comp = state.currentComposition
            if (comp != null) {
                if (view.getComposition() != comp.nativeComposition) {
                    view.setComposition(comp.nativeComposition, state.maxFrameRate)
                    if (state.isPlaying) {
                        view.play()
                    } else {
                        view.flush()
                    }
                } else if (view.isPlaying() != state.isPlaying) {
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
            view.setComposition(null)
            currentState.nativeListener = null
            currentState.playAction = null
            currentState.pauseAction = null
            currentState.stopAction = null
            currentState.seekAction = null
            currentState.flushAction = null
        }
    )
}
