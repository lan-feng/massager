package com.massager.app.data.local

// 文件说明：管理磁盘缓存文件的统一工具。
import android.content.Context
import com.massager.app.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class AppCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun cacheSnapshot(): CacheSnapshot = withContext(ioDispatcher) {
        val totalBytes = cacheDirectories().sumOf { directorySize(it) }
        CacheSnapshot(totalBytes)
    }

    suspend fun clearCache(): CacheClearResult = withContext(ioDispatcher) {
        val dirs = cacheDirectories()
        val beforeBytes = dirs.sumOf { directorySize(it) }
        dirs.forEach { clearDirectory(it) }
        val afterBytes = dirs.sumOf { directorySize(it) }
        CacheClearResult(
            freedBytes = (beforeBytes - afterBytes).coerceAtLeast(0),
            remainingBytes = afterBytes.coerceAtLeast(0)
        )
    }

    private fun cacheDirectories(): List<File> = buildList {
        add(context.cacheDir)
        add(context.codeCacheDir)
        context.externalCacheDirs?.forEach { dir ->
            if (dir != null) add(dir)
        }
    }

    private fun directorySize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        val files = dir.listFiles() ?: return 0L
        var total = 0L
        for (child in files) {
            total += if (child.isDirectory) {
                directorySize(child)
            } else {
                child.length()
            }
        }
        return total
    }

    private fun clearDirectory(dir: File?) {
        if (dir == null || !dir.exists()) return
        val files = dir.listFiles() ?: return
        for (child in files) {
            if (child.isDirectory) {
                clearDirectory(child)
            }
            child.delete()
        }
    }
}

data class CacheSnapshot(val bytes: Long) {
    val display: String = formatBytes(bytes)
}

data class CacheClearResult(
    val freedBytes: Long,
    val remainingBytes: Long
) {
    val freedDisplay: String = formatBytes(freedBytes)
    val remainingDisplay: String = formatBytes(remainingBytes)
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return if (index == 0) {
        "${bytes} B"
    } else {
        String.format("%.1f %s", value, units[index])
    }
}
