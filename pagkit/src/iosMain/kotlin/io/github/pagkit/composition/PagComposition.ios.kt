@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.pagkit.composition

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.libpag.ios.PAGComposition
import platform.CoreGraphics.CGSizeMake

actual open class PagComposition internal constructor(
    internal val nativeComposition: PAGComposition
) : PagLayer(nativeComposition) {

    actual val width: Int
        get() = nativeComposition.width().toInt()

    actual val height: Int
        get() = nativeComposition.height().toInt()

    actual val numChildren: Int
        get() = nativeComposition.numChildren().toInt()

    actual fun setContentSize(width: Int, height: Int) {
        nativeComposition.setContentSize(CGSizeMake(width.toDouble(), height.toDouble()))
    }

    actual fun getLayerAt(index: Int): PagLayer? {
        val l = nativeComposition.getLayerAt(index) ?: return null
        return PagLayer(l)
    }

    actual fun addLayer(layer: PagLayer): Boolean {
        return nativeComposition.addLayer(layer.nativeLayer)
    }

    actual fun addLayerAt(layer: PagLayer, index: Int): Boolean {
        return nativeComposition.addLayer(layer.nativeLayer, index)
    }

    actual fun removeLayer(layer: PagLayer): PagLayer? {
        val removed = nativeComposition.removeLayer(layer.nativeLayer) ?: return null
        return PagLayer(removed)
    }

    actual fun removeLayerAt(index: Int): PagLayer? {
        val removed = nativeComposition.removeLayerAt(index) ?: return null
        return PagLayer(removed)
    }

    actual fun removeAllLayers() {
        nativeComposition.removeAllLayers()
    }

    actual fun swapLayer(layer1: PagLayer, layer2: PagLayer) {
        nativeComposition.swapLayer(layer1.nativeLayer, layer2.nativeLayer)
    }

    actual fun swapLayerAt(index1: Int, index2: Int) {
        nativeComposition.swapLayerAt(index1, index2)
    }

    @Suppress("UNCHECKED_CAST")
    actual fun getLayersByName(name: String): List<PagLayer> {
        val layers = nativeComposition.getLayersByName(name) as? List<org.libpag.ios.PAGLayer> ?: return emptyList()
        return layers.map { PagLayer(it) }
    }

    actual fun audioBytes(): ByteArray? {
        val data = nativeComposition.audioBytes() ?: return null
        val length = data.length.toInt()
        if (length == 0) return ByteArray(0)
        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
        return bytes
    }

    actual fun audioStartTime(): Long {
        return nativeComposition.audioStartTime()
    }

    actual companion object {
        actual fun make(width: Int, height: Int): PagComposition {
            val comp = PAGComposition.Make(CGSizeMake(width.toDouble(), height.toDouble()))
            if (comp == null) {
                println("[PagKit] WARN: PAGComposition.Make($width, $height) returned null, fallback to default")
                return PagComposition(PAGComposition())
            }
            return PagComposition(comp)
        }
    }
}
