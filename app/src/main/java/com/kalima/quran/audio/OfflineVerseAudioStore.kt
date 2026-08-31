package com.kalima.quran.audio

import android.content.Context
import com.kalima.quran.data.QuranVerseAudioLocation
import java.io.File
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

internal class OfflineVerseAudioStore private constructor(context: Context) {
    private val rootDirectory = File(context.filesDir, DIRECTORY_NAME)
    private val downloadMutex = Mutex()
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val backgroundDownloads = ConcurrentHashMap.newKeySet<String>()

    fun cachedFile(location: QuranVerseAudioLocation): File? =
        destinationFor(location).takeIf(::isUsableAudioFile)

    suspend fun ensureCached(location: QuranVerseAudioLocation): WordAudioCacheResult =
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

    fun cacheInBackground(location: QuranVerseAudioLocation) {
        if (cachedFile(location) != null || !backgroundDownloads.add(location.fileName)) return
        backgroundScope.launch {
            try {
                ensureCached(location)
            } finally {
                backgroundDownloads.remove(location.fileName)
            }
        }
    }

    private suspend fun download(location: QuranVerseAudioLocation) = withContext(Dispatchers.IO) {
        val destination = destinationFor(location)
        val directory = requireNotNull(destination.parentFile)
        check(directory.isDirectory || directory.mkdirs()) { "Unable to create offline audio directory" }
        val temporary = File(directory, "${destination.name}.${System.nanoTime()}.part")
        val connection = openPinnedAudioConnection(
            url = URL(location.hussaryUrl),
            expectedHost = AUDIO_HOST,
            userAgent = USER_AGENT,
            connectTimeoutMs = CONNECT_TIMEOUT_MS,
            readTimeoutMs = READ_TIMEOUT_MS,
        )
        try {
            validateAudioResponse(connection, MAX_AUDIO_BYTES)
            connection.inputStream.use { input ->
                temporary.outputStream().buffered().use { output ->
                    copyWithLimit(input, output, MAX_AUDIO_BYTES)
                }
            }
            check(isUsableAudioFile(temporary)) { "Downloaded ayah audio is incomplete" }
            if (destination.exists()) check(destination.delete()) { "Unable to replace incomplete ayah audio" }
            check(temporary.renameTo(destination)) { "Unable to store downloaded ayah audio" }
        } finally {
            connection.disconnect()
            if (temporary.exists()) temporary.delete()
        }
    }

    private fun destinationFor(location: QuranVerseAudioLocation): File = File(
        File(rootDirectory, location.surah.toString().padStart(3, '0')),
        location.fileName,
    )

    private fun isUsableAudioFile(file: File): Boolean =
        file.isFile &&
            file.length() in MIN_AUDIO_BYTES..MAX_AUDIO_BYTES &&
            hasMp3Signature(file)

    companion object {
        private const val DIRECTORY_NAME = "quran-verse-audio-hussary"
        private const val AUDIO_HOST = "everyayah.com"
        private const val MIN_AUDIO_BYTES = 4_096L
        private const val MAX_AUDIO_BYTES = 16L * 1024 * 1024
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 60_000
        private const val USER_AGENT = "Kalima Android Al-Hussary ayah audio"

        @Volatile
        private var instance: OfflineVerseAudioStore? = null

        fun get(context: Context): OfflineVerseAudioStore = instance ?: synchronized(this) {
            instance ?: OfflineVerseAudioStore(context.applicationContext).also { instance = it }
        }
    }
}
