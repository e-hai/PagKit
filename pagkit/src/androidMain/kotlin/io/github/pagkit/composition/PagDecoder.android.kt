package io.github.pagkit.composition

import android.graphics.Bitmap
import org.libpag.PAGDecoder

actual class PagDecoder internal constructor(internal val nativeDecoder: PAGDecoder) {
    actual val width: Int
        get() = nativeDecoder.width()

    actual val height: Int
        get() = nativeDecoder.height()

    actual val numFrames: Int
        get() = nativeDecoder.numFrames()

    actual val frameRate: Float
        get() = nativeDecoder.frameRate()

    actual fun checkFrameChanged(index: Int): Boolean {
        return nativeDecoder.checkFrameChanged(index)
    }

    /**
     * Android 平台原生扩展：直接解码指定帧为 [Bitmap]。
     */
    fun frameAtIndex(index: Int): Bitmap? {
        return nativeDecoder.frameAtIndex(index)
    }

    actual companion object {
        actual fun make(composition: PagComposition): PagDecoder? {
            val decoder = PAGDecoder.Make(composition.nativeComposition) ?: return null
            return PagDecoder(decoder)
        }

        actual fun make(composition: PagComposition, maxFrameRate: Float, scale: Float): PagDecoder? {
            val decoder = PAGDecoder.Make(composition.nativeComposition, maxFrameRate, scale) ?: return null
            return PagDecoder(decoder)
        }
    }
}
