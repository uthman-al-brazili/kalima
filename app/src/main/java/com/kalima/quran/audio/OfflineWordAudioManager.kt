package com.kalima.quran.audio

import android.content.Context
import com.kalima.quran.data.QuranVerseAudioLocation
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
    private val wordStore = OfflineWordAudioStore.get(context)
    private val verseStore = OfflineVerseAudioStore.get(context)
    private val mutableState = MutableStateFlow(OfflineWordAudioDownloadState())
    val state: StateFlow<OfflineWordAudioDownloadState> = mutableState.asStateFlow()

    suspend fun download(
        wordLocations: List<QuranWordAudioLocation>,
        verseLocations: List<QuranVerseAudioLocation> = emptyList(),
    ) {
        val distinctWordLocations = wordLocations.distinctBy(QuranWordAudioLocation::fileName)
        val distinctVerseLocations = verseLocations.distinctBy(QuranVerseAudioLocation::fileName)
        mutableState.value = OfflineWordAudioDownloadState(
            running = true,
            total = distinctWordLocations.size + distinctVerseLocations.size,
        )
        try {
            distinctWordLocations.forEach { location ->
                currentCoroutineContext().ensureActive()
                record(wordStore.ensureCached(location))
            }
            distinctVerseLocations.forEach { location ->
                currentCoroutineContext().ensureActive()
                record(verseStore.ensureCached(location))
            }
        } finally {
            mutableState.value = mutableState.value.copy(running = false)
        }
    }

    private fun record(result: WordAudioCacheResult) {
        val previous = mutableState.value
        mutableState.value = previous.copy(
            completed = previous.completed + 1,
            downloaded = previous.downloaded + if (result == WordAudioCacheResult.Downloaded) 1 else 0,
            alreadyAvailable = previous.alreadyAvailable +
                if (result == WordAudioCacheResult.AlreadyCached) 1 else 0,
            failed = previous.failed + if (result == WordAudioCacheResult.Failed) 1 else 0,
        )
    }

    companion object {
        const val ESTIMATED_BYTES_PER_WORD = 68_000L
        const val ESTIMATED_BYTES_PER_VERSE = 500_000L

        fun estimatedMegabytes(wordCount: Int, verseCount: Int = 0): Long =
            ((wordCount * ESTIMATED_BYTES_PER_WORD) +
                (verseCount * ESTIMATED_BYTES_PER_VERSE) +
                BYTES_PER_MEGABYTE - 1) / BYTES_PER_MEGABYTE

        private const val BYTES_PER_MEGABYTE = 1_000_000L
    }
}
