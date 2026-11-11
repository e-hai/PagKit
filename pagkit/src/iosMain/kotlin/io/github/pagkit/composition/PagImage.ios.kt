@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.pagkit.composition

import io.github.pagkit.model.PagScaleMode
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.libpag.ios.PAGImage

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
    }
}
