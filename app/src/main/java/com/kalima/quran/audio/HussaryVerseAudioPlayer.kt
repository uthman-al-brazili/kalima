package com.kalima.quran.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Handler
import android.os.Looper
import com.kalima.quran.data.QuranVerseAudioLocation

internal class HussaryVerseAudioPlayer(context: Context) {
    private var player: MediaPlayer? = null
    private var playsRemaining = 0
    private var onPlaybackProgress: ((VerseAudioPlaybackProgress) -> Unit)? = null
    private val progressHandler = Handler(Looper.getMainLooper())
    private val offlineStore = OfflineVerseAudioStore.get(context)

    fun hasOfflineAudio(location: QuranVerseAudioLocation): Boolean =
        offlineStore.cachedFile(location) != null

    fun play(
        location: QuranVerseAudioLocation,
        playbackRate: Float,
        repeatCount: Int,
        allowStreaming: Boolean,
        onFailure: () -> Unit,
        onProgress: (VerseAudioPlaybackProgress) -> Unit,
    ): PronunciationResult {
        stop()
        val cachedFile = offlineStore.cachedFile(location)
        if (cachedFile == null && !allowStreaming) return PronunciationResult.OfflineAudioMissing
        val newPlayer = MediaPlayer()
        player = newPlayer
        onPlaybackProgress = onProgress
        playsRemaining = repeatCount.coerceIn(1, 5)

        return try {
            newPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            newPlayer.setDataSource(cachedFile?.absolutePath ?: location.hussaryUrl)
            if (cachedFile == null) offlineStore.cacheInBackground(location)
            newPlayer.setOnPreparedListener { preparedPlayer ->
                if (player !== preparedPlayer) {
                    preparedPlayer.release()
                    return@setOnPreparedListener
                }
                runCatching {
                    preparedPlayer.playbackParams = PlaybackParams().setSpeed(
                        playbackRate.coerceIn(MIN_PLAYBACK_RATE, MAX_PLAYBACK_RATE),
                    )
                    playsRemaining -= 1
                    preparedPlayer.start()
                    reportProgress(preparedPlayer)
                }.onFailure {
                    fail(preparedPlayer, onFailure)
                }
            }
            newPlayer.setOnCompletionListener { completedPlayer ->
                if (player !== completedPlayer) return@setOnCompletionListener
                if (playsRemaining > 0) {
                    runCatching {
                        progressHandler.removeCallbacksAndMessages(null)
                        playsRemaining -= 1
                        completedPlayer.seekTo(0)
                        completedPlayer.start()
                        reportProgress(completedPlayer)
                    }.onFailure {
                        fail(completedPlayer, onFailure)
                    }
                } else {
                    stop()
                }
            }
            newPlayer.setOnErrorListener { failedPlayer, _, _ ->
                fail(failedPlayer, onFailure)
                true
            }
            newPlayer.prepareAsync()
            PronunciationResult.Started
        } catch (_: Exception) {
            if (player === newPlayer) stop() else newPlayer.release()
            PronunciationResult.Failed
        }
    }

    fun stop() {
        progressHandler.removeCallbacksAndMessages(null)
        val current = player
        player = null
        playsRemaining = 0
        onPlaybackProgress?.invoke(VerseAudioPlaybackProgress())
        onPlaybackProgress = null
        current?.setOnPreparedListener(null)
        current?.setOnCompletionListener(null)
        current?.setOnErrorListener(null)
        runCatching { current?.stop() }
        current?.release()
    }

    private fun fail(failedPlayer: MediaPlayer, onFailure: () -> Unit) {
        if (player === failedPlayer) {
            stop()
            onFailure()
        } else {
            failedPlayer.release()
        }
    }

    private fun reportProgress(current: MediaPlayer) {
        if (player !== current) return
        val snapshot = runCatching {
            VerseAudioPlaybackProgress(
                isPlaying = current.isPlaying,
                positionMs = current.currentPosition.coerceAtLeast(0),
                durationMs = current.duration.coerceAtLeast(0),
            )
        }.getOrElse { VerseAudioPlaybackProgress() }
        onPlaybackProgress?.invoke(snapshot)
        if (snapshot.isPlaying) {
            progressHandler.postDelayed({ reportProgress(current) }, PROGRESS_INTERVAL_MS)
        }
    }

    private companion object {
        const val MIN_PLAYBACK_RATE = 0.5f
        const val MAX_PLAYBACK_RATE = 1.2f
        const val PROGRESS_INTERVAL_MS = 75L
    }
}
