package io.github.pagkit.model

/**
 * 跨平台 PAG 字体注册与管理对象。
 * 用于注册外部 TTF/OTF 自定义字体，确保文字图层渲染样式与 AE 设计完全一致。
 */
expect class PagFont {
    val fontFamily: String
    val fontStyle: String

    companion object {
        /** 从本地文件绝对路径注册字体 */
        fun registerFont(fontPath: String): PagFont?

        /** 从本地文件绝对路径注册指定家族与风格的字体 */
        fun registerFont(fontPath: String, fontFamily: String, fontStyle: String): PagFont?

        /** 从二进制字节数组注册字体 */
        fun registerFont(bytes: ByteArray): PagFont?

        /** 从二进制字节数组注册指定家族与风格的字体 */
        fun registerFont(bytes: ByteArray, fontFamily: String, fontStyle: String): PagFont?

        /** 注销已注册的字体 */
        fun unregisterFont(font: PagFont)
    }
}
