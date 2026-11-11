package io.github.pagkit.composition

/**
 * 跨平台 PAG 合成容器封装（支持多 PAG 图层自由组合与层级管理）。
 */
expect open class PagComposition : PagLayer {
    val width: Int
    val height: Int
    val numChildren: Int

    /** 设置合成宽高 */
    fun setContentSize(width: Int, height: Int)

    /** 获取指定索引位置的子图层 */
    fun getLayerAt(index: Int): PagLayer?

    /** 添加子图层至最顶层 */
    fun addLayer(layer: PagLayer): Boolean

    /** 添加子图层至指定索引位置 */
    fun addLayerAt(layer: PagLayer, index: Int): Boolean

    /** 移除指定子图层 */
    fun removeLayer(layer: PagLayer): PagLayer?

    /** 移除指定索引位置的子图层 */
    fun removeLayerAt(index: Int): PagLayer?

    /** 移除全部子图层 */
    fun removeAllLayers()

    /** 交换两个子图层的层级 */
    fun swapLayer(layer1: PagLayer, layer2: PagLayer)

    /** 交换指定两个索引位置的子图层 */
    fun swapLayerAt(index1: Int, index2: Int)

    /** 按图层名称匹配子图层列表 */
    fun getLayersByName(name: String): List<PagLayer>

    /** 获取动画内嵌的音频二进制数据 (AAC in MPEG-4)，若无音频则返回 null */
    fun audioBytes(): ByteArray?

    /** 获取首帧音频在动画时间轴上的起始播放时间（微秒） */
    fun audioStartTime(): Long

    companion object {
        /** 创建指定宽高的空合成容器 */
        fun make(width: Int, height: Int): PagComposition
    }
}
