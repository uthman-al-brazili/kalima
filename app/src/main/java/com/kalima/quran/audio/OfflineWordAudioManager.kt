package com.kalima.quran.audio

import android.content.Context
import com.kalima.quran.data.QuranWordAudioLocation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OfflineWordAudioDownloadState(
    val running: Boolean = false,
    val completed: Int = 0,
    val total: Int = 0,
    val downloaded: Int = 0,
    val alreadyAvailable: Int = 0,
    val failed: Int = 0,
)

class OfflineWordAudioManager(context: Context) {
    private val store = OfflineWordAudioStore.get(context)
    private val mutableState = MutableStateFlow(OfflineWordAudioDownloadState())
    val state: StateFlow<OfflineWordAudioDownloadState> = mutableState.asStateFlow()

    suspend fun download(locations: List<QuranWordAudioLocation>) {
        val distinctLocations = locations.distinctBy(QuranWordAudioLocation::fileName)
        mutableState.value = OfflineWordAudioDownloadState(
            running = true,
            total = distinctLocations.size,
        )
        try {
            distinctLocations.forEach { location ->
                currentCoroutineContext().ensureActive()
                val previous = mutableState.value
                val result = store.ensureCached(location)
                mutableState.value = previous.copy(
                    completed = previous.completed + 1,
                    downloaded = previous.downloaded + if (result == WordAudioCacheResult.Downloaded) 1 else 0,
                    alreadyAvailable = previous.alreadyAvailable +
                        if (result == WordAudioCacheResult.AlreadyCached) 1 else 0,
                    failed = previous.failed + if (result == WordAudioCacheResult.Failed) 1 else 0,
                )
            }
        } finally {
            mutableState.value = mutableState.value.copy(running = false)
        }
    }

    companion object {
        const val ESTIMATED_BYTES_PER_WORD = 68_000L

        fun estimatedMegabytes(wordCount: Int): Long =
            ((wordCount * ESTIMATED_BYTES_PER_WORD) + BYTES_PER_MEGABYTE - 1) / BYTES_PER_MEGABYTE

        private const val BYTES_PER_MEGABYTE = 1_000_000L
    }
}
