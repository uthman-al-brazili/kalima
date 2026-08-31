package com.kalima.quran.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kalima.quran.MainActivity
import com.kalima.quran.R
import com.kalima.quran.data.QuranVerseAudioLocation
import com.kalima.quran.data.QuranWordAudioLocation
import com.kalima.quran.localization.LanguageManager
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal data class OfflineAudioDownloadRequest(
    val wordLocations: List<QuranWordAudioLocation>,
    val verseLocations: List<QuranVerseAudioLocation>,
) {
    val total: Int get() = wordLocations.size + verseLocations.size
}

internal object OfflineAudioDownloadRequestStore {
    fun write(context: Context, requestId: String, request: OfflineAudioDownloadRequest) {
        val destination = requestFile(context, requestId)
        val directory = requireNotNull(destination.parentFile)
        check(directory.isDirectory || directory.mkdirs()) {
            "Unable to create offline audio request directory"
        }
        val temporary = File(directory, "$requestId.tmp")
        try {
            DataOutputStream(BufferedOutputStream(FileOutputStream(temporary))).use { output ->
                output.writeInt(FORMAT_VERSION)
                output.writeInt(request.wordLocations.size)
                request.wordLocations.forEach { location ->
                    output.writeInt(location.surah)
                    output.writeInt(location.ayah)
                    output.writeInt(location.word)
                }
                output.writeInt(request.verseLocations.size)
                request.verseLocations.forEach { location ->
                    output.writeInt(location.surah)
                    output.writeInt(location.ayah)
                }
            }
            if (destination.exists()) check(destination.delete()) {
                "Unable to replace offline audio request"
            }
            check(temporary.renameTo(destination)) { "Unable to store offline audio request" }
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    fun read(context: Context, requestId: String): OfflineAudioDownloadRequest {
        DataInputStream(
            BufferedInputStream(FileInputStream(requestFile(context, requestId))),
        ).use { input ->
            check(input.readInt() == FORMAT_VERSION) { "Unsupported offline audio request" }
            val wordCount = input.readCount()
            val words = List(wordCount) {
                QuranWordAudioLocation(input.readInt(), input.readInt(), input.readInt())
            }
            val verseCount = input.readCount()
            val verses = List(verseCount) {
                QuranVerseAudioLocation(input.readInt(), input.readInt())
            }
            return OfflineAudioDownloadRequest(words, verses)
        }
    }

    fun delete(context: Context, requestId: String) {
        requestFile(context, requestId).delete()
    }

    private fun DataInputStream.readCount(): Int = readInt().also { count ->
        require(count in 0..MAX_LOCATIONS) { "Invalid offline audio location count: $count" }
    }

    private fun requestFile(context: Context, requestId: String): File {
        require(requestId.matches(UUID_PATTERN)) { "Invalid offline audio request ID" }
        return File(File(context.filesDir, DIRECTORY_NAME), "$requestId.bin")
    }

    private const val FORMAT_VERSION = 1
    private const val MAX_LOCATIONS = 100_000
    private const val DIRECTORY_NAME = "offline-audio-requests"
    private val UUID_PATTERN = Regex("[0-9a-fA-F-]{36}")
}

class OfflineWordAudioDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    private val wordStore = OfflineWordAudioStore.get(appContext)
    private val verseStore = OfflineVerseAudioStore.get(appContext)

    override suspend fun doWork(): Result {
        val requestId = inputData.getString(KEY_REQUEST_ID) ?: return Result.failure()
        val expectedTotal = inputData.getInt(KEY_TOTAL, 0)
        var state = OfflineWordAudioDownloadState(running = true, total = expectedTotal)

        try {
            setForeground(foregroundInfo(state))
            val request = OfflineAudioDownloadRequestStore.read(applicationContext, requestId)
            state = state.copy(total = request.total)
            publish(state)
            request.wordLocations.forEach { location ->
                currentCoroutineContext().ensureActive()
                state = state.record(wordStore.ensureCached(location))
                publish(state)
            }
            request.verseLocations.forEach { location ->
                currentCoroutineContext().ensureActive()
                state = state.record(verseStore.ensureCached(location))
                publish(state)
            }
            return Result.success(state.toWorkData())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            state = state.copy(
                completed = state.total,
                failed = state.failed + (state.total - state.completed).coerceAtLeast(0),
            )
            return Result.failure(state.toWorkData())
        } finally {
            OfflineAudioDownloadRequestStore.delete(applicationContext, requestId)
        }
    }

    private suspend fun publish(state: OfflineWordAudioDownloadState) {
        setProgress(state.toWorkData())
        setForeground(foregroundInfo(state))
    }

    private fun OfflineWordAudioDownloadState.record(
        result: WordAudioCacheResult,
    ): OfflineWordAudioDownloadState = copy(
        completed = completed + 1,
        downloaded = downloaded + if (result == WordAudioCacheResult.Downloaded) 1 else 0,
        alreadyAvailable = alreadyAvailable +
            if (result == WordAudioCacheResult.AlreadyCached) 1 else 0,
        failed = failed + if (result == WordAudioCacheResult.Failed) 1 else 0,
    )

    private fun OfflineWordAudioDownloadState.toWorkData() = workDataOf(
        KEY_COMPLETED to completed,
        KEY_TOTAL to total,
        KEY_DOWNLOADED to downloaded,
        KEY_ALREADY_AVAILABLE to alreadyAvailable,
        KEY_FAILED to failed,
    )

    private fun foregroundInfo(state: OfflineWordAudioDownloadState): ForegroundInfo {
        val localized = LanguageManager.localizedContext(applicationContext)
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                localized.getString(R.string.offline_audio_download_channel),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = localized.getString(R.string.offline_audio_download_channel_description)
            },
        )
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        val notification = NotificationCompat.Builder(localized, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localized.getString(R.string.offline_audio_download_title))
            .setContentText(
                localized.getString(
                    R.string.offline_audio_download_progress,
                    state.completed,
                    state.total,
                ),
            )
            .setProgress(state.total, state.completed, state.total == 0)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, localized.getString(R.string.cancel_download), cancelIntent)
            .build()
        return ForegroundInfo(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    companion object {
        const val KEY_REQUEST_ID = "request_id"
        const val KEY_COMPLETED = "completed"
        const val KEY_TOTAL = "total"
        const val KEY_DOWNLOADED = "downloaded"
        const val KEY_ALREADY_AVAILABLE = "already_available"
        const val KEY_FAILED = "failed"

        private const val CHANNEL_ID = "offline_audio_downloads"
        private const val NOTIFICATION_ID = 1210
    }
}
