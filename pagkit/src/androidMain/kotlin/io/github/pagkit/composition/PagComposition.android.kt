package io.github.pagkit.composition

import org.libpag.PAGComposition

actual open class PagComposition internal constructor(
    internal val nativeComposition: PAGComposition
) : PagLayer(nativeComposition) {

    actual val width: Int
        get() = nativeComposition.width()

    actual val height: Int
        get() = nativeComposition.height()

    actual val numChildren: Int
        get() = nativeComposition.numChildren()

    actual fun setContentSize(width: Int, height: Int) {
        nativeComposition.setContentSize(width, height)
    }

    actual fun getLayerAt(index: Int): PagLayer? {
        val l = nativeComposition.getLayerAt(index) ?: return null
        return PagLayer(l)
    }

    actual fun addLayer(layer: PagLayer): Boolean {
        nativeComposition.addLayer(layer.nativeLayer)
        return true
    }

    actual fun addLayerAt(layer: PagLayer, index: Int): Boolean {
        nativeComposition.addLayerAt(layer.nativeLayer, index)
        return true
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

    actual fun getLayersByName(name: String): List<PagLayer> {
        val layers = nativeComposition.getLayersByName(name) ?: return emptyList()
        return layers.map { PagLayer(it) }
    }

    actual companion object {
        actual fun make(width: Int, height: Int): PagComposition {
            val comp = PAGComposition.Make(width, height)
            return PagComposition(comp)
        }
    }
}
