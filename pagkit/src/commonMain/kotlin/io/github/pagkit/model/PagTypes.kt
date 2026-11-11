package io.github.pagkit.model

import io.github.pagkit.composition.PagComposition

/**
 * PAG 缩放模式，严格对齐原生 PAGScaleMode。
 */
enum class PagScaleMode(val nativeValue: Int) {
    /** 不缩放，从左上角开始裁剪 */
    None(0),
    /** 拉伸填满整个视图（不保持宽高比，可能变形） */
    Stretch(1),
    /** 等比缩放以完整显示（黑边留白模式，默认） */
    LetterBox(2),
    /** 等比缩放以填满视图（居中裁剪超出部分） */
    Zoom(3);

    companion object {
        fun fromNative(value: Int): PagScaleMode =
            entries.firstOrNull { it.nativeValue == value } ?: LetterBox
    }
}

/**
 * PAG 时间拉伸模式，严格对齐原生 PAGTimeStretchMode。
 */
enum class PagTimeStretchMode(val nativeValue: Int) {
    None(0),
    Scale(1),
    Repeat(2),
    RepeatInverted(3);

    companion object {
        fun fromNative(value: Int): PagTimeStretchMode =
            entries.firstOrNull { it.nativeValue == value } ?: Repeat
    }
}

/**
 * 图层类型枚举，对齐 PAGLayerType。
 */
enum class PagLayerType(val nativeValue: Int) {
    Unknown(0),
    Null(1),
    Solid(2),
    Text(3),
    Shape(4),
    Image(5),
    PreCompose(6);

    companion object {
        fun fromNative(value: Int): PagLayerType =
            entries.firstOrNull { it.nativeValue == value } ?: Unknown
    }
}

/**
 * 统一的数据源描述（支持多种途径加载 PAG 动效）。
 */
sealed interface PagSource {
    /** 从本地文件绝对路径加载 */
    data class FilePath(val path: String) : PagSource

    /** 从平台内置 Assets（Android assets / iOS Main Bundle 资源）加载 */
    data class Asset(val assetName: String) : PagSource

    /** 从网络 URL 异步下载并自动缓存加载 */
    data class Url(val url: String) : PagSource

    /** 从内存二进制数组加载（适合 Compose Resources 的 Res.readBytes(...)） */
    class Bytes(val bytes: ByteArray, val cacheKey: String = bytes.contentHashCode().toString()) : PagSource {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Bytes) return false
            return cacheKey == other.cacheKey && bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int = cacheKey.hashCode() * 31 + bytes.contentHashCode()
    }

    /** 直接挂载已构建好的 PagComposition（支持多动效组合、时间轴偏移与图层叠加） */
    data class Composition(val composition: PagComposition) : PagSource
}
