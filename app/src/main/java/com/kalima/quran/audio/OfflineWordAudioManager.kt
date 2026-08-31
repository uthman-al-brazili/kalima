package com.kalima.quran.audio

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kalima.quran.data.QuranVerseAudioLocation
import com.kalima.quran.data.QuranWordAudioLocation
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class OfflineWordAudioDownloadState(
    val running: Boolean = false,
    val completed: Int = 0,
    val total: Int = 0,
    val downloaded: Int = 0,
    val alreadyAvailable: Int = 0,
    val failed: Int = 0,
)

class OfflineWordAudioManager private constructor(context: Context) {
    private val applicationContext = context.applicationContext
    private val workManager = WorkManager.getInstance(applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val enqueuePending = AtomicBoolean(false)
    private val cancelPending = AtomicBoolean(false)
    private val preferences = applicationContext.getSharedPreferences(
        DOWNLOAD_PREFERENCES,
        Context.MODE_PRIVATE,
    )
    private val trackedWorkId = MutableStateFlow(
        preferences.getString(KEY_CURRENT_WORK_ID, null)?.let { id ->
            runCatching { UUID.fromString(id) }.getOrNull()
        },
    )
    private val mutableState = MutableStateFlow(
        OfflineWordAudioDownloadState(running = trackedWorkId.value != null),
    )
    val state: StateFlow<OfflineWordAudioDownloadState> = mutableState.asStateFlow()

    init {
        scope.launch {
            trackedWorkId.collectLatest { workId ->
                if (workId == null) {
                    if (!enqueuePending.get()) {
                        mutableState.value = OfflineWordAudioDownloadState()
                    }
                } else {
                    workManager.getWorkInfoByIdFlow(workId).collectLatest { workInfo ->
                        if (workInfo != null) {
                            mutableState.value = workInfo.toDownloadState()
                        } else if (!enqueuePending.get()) {
                            mutableState.value = OfflineWordAudioDownloadState()
                        }
                    }
                }
            }
        }
    }

    fun download(
        wordLocations: List<QuranWordAudioLocation>,
        verseLocations: List<QuranVerseAudioLocation> = emptyList(),
    ) {
        if (mutableState.value.running || !enqueuePending.compareAndSet(false, true)) return
        cancelPending.set(false)

        val request = OfflineAudioDownloadRequest(
            wordLocations = wordLocations.distinctBy(QuranWordAudioLocation::fileName),
            verseLocations = verseLocations.distinctBy(QuranVerseAudioLocation::fileName),
        )
        if (request.total == 0) {
            enqueuePending.set(false)
            return
        }
        mutableState.value = OfflineWordAudioDownloadState(running = true, total = request.total)

        scope.launch {
            val workId = UUID.randomUUID()
            try {
                OfflineAudioDownloadRequestStore.write(
                    applicationContext,
                    workId.toString(),
                    request,
                )
                if (cancelPending.get()) {
                    OfflineAudioDownloadRequestStore.delete(applicationContext, workId.toString())
                    mutableState.value = mutableState.value.copy(running = false)
                    return@launch
                }
                val work = OneTimeWorkRequestBuilder<OfflineWordAudioDownloadWorker>()
                    .setId(workId)
                    .addTag("$TOTAL_TAG${request.total}")
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    )
                    .setInputData(
                        workDataOf(
                            OfflineWordAudioDownloadWorker.KEY_REQUEST_ID to workId.toString(),
                            OfflineWordAudioDownloadWorker.KEY_TOTAL to request.total,
                        ),
                    )
                    .build()
                check(
                    preferences.edit()
                        .putString(KEY_CURRENT_WORK_ID, workId.toString())
                        .commit(),
                ) { "Unable to remember offline audio work" }
                trackedWorkId.value = workId
                workManager
                    .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, work)
                    .result
                    .get()
                if (cancelPending.get()) workManager.cancelWorkById(workId)
            } catch (_: Exception) {
                OfflineAudioDownloadRequestStore.delete(applicationContext, workId.toString())
                mutableState.value = OfflineWordAudioDownloadState(
                    total = request.total,
                    failed = request.total,
                )
            } finally {
                enqueuePending.set(false)
            }
        }
    }

    fun cancel() {
        cancelPending.set(true)
        enqueuePending.set(false)
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    private fun WorkInfo.toDownloadState(): OfflineWordAudioDownloadState {
        val counts = if (state == WorkInfo.State.SUCCEEDED || state == WorkInfo.State.FAILED) {
            outputData
        } else {
            progress
        }
        return OfflineWordAudioDownloadState(
            running = !state.isFinished,
            completed = counts.getInt(OfflineWordAudioDownloadWorker.KEY_COMPLETED, 0),
            total = counts.getInt(
                OfflineWordAudioDownloadWorker.KEY_TOTAL,
                tags.firstNotNullOfOrNull { tag ->
                    tag.removePrefix(TOTAL_TAG).takeIf { it != tag }?.toIntOrNull()
                } ?: 0,
            ),
            downloaded = counts.getInt(OfflineWordAudioDownloadWorker.KEY_DOWNLOADED, 0),
            alreadyAvailable = counts.getInt(
                OfflineWordAudioDownloadWorker.KEY_ALREADY_AVAILABLE,
                0,
            ),
            failed = counts.getInt(OfflineWordAudioDownloadWorker.KEY_FAILED, 0),
        )
    }

    companion object {
        const val ESTIMATED_BYTES_PER_WORD = 68_000L
        const val ESTIMATED_BYTES_PER_VERSE = 500_000L

        private const val BYTES_PER_MEGABYTE = 1_000_000L
        private const val UNIQUE_WORK_NAME = "offline-quran-audio-download"
        private const val TOTAL_TAG = "offline-audio-total:"
        private const val DOWNLOAD_PREFERENCES = "offline_audio_download"
        private const val KEY_CURRENT_WORK_ID = "current_work_id"

        @Volatile
        private var instance: OfflineWordAudioManager? = null

        fun get(context: Context): OfflineWordAudioManager = instance ?: synchronized(this) {
            instance ?: OfflineWordAudioManager(context).also { instance = it }
        }

        fun estimatedMegabytes(wordCount: Int, verseCount: Int = 0): Long =
            ((wordCount * ESTIMATED_BYTES_PER_WORD) +
                (verseCount * ESTIMATED_BYTES_PER_VERSE) +
                BYTES_PER_MEGABYTE - 1) / BYTES_PER_MEGABYTE
    }
}
