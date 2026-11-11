package io.github.pagkit.model

/**
 * 文本图层数据描述，用于读取与替换 PAG 动效中的可编辑文本属性。
 */
data class PagTextData(
    /** 文本内容 */
    val text: String,
    /** 字体像素大小 */
    val fontSize: Float = 24f,
    /** 填充颜色 (ARGB 整数，例如 0xFFFFFFFF) */
    val fillColorArgb: Int = 0xFFFFFFFF.toInt(),
    /** 描边颜色 (ARGB 整数) */
    val strokeColorArgb: Int = 0xFF000000.toInt(),
    /** 描边粗细 */
    val strokeWidth: Float = 0f,
    /** 字体名称 */
    val fontFamily: String = "",
    /** 字体样式，如 Bold, Italic */
    val fontStyle: String = "",
    /** 仿粗体 */
    val fauxBold: Boolean = false,
    /** 仿斜体 */
    val fauxItalic: Boolean = false,
    /** 行距 */
    val leading: Float = 0f,
    /** 字间距 */
    val tracking: Float = 0f,
    /** 背景颜色 (ARGB 整数) */
    val backgroundColorArgb: Int = 0,
    /** 背景透明度 (0 - 255) */
    val backgroundAlpha: Int = 0
)
