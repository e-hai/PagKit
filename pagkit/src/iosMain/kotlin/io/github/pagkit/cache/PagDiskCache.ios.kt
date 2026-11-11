@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.pagkit.cache

import io.github.pagkit.composition.PagFile
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.libpag.ios.PAGDiskCache as NativePAGDiskCache
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToFile

actual object PagDiskCache {
    private val httpClient by lazy { HttpClient(Darwin) }

    actual fun setCacheDir(dirPath: String?) {
        // iOS libpag 内部自动使用系统沙盒 Cache 目录
    }

    actual fun getMaxDiskSize(): Long {
        return NativePAGDiskCache.MaxDiskSize().toLong()
    }

    actual fun setMaxDiskSize(sizeBytes: Long) {
        NativePAGDiskCache.SetMaxDiskSize(sizeBytes.toULong())
    }

    actual fun removeAll() {
        NativePAGDiskCache.RemoveAll()
        try {
            val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
            val cacheBase = paths.firstOrNull() as? String ?: return
            val cacheDir = "$cacheBase/pagkit_net_cache"
            val fileManager = NSFileManager.defaultManager
            if (fileManager.fileExistsAtPath(cacheDir)) {
                fileManager.removeItemAtPath(cacheDir, error = null)
            }
        } catch (_: Throwable) {}
    }

    @OptIn(BetaInteropApi::class)
    actual suspend fun loadFromUrl(url: String): PagFile? = withContext(Dispatchers.IO) {
        try {
            val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
            val cacheBase = paths.firstOrNull() as? String ?: return@withContext null
            val cacheDir = "$cacheBase/pagkit_net_cache"
            val fileManager = NSFileManager.defaultManager

            if (!fileManager.fileExistsAtPath(cacheDir)) {
                fileManager.createDirectoryAtPath(cacheDir, withIntermediateDirectories = true, attributes = null, error = null)
            }

            val safeKey = url.hashCode().toUInt().toString(16)
            val cachedPath = "$cacheDir/$safeKey.pag"

            if (fileManager.fileExistsAtPath(cachedPath)) {
                return@withContext PagFile.loadFromPath(cachedPath)
            }

            val response = httpClient.get(url)
            val bytes = response.readRawBytes()
            if (bytes.isNotEmpty()) {
                val data = bytes.usePinned { pinned ->
                    NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
                }
                data.writeToFile(cachedPath, atomically = true)
                PagFile.loadFromPath(cachedPath)
            } else {
                null
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }
}
