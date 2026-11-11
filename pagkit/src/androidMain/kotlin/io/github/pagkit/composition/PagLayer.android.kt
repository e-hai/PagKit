package io.github.pagkit.composition

import android.graphics.Matrix
import io.github.pagkit.model.PagLayerType
import io.github.pagkit.model.PagMarker
import org.libpag.PAGLayer

actual open class PagLayer internal constructor(internal val nativeLayer: PAGLayer) {
    actual val layerType: PagLayerType
        get() = PagLayerType.fromNative(nativeLayer.layerType())

    actual val layerName: String
        get() = nativeLayer.layerName() ?: ""

    actual val markers: List<PagMarker>
        get() {
            val arr = nativeLayer.markers() ?: return emptyList()
            return arr.map {
                PagMarker(
                    startTimeUs = it.mStartTime,
                    durationUs = it.mDuration,
                    comment = it.mComment ?: ""
                )
            }
        }

    actual val editableIndex: Int
        get() = nativeLayer.editableIndex()

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
        get() = nativeLayer.progress
        set(value) {
            nativeLayer.progress = value
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
        val m = nativeLayer.matrix() ?: Matrix()
        m.postTranslate(dx, dy)
        nativeLayer.setMatrix(m)
    }

    actual fun setScale(sx: Float, sy: Float) {
        val m = nativeLayer.matrix() ?: Matrix()
        m.postScale(sx, sy)
        nativeLayer.setMatrix(m)
    }

    actual fun setRotation(degrees: Float) {
        val m = nativeLayer.matrix() ?: Matrix()
        m.postRotate(degrees)
        nativeLayer.setMatrix(m)
    }
}
