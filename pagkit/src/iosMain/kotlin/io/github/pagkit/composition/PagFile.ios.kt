@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.pagkit.composition

import io.github.pagkit.model.PagLayerType
import io.github.pagkit.model.PagTextData
import io.github.pagkit.model.PagTimeStretchMode
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.libpag.ios.PAGFile
import org.libpag.ios.PAGText
import platform.Foundation.NSBundle
import platform.Foundation.NSNumber
import platform.UIKit.UIColor

actual class PagFile internal constructor(
    internal val nativeFile: PAGFile
) : PagComposition(nativeFile) {

    actual val tagLevel: Int
        get() = nativeFile.tagLevel().toInt()

    actual val numTexts: Int
        get() = nativeFile.numTexts()

    actual val numImages: Int
        get() = nativeFile.numImages()

    actual val numVideos: Int
        get() = nativeFile.numVideos()

    actual val filePath: String
        get() = nativeFile.path() ?: ""

    actual var timeStretchMode: PagTimeStretchMode
        get() = PagTimeStretchMode.fromNative(nativeFile.timeStretchMode().toInt())
        set(value) {
            nativeFile.seTimeStretchMode(value.nativeValue.toUInt())
        }

    actual fun setDurationUs(durationUs: Long) {
        nativeFile.setDuration(durationUs)
    }

    actual fun getTextData(editableTextIndex: Int): PagTextData? {
        val t = nativeFile.getTextData(editableTextIndex) ?: return null
        return PagTextData(
            text = t.text ?: "",
            fontSize = t.fontSize,
            fillColorArgb = 0xFFFFFFFF.toInt(),
            strokeColorArgb = 0xFF000000.toInt(),
            strokeWidth = t.strokeWidth,
            fontFamily = t.fontFamily ?: "",
            fontStyle = t.fontStyle ?: "",
            fauxBold = t.fauxBold,
            fauxItalic = t.fauxItalic,
            leading = t.leading,
            tracking = t.tracking,
            backgroundColorArgb = 0,
            backgroundAlpha = t.backgroundAlpha
        )
    }

    actual fun replaceText(editableTextIndex: Int, textData: PagTextData?) {
        if (textData == null) {
            nativeFile.replaceText(editableTextIndex, null)
            return
        }
        val t = PAGText().apply {
            text = textData.text
            fontSize = textData.fontSize
            strokeWidth = textData.strokeWidth
            fontFamily = textData.fontFamily
            fontStyle = textData.fontStyle
            fauxBold = textData.fauxBold
            fauxItalic = textData.fauxItalic
            leading = textData.leading
            tracking = textData.tracking
            backgroundAlpha = textData.backgroundAlpha

            val a = ((textData.fillColorArgb shr 24) and 0xFF) / 255.0
            val r = ((textData.fillColorArgb shr 16) and 0xFF) / 255.0
            val g = ((textData.fillColorArgb shr 8) and 0xFF) / 255.0
            val b = (textData.fillColorArgb and 0xFF) / 255.0
            fillColor = UIColor.colorWithRed(r, g, b, a)

            val sa = ((textData.strokeColorArgb shr 24) and 0xFF) / 255.0
            val sr = ((textData.strokeColorArgb shr 16) and 0xFF) / 255.0
            val sg = ((textData.strokeColorArgb shr 8) and 0xFF) / 255.0
            val sb = (textData.strokeColorArgb and 0xFF) / 255.0
            strokeColor = UIColor.colorWithRed(sr, sg, sb, sa)
        }
        nativeFile.replaceText(editableTextIndex, t)
    }

    actual fun replaceImage(editableImageIndex: Int, image: PagImage?) {
        nativeFile.replaceImage(editableImageIndex, image?.nativeImage)
    }

    actual fun replaceImageByName(layerName: String, image: PagImage?) {
        nativeFile.replaceImageByName(layerName, image?.nativeImage)
    }

    @Suppress("UNCHECKED_CAST")
    actual fun getEditableIndices(layerType: PagLayerType): List<Int> {
        val indices = nativeFile.getEditableIndices(layerType.toNative()) as? List<NSNumber> ?: return emptyList()
        return indices.map { it.intValue }
    }

    @Suppress("UNCHECKED_CAST")
    actual fun getLayersByEditableIndex(editableIndex: Int, layerType: PagLayerType): List<PagLayer> {
        val layers = nativeFile.getLayersByEditableIndex(editableIndex, layerType.toNative()) as? List<org.libpag.ios.PAGLayer> ?: return emptyList()
        return layers.map { PagLayer(it) }
    }

    actual fun copyOriginal(): PagFile {
        val copy = nativeFile.copyOriginal()
        if (copy == null) {
            println("[PagKit] WARN: PAGFile.copyOriginal returned null, returning original instance")
            return this
        }
        return PagFile(copy)
    }

    actual companion object {
        actual fun maxSupportedTagLevel(): Int = PAGFile.MaxSupportedTagLevel().toInt()

        actual fun loadFromPath(path: String): PagFile? {
            val file = PAGFile.Load(path) ?: return null
            return PagFile(file)
        }

        actual fun loadFromBytes(bytes: ByteArray): PagFile? {
            if (bytes.isEmpty()) return null
            val file = bytes.usePinned { pinned ->
                PAGFile.Load(pinned.addressOf(0), bytes.size.toULong())
            } ?: return null
            return PagFile(file)
        }

        actual fun loadFromAssets(assetName: String): PagFile? {
            val path = NSBundle.mainBundle.pathForResource(assetName, null)
                ?: NSBundle.mainBundle.resourcePath?.let { "$it/$assetName" }
                ?: return null
            return loadFromPath(path)
        }
    }
}
