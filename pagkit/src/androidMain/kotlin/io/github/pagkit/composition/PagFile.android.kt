package io.github.pagkit.composition

import io.github.pagkit.PagKitAndroid
import io.github.pagkit.model.PagLayerType
import io.github.pagkit.model.PagTextData
import io.github.pagkit.model.PagTimeStretchMode
import org.libpag.PAGFile
import org.libpag.PAGText

actual class PagFile internal constructor(
    internal val nativeFile: PAGFile
) : PagComposition(nativeFile) {

    actual val tagLevel: Int
        get() = nativeFile.tagLevel()

    actual val numTexts: Int
        get() = nativeFile.numTexts()

    actual val numImages: Int
        get() = nativeFile.numImages()

    actual val numVideos: Int
        get() = nativeFile.numVideos()

    actual val filePath: String
        get() = nativeFile.path() ?: ""

    actual var timeStretchMode: PagTimeStretchMode
        get() = PagTimeStretchMode.fromNative(nativeFile.timeStretchMode())
        set(value) {
            nativeFile.setTimeStretchMode(value.nativeValue)
        }

    actual fun setDurationUs(durationUs: Long) {
        nativeFile.setDuration(durationUs)
    }

    actual fun getTextData(editableTextIndex: Int): PagTextData? {
        val t = nativeFile.getTextData(editableTextIndex) ?: return null
        return PagTextData(
            text = t.text ?: "",
            fontSize = t.fontSize,
            fillColorArgb = t.fillColor,
            strokeColorArgb = t.strokeColor,
            strokeWidth = t.strokeWidth,
            fontFamily = t.fontFamily ?: "",
            fontStyle = t.fontStyle ?: "",
            fauxBold = t.fauxBold,
            fauxItalic = t.fauxItalic,
            leading = t.leading,
            tracking = t.tracking,
            backgroundColorArgb = t.backgroundColor,
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
            fillColor = textData.fillColorArgb
            strokeColor = textData.strokeColorArgb
            strokeWidth = textData.strokeWidth
            fontFamily = textData.fontFamily
            fontStyle = textData.fontStyle
            fauxBold = textData.fauxBold
            fauxItalic = textData.fauxItalic
            leading = textData.leading
            tracking = textData.tracking
            backgroundColor = textData.backgroundColorArgb
            backgroundAlpha = textData.backgroundAlpha
        }
        nativeFile.replaceText(editableTextIndex, t)
    }

    actual fun replaceImage(editableImageIndex: Int, image: PagImage?) {
        nativeFile.replaceImage(editableImageIndex, image?.nativeImage)
    }

    actual fun replaceImageByName(layerName: String, image: PagImage?) {
        nativeFile.replaceImageByName(layerName, image?.nativeImage)
    }

    actual fun getEditableIndices(layerType: PagLayerType): List<Int> {
        val indices = nativeFile.getEditableIndices(layerType.nativeValue) ?: return emptyList()
        return indices.toList()
    }

    actual fun getLayersByEditableIndex(editableIndex: Int, layerType: PagLayerType): List<PagLayer> {
        val layers = nativeFile.getLayersByEditableIndex(editableIndex, layerType.nativeValue) ?: return emptyList()
        return layers.map { PagLayer(it) }
    }

    actual fun copyOriginal(): PagFile {
        return PagFile(nativeFile.copyOriginal())
    }

    actual companion object {
        actual fun maxSupportedTagLevel(): Int = PAGFile.MaxSupportedTagLevel()

        actual fun loadFromPath(path: String): PagFile? {
            val file = PAGFile.Load(path) ?: return null
            return PagFile(file)
        }

        actual fun loadFromBytes(bytes: ByteArray): PagFile? {
            val file = PAGFile.Load(bytes) ?: return null
            return PagFile(file)
        }

        actual fun loadFromAssets(assetName: String): PagFile? {
            val context = PagKitAndroid.getAppContext()
            val file = PAGFile.Load(context.assets, assetName) ?: return null
            return PagFile(file)
        }
    }
}
