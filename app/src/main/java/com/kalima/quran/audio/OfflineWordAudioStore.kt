package com.kalima.quran.audio

import android.content.Context
import com.kalima.quran.data.QuranWordAudioLocation
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal enum class WordAudioCacheResult {
    AlreadyCached,
    Downloaded,
    Failed,
}

internal class OfflineWordAudioStore private constructor(context: Context) {
    private val rootDirectory = File(context.filesDir, DIRECTORY_NAME)
    private val downloadMutex = Mutex()
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val backgroundDownloads = ConcurrentHashMap.newKeySet<String>()

    fun cachedFile(location: QuranWordAudioLocation): File? =
        destinationFor(location).takeIf(::isUsableAudioFile)

    suspend fun ensureCached(location: QuranWordAudioLocation): WordAudioCacheResult =
        downloadMutex.withLock {
            if (cachedFile(location) != null) return@withLock WordAudioCacheResult.AlreadyCached
            try {
                download(location)
                WordAudioCacheResult.Downloaded
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                WordAudioCacheResult.Failed
            }
        }

    fun cacheInBackground(location: QuranWordAudioLocation) {
        if (cachedFile(location) != null || !backgroundDownloads.add(location.fileName)) return
        backgroundScope.launch {
            try {
                ensureCached(location)
            } finally {
                backgroundDownloads.remove(location.fileName)
            }
        }
    }

    private suspend fun download(location: QuranWordAudioLocation) = withContext(Dispatchers.IO) {
        val destination = destinationFor(location)
        val directory = requireNotNull(destination.parentFile)
        check(directory.isDirectory || directory.mkdirs()) { "Unable to create offline audio directory" }
        val temporary = File(directory, "${destination.name}.${System.nanoTime()}.part")
        val connection = URL(location.quranComUrl).openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = true
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            check(connection.responseCode == HttpURLConnection.HTTP_OK) {
                "Quran.com audio returned HTTP ${connection.responseCode}"
            }
            connection.inputStream.use { input ->
                temporary.outputStream().buffered().use { output -> input.copyTo(output) }
            }
            check(isUsableAudioFile(temporary)) { "Downloaded word audio is incomplete" }
            if (destination.exists()) check(destination.delete()) { "Unable to replace incomplete word audio" }
            check(temporary.renameTo(destination)) { "Unable to store downloaded word audio" }
        } finally {
            connection.disconnect()
            if (temporary.exists()) temporary.delete()
        }
    }

    private fun destinationFor(location: QuranWordAudioLocation): File = File(
        File(rootDirectory, location.surah.toString().padStart(3, '0')),
        location.fileName,
    )

    private fun isUsableAudioFile(file: File): Boolean = file.isFile && file.length() >= MIN_AUDIO_BYTES

    companion object {
        private const val DIRECTORY_NAME = "quran-word-audio"
        private const val MIN_AUDIO_BYTES = 4_096L
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val USER_AGENT = "Kalima Android offline word audio"

        @Volatile
        private var instance: OfflineWordAudioStore? = null

        fun get(context: Context): OfflineWordAudioStore = instance ?: synchronized(this) {
            instance ?: OfflineWordAudioStore(context.applicationContext).also { instance = it }
        }
    }
}
