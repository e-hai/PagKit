@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.pagkit.composition

import io.github.pagkit.model.PagScaleMode
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.libpag.ios.PAGImage
import platform.CoreGraphics.CGImageRef
import platform.CoreVideo.CVPixelBufferRef
import platform.UIKit.UIImage

actual class PagImage internal constructor(internal val nativeImage: PAGImage) {
    actual val width: Int
        get() = nativeImage.width().toInt()

    actual val height: Int
        get() = nativeImage.height().toInt()

    actual var scaleMode: PagScaleMode
        get() = PagScaleMode.fromNative(nativeImage.scaleMode().toInt())
        set(value) {
            nativeImage.setScaleMode(value.nativeValue.toUInt())
        }

    actual companion object {
        actual fun fromPath(path: String): PagImage? {
            val img = PAGImage.FromPath(path) ?: return null
            return PagImage(img)
        }

        actual fun fromBytes(bytes: ByteArray): PagImage? {
            if (bytes.isEmpty()) return null
            val img = bytes.usePinned { pinned ->
                PAGImage.FromBytes(pinned.addressOf(0), bytes.size.toULong())
            } ?: return null
            return PagImage(img)
        }

        /**
         * iOS 平台原生扩展：从 CoreGraphics [CGImageRef] 构造 [PagImage]，实现零拷贝渲染。
         */
        fun fromCGImage(cgImage: CGImageRef): PagImage? {
            val img = PAGImage.FromCGImage(cgImage) ?: return null
            return PagImage(img)
        }

        /**
         * iOS 平台原生扩展：从 UIKit [UIImage] 构造 [PagImage]。
         */
        fun fromUIImage(uiImage: UIImage): PagImage? {
            val cgImage = uiImage.CGImage ?: return null
            return fromCGImage(cgImage)
        }

        /**
         * iOS 平台原生扩展：从 CoreVideo [CVPixelBufferRef] 构造 [PagImage]，支持相机或视频纹理实时替换。
         */
        fun fromPixelBuffer(pixelBuffer: CVPixelBufferRef): PagImage? {
            val img = PAGImage.FromPixelBuffer(pixelBuffer) ?: return null
            return PagImage(img)
        }
    }
}
