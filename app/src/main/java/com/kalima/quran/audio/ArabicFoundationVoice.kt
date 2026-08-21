package com.kalima.quran.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import java.util.Locale

internal class ArabicFoundationVoice(context: Context) {
    private enum class State {
        Initializing,
        Ready,
        Unavailable,
    }

    private data class PendingSpeech(
        val text: String,
        val playbackRate: Float,
        val onPlaybackResult: (PronunciationResult) -> Unit,
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private var state = State.Initializing
    private var pendingSpeech: PendingSpeech? = null
    private var textToSpeech: TextToSpeech? = null

    init {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            mainHandler.post { finishInitialization(status) }
        }
    }

    fun speak(
        text: String,
        playbackRate: Float,
        onPlaybackResult: (PronunciationResult) -> Unit,
    ): PronunciationResult {
        if (text.isBlank()) return PronunciationResult.Failed
        val request = PendingSpeech(text, playbackRate, onPlaybackResult)
        return when (state) {
            State.Initializing -> {
                pendingSpeech = request
                PronunciationResult.Started
            }
            State.Ready -> play(request)
            State.Unavailable -> PronunciationResult.DeviceVoiceUnavailable
        }
    }

    fun stop() {
        pendingSpeech = null
        textToSpeech?.stop()
    }

    fun shutdown() {
        pendingSpeech = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        state = State.Unavailable
    }

    private fun finishInitialization(status: Int) {
        val engine = textToSpeech
        val languageAvailable = if (status == TextToSpeech.SUCCESS && engine != null) {
            ARABIC_LOCALES.any { locale ->
                engine.setLanguage(locale) >= TextToSpeech.LANG_AVAILABLE
            }
        } else {
            false
        }
        state = if (languageAvailable) State.Ready else State.Unavailable
        val pending = pendingSpeech.also { pendingSpeech = null } ?: return
        val result = if (languageAvailable) {
            play(pending)
        } else {
            PronunciationResult.DeviceVoiceUnavailable
        }
        if (result != PronunciationResult.Started) pending.onPlaybackResult(result)
    }

    private fun play(request: PendingSpeech): PronunciationResult {
        val engine = textToSpeech ?: return PronunciationResult.DeviceVoiceUnavailable
        engine.stop()
        engine.setSpeechRate(request.playbackRate)
        val result = engine.speak(
            request.text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            FOUNDATION_UTTERANCE_ID,
        )
        return if (result == TextToSpeech.SUCCESS) {
            PronunciationResult.Started
        } else {
            PronunciationResult.Failed
        }
    }

    private companion object {
        const val FOUNDATION_UTTERANCE_ID = "kalima-foundation-pronunciation"
        val ARABIC_LOCALES: List<Locale> = listOf(
            Locale.forLanguageTag("ar-SA"),
            Locale.forLanguageTag("ar"),
        )
    }
}
