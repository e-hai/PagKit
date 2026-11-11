package io.github.pagkit.model

import io.github.pagkit.PagKitAndroid
import org.libpag.PAGFont
import java.io.File

actual class PagFont internal constructor(internal val nativeFont: PAGFont) {
    actual val fontFamily: String
        get() = nativeFont.fontFamily

    actual val fontStyle: String
        get() = nativeFont.fontStyle

    actual companion object {
        actual fun registerFont(fontPath: String): PagFont? {
            val f = PAGFont.RegisterFont(fontPath) ?: return null
            return PagFont(f)
        }

        actual fun registerFont(fontPath: String, fontFamily: String, fontStyle: String): PagFont? {
            val f = PAGFont.RegisterFont(fontPath, 0, fontFamily, fontStyle) ?: return null
            return PagFont(f)
        }

        actual fun registerFont(bytes: ByteArray): PagFont? {
            val file = File(PagKitAndroid.getAppContext().cacheDir, "font_${bytes.hashCode()}.ttf")
            if (!file.exists()) {
                file.writeBytes(bytes)
            }
            return registerFont(file.absolutePath)
        }

        actual fun registerFont(bytes: ByteArray, fontFamily: String, fontStyle: String): PagFont? {
            val file = File(PagKitAndroid.getAppContext().cacheDir, "font_${fontFamily}_${fontStyle}.ttf")
            if (!file.exists()) {
                file.writeBytes(bytes)
            }
            return registerFont(file.absolutePath, fontFamily, fontStyle)
        }

        actual fun unregisterFont(font: PagFont) {
            PAGFont.UnregisterFont(font.nativeFont)
        }
    }
}
