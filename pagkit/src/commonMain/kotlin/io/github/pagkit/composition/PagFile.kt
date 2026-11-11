package io.github.pagkit.composition

import io.github.pagkit.model.PagLayerType
import io.github.pagkit.model.PagTextData
import io.github.pagkit.model.PagTimeStretchMode

/**
 * 跨平台 PAG 文件模型封装（继承自 PagComposition，支持动态图文编辑）。
 */
expect class PagFile : PagComposition {
    val tagLevel: Int
    val numTexts: Int
    val numImages: Int
    val numVideos: Int
    val filePath: String

    var timeStretchMode: PagTimeStretchMode

    /** 设置总时长（微秒） */
    fun setDurationUs(durationUs: Long)

    /** 获取指定可编辑索引的文本数据 */
    fun getTextData(editableTextIndex: Int): PagTextData?

    /** 替换指定索引的可编辑文本 */
    fun replaceText(editableTextIndex: Int, textData: PagTextData?)

    /** 替换指定索引的可编辑图片 */
    fun replaceImage(editableImageIndex: Int, image: PagImage?)

    /** 按图层名称替换图片 */
    fun replaceImageByName(layerName: String, image: PagImage?)

    /** 获取指定图层类型的所有可编辑索引 */
    fun getEditableIndices(layerType: PagLayerType): List<Int>

    /** 获取指定可编辑索引和类型的图层列表 */
    fun getLayersByEditableIndex(editableIndex: Int, layerType: PagLayerType): List<PagLayer>

    /** 拷贝一份原始文件实例，用于独立修改互不干扰 */
    fun copyOriginal(): PagFile

    companion object {
        /** 最大支持的 tag level */
        fun maxSupportedTagLevel(): Int

        /** 从本地文件绝对路径加载 */
        fun loadFromPath(path: String): PagFile?

        /** 从二进制字节数组加载 */
        fun loadFromBytes(bytes: ByteArray): PagFile?

        /** 从应用 Assets（Android assets / iOS Main Bundle 资源）加载 */
        fun loadFromAssets(assetName: String): PagFile?
    }
}
