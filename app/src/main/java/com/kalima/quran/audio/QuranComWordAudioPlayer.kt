package com.kalima.quran.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import com.kalima.quran.data.QuranWordAudioLocation

internal class QuranComWordAudioPlayer(context: Context) {
    private var player: MediaPlayer? = null
    private var playsRemaining = 0
    private val offlineStore = OfflineWordAudioStore.get(context)

    fun hasOfflineAudio(location: QuranWordAudioLocation): Boolean =
        offlineStore.cachedFile(location) != null

    fun play(
        location: QuranWordAudioLocation,
        playbackRate: Float,
        repeatCount: Int,
        allowStreaming: Boolean,
        onFailure: () -> Unit,
    ): PronunciationResult {
        stop()
        val cachedFile = offlineStore.cachedFile(location)
        if (cachedFile == null && !allowStreaming) return PronunciationResult.OfflineAudioMissing
        val newPlayer = MediaPlayer()
        player = newPlayer
        playsRemaining = repeatCount.coerceIn(1, 5)

        return try {
            newPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            newPlayer.setDataSource(cachedFile?.absolutePath ?: location.quranComUrl)
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
                }.onFailure {
                    fail(preparedPlayer, onFailure)
                }
            }
            newPlayer.setOnCompletionListener { completedPlayer ->
                if (player !== completedPlayer) return@setOnCompletionListener
                if (playsRemaining > 0) {
                    runCatching {
                        playsRemaining -= 1
                        completedPlayer.seekTo(0)
                        completedPlayer.start()
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
        val current = player
        player = null
        playsRemaining = 0
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

    private companion object {
        const val MIN_PLAYBACK_RATE = 0.5f
        const val MAX_PLAYBACK_RATE = 1.2f
    }
}
