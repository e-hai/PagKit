package io.github.pagkit.cache

import io.github.pagkit.PagKitAndroid
import io.github.pagkit.composition.PagFile
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.libpag.PAGDiskCache as NativePAGDiskCache
import java.io.File
import java.security.MessageDigest

actual object PagDiskCache {
    private val httpClient by lazy { HttpClient(OkHttp) }

    actual fun setCacheDir(dirPath: String?) {
        // Android 端原生由 LibraryLoadUtils 自动使用应用 CacheDir
    }

    actual fun getMaxDiskSize(): Long {
        return NativePAGDiskCache.MaxDiskSize()
    }

    actual fun setMaxDiskSize(sizeBytes: Long) {
        NativePAGDiskCache.SetMaxDiskSize(sizeBytes)
    }

    actual fun removeAll() {
        NativePAGDiskCache.RemoveAll()
        try {
            val context = PagKitAndroid.getAppContext()
            val cacheDir = File(context.cacheDir, "pagkit_net_cache")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
        } catch (_: Throwable) {}
    }

    actual suspend fun loadFromUrl(url: String): PagFile? = withContext(Dispatchers.IO) {
        try {
            val context = PagKitAndroid.getAppContext()
            val cacheDir = File(context.cacheDir, "pagkit_net_cache").apply { mkdirs() }
            val hash = MessageDigest.getInstance("MD5").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
            val cachedFile = File(cacheDir, "$hash.pag")

            if (cachedFile.exists() && cachedFile.length() > 0) {
                return@withContext PagFile.loadFromPath(cachedFile.absolutePath)
            }

            val response = httpClient.get(url)
            val bytes = response.readRawBytes()
            if (bytes.isNotEmpty()) {
                cachedFile.writeBytes(bytes)
                PagFile.loadFromPath(cachedFile.absolutePath)
            } else {
                null
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }
}
