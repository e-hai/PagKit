package io.github.pagkit.cache

import io.github.pagkit.composition.PagFile

/**
 * 跨平台 PAG 磁盘缓存与网络加载器封装。
 */
expect object PagDiskCache {
    /** 设置磁盘缓存目录（为 null 则使用平台默认缓存目录） */
    fun setCacheDir(dirPath: String?)

    /** 获取磁盘缓存上限字节数（默认 1GB） */
    fun getMaxDiskSize(): Long

    /** 设置磁盘缓存上限字节数 */
    fun setMaxDiskSize(sizeBytes: Long)

    /** 清空所有 PAG 磁盘缓存 */
    fun removeAll()

    /**
     * 异步加载/预加载指定网络 URL 的 PAG 动效，自动走磁盘缓存。
     */
    suspend fun loadFromUrl(url: String): PagFile?
}
