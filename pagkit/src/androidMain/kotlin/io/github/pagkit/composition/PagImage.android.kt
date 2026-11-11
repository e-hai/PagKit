package io.github.pagkit.composition

import android.graphics.Bitmap
import io.github.pagkit.model.PagScaleMode
import org.libpag.PAGImage

actual class PagImage internal constructor(internal val nativeImage: PAGImage) {
    actual val width: Int
        get() = nativeImage.width()

    actual val height: Int
        get() = nativeImage.height()

    actual var scaleMode: PagScaleMode
        get() = PagScaleMode.fromNative(nativeImage.scaleMode())
        set(value) {
            nativeImage.setScaleMode(value.nativeValue)
        }

    actual companion object {
        actual fun fromPath(path: String): PagImage? {
            val img = PAGImage.FromPath(path) ?: return null
            return PagImage(img)
        }

        actual fun fromBytes(bytes: ByteArray): PagImage? {
            val img = PAGImage.FromBytes(bytes) ?: return null
            return PagImage(img)
        }

        /**
         * Android 平台原生扩展：直接从 [Bitmap] 构造 [PagImage]，实现零拷贝图元替换。
         */
        fun fromBitmap(bitmap: Bitmap): PagImage? {
            val img = PAGImage.FromBitmap(bitmap) ?: return null
            return PagImage(img)
        }
    }
}
