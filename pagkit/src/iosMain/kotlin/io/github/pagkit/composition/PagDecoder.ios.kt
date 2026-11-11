@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.pagkit.composition

import org.libpag.ios.PAGDecoder
import platform.UIKit.UIImage

actual class PagDecoder internal constructor(internal val nativeDecoder: PAGDecoder) {
    actual val width: Int
        get() = nativeDecoder.width().toInt()

    actual val height: Int
        get() = nativeDecoder.height().toInt()

    actual val numFrames: Int
        get() = nativeDecoder.numFrames().toInt()

    actual val frameRate: Float
        get() = nativeDecoder.frameRate()

    actual fun checkFrameChanged(index: Int): Boolean {
        return nativeDecoder.checkFrameChanged(index)
    }

    /**
     * iOS 平台原生扩展：直接解码指定帧为 [UIImage]。
     */
    fun frameAtIndex(index: Int): UIImage? {
        return nativeDecoder.frameAtIndex(index.toLong())
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
