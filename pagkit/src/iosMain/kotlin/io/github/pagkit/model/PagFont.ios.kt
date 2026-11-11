@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.pagkit.model

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.libpag.ios.PAGFont

actual class PagFont internal constructor(internal val nativeFont: PAGFont) {
    actual val fontFamily: String
        get() = nativeFont.fontFamily ?: ""

    actual val fontStyle: String
        get() = nativeFont.fontStyle ?: ""

    actual companion object {
        actual fun registerFont(fontPath: String): PagFont? {
            val f = PAGFont.RegisterFont(fontPath) ?: return null
            return PagFont(f)
        }

        actual fun registerFont(fontPath: String, fontFamily: String, fontStyle: String): PagFont? {
            val f = PAGFont.RegisterFont(fontPath, fontFamily, fontStyle) ?: return null
            return PagFont(f)
        }

        actual fun registerFont(bytes: ByteArray): PagFont? {
            if (bytes.isEmpty()) return null
            val f = bytes.usePinned { pinned ->
                PAGFont.RegisterFont(pinned.addressOf(0), bytes.size.toULong())
            } ?: return null
            return PagFont(f)
        }

        actual fun registerFont(bytes: ByteArray, fontFamily: String, fontStyle: String): PagFont? {
            if (bytes.isEmpty()) return null
            val f = bytes.usePinned { pinned ->
                PAGFont.RegisterFont(pinned.addressOf(0), bytes.size.toULong(), fontFamily, fontStyle)
            } ?: return null
            return PagFont(f)
        }

        actual fun unregisterFont(font: PagFont) {
            PAGFont.UnregisterFont(font.nativeFont)
        }
    }
}
