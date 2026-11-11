@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.pagkit.composition

import io.github.pagkit.model.PagLayerType
import org.libpag.ios.PAGLayer
import org.libpag.ios.PAGLayerType as NativePAGLayerType
import platform.CoreGraphics.CGAffineTransformRotate
import platform.CoreGraphics.CGAffineTransformScale
import platform.CoreGraphics.CGAffineTransformTranslate
import kotlin.math.PI

internal fun NativePAGLayerType.toModel(): PagLayerType = when (this) {
    NativePAGLayerType.PAGLayerTypeNull -> PagLayerType.Null
    NativePAGLayerType.PAGLayerTypeSolid -> PagLayerType.Solid
    NativePAGLayerType.PAGLayerTypeText -> PagLayerType.Text
    NativePAGLayerType.PAGLayerTypeShape -> PagLayerType.Shape
    NativePAGLayerType.PAGLayerTypeImage -> PagLayerType.Image
    NativePAGLayerType.PAGLayerTypePreCompose -> PagLayerType.PreCompose
    else -> PagLayerType.Unknown
}

internal fun PagLayerType.toNative(): NativePAGLayerType = when (this) {
    PagLayerType.Null -> NativePAGLayerType.PAGLayerTypeNull
    PagLayerType.Solid -> NativePAGLayerType.PAGLayerTypeSolid
    PagLayerType.Text -> NativePAGLayerType.PAGLayerTypeText
    PagLayerType.Shape -> NativePAGLayerType.PAGLayerTypeShape
    PagLayerType.Image -> NativePAGLayerType.PAGLayerTypeImage
    PagLayerType.PreCompose -> NativePAGLayerType.PAGLayerTypePreCompose
    PagLayerType.Unknown -> NativePAGLayerType.PAGLayerTypeUnknown
}

actual open class PagLayer internal constructor(internal val nativeLayer: PAGLayer) {
    actual val layerType: PagLayerType
        get() = nativeLayer.layerType().toModel()

    actual val layerName: String
        get() = nativeLayer.layerName() ?: ""

    actual val editableIndex: Int
        get() = nativeLayer.editableIndex().toInt()

    actual var visible: Boolean
        get() = nativeLayer.visible()
        set(value) {
            nativeLayer.setVisible(value)
        }

    actual var alpha: Float
        get() = nativeLayer.alpha()
        set(value) {
            nativeLayer.setAlpha(value)
        }

    actual val durationUs: Long
        get() = nativeLayer.duration()

    actual val frameRate: Float
        get() = nativeLayer.frameRate()

    actual var startTimeUs: Long
        get() = nativeLayer.startTime()
        set(value) {
            nativeLayer.setStartTime(value)
        }

    actual var currentTimeUs: Long
        get() = nativeLayer.currentTime()
        set(value) {
            nativeLayer.setCurrentTime(value)
        }

    actual var progress: Double
        get() = nativeLayer.getProgress()
        set(value) {
            nativeLayer.setProgress(value)
        }

    actual var excludedFromTimeline: Boolean
        get() = nativeLayer.excludedFromTimeline()
        set(value) {
            nativeLayer.setExcludedFromTimeline(value)
        }

    actual fun resetMatrix() {
        nativeLayer.resetMatrix()
    }

    actual fun setTranslation(dx: Float, dy: Float) {
        val current = nativeLayer.matrix()
        val modified = CGAffineTransformTranslate(current, dx.toDouble(), dy.toDouble())
        nativeLayer.setMatrix(modified)
    }

    actual fun setScale(sx: Float, sy: Float) {
        val current = nativeLayer.matrix()
        val modified = CGAffineTransformScale(current, sx.toDouble(), sy.toDouble())
        nativeLayer.setMatrix(modified)
    }

    actual fun setRotation(degrees: Float) {
        val current = nativeLayer.matrix()
        val radians = (degrees * PI / 180.0)
        val modified = CGAffineTransformRotate(current, radians)
        nativeLayer.setMatrix(modified)
    }
}
