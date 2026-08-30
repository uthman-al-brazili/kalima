package com.kalima.quran.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/**
 * Resolves optional foundation recordings by raw-resource name. This lets new letter-sound and
 * decoding exercises ship before every human recording is ready; adding a correctly named file
 * later requires no Kotlin change.
 */
internal class OfflineFoundationExerciseAudioPlayer(context: Context) {
    private val applicationContext = context.applicationContext
    private var player: MediaPlayer? = null
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    fun hasAudio(resourceName: String): Boolean = resourceId(resourceName) != 0

    fun play(resourceName: String, onFailure: () -> Unit): PronunciationResult {
        stop()
        val resourceId = resourceId(resourceName)
        if (resourceId == 0) return PronunciationResult.OfflineAudioMissing
        val newPlayer = runCatching {
            MediaPlayer.create(applicationContext, resourceId, audioAttributes, 0)
        }.getOrNull() ?: return PronunciationResult.Failed
        player = newPlayer
        return runCatching {
            newPlayer.setOnCompletionListener { completed -> release(completed) }
            newPlayer.setOnErrorListener { failed, _, _ ->
                release(failed)
                onFailure()
                true
            }
            newPlayer.start()
            PronunciationResult.Started
        }.getOrElse {
            release(newPlayer)
            PronunciationResult.Failed
        }
    }

    fun stop() {
        val current = player ?: return
        player = null
        current.setOnCompletionListener(null)
        current.setOnErrorListener(null)
        runCatching { current.stop() }
        current.release()
    }

    private fun release(completed: MediaPlayer) {
        if (player === completed) player = null
        completed.setOnCompletionListener(null)
        completed.setOnErrorListener(null)
        completed.release()
    }

    @Suppress("DEPRECATION")
    private fun resourceId(resourceName: String): Int = applicationContext.resources.getIdentifier(
        resourceName,
        "raw",
        applicationContext.packageName,
    )
}
