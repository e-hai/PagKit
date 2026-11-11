package io.github.pagkit.composition

import io.github.pagkit.model.PagScaleMode

/**
 * 跨平台 PAG 图片图层替换对象封装。
 */
expect class PagImage {
    val width: Int
    val height: Int
    var scaleMode: PagScaleMode

    companion object {
        /** 从本地文件绝对路径加载图片 */
        fun fromPath(path: String): PagImage?

        /** 从内存二进制数组加载图片 */
        fun fromBytes(bytes: ByteArray): PagImage?
    }
}
