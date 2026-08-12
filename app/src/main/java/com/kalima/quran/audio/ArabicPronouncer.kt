package com.kalima.quran.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

enum class PronunciationResult {
    Started,
    Initializing,
    Unavailable,
    Failed,
}

class ArabicPronouncer(context: Context) : TextToSpeech.OnInitListener {
    private enum class State {
        Initializing,
        Ready,
        Unavailable,
    }

    @Volatile
    private var state = State.Initializing
    @Volatile
    private var initialized = false
    private var engine: TextToSpeech? = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            state = State.Unavailable
            return
        }

        initialized = true
        state = if (configureArabicVoice()) State.Ready else State.Unavailable
    }

    private fun configureArabicVoice(): Boolean {
        val currentEngine = engine ?: return false
        val arabic = Locale.forLanguageTag("ar")
        val available = currentEngine.isLanguageAvailable(arabic)
        if (available < TextToSpeech.LANG_AVAILABLE ||
            currentEngine.setLanguage(arabic) < TextToSpeech.LANG_AVAILABLE
        ) {
            return false
        }

        currentEngine.setSpeechRate(0.78f)
        return true
    }

    fun speak(
        text: String,
        speechRate: Float = DEFAULT_RATE,
        repeatCount: Int = 1,
    ): PronunciationResult = when (state) {
        State.Initializing -> PronunciationResult.Initializing
        State.Unavailable -> {
            if (initialized && configureArabicVoice()) {
                state = State.Ready
                speakPrepared(text, speechRate, repeatCount)
            } else {
                PronunciationResult.Unavailable
            }
        }
        State.Ready -> speakPrepared(text, speechRate, repeatCount)
    }

    private fun speakPrepared(text: String, speechRate: Float, repeatCount: Int): PronunciationResult {
        engine?.setSpeechRate(speechRate.coerceIn(0.4f, 1.2f))
        val prepared = ArabicSpeechText.prepare(text)
        var result = TextToSpeech.SUCCESS
        repeatCount.coerceIn(1, 5).let { repeats ->
            repeat(repeats) { index ->
                result = engine?.speak(
                    prepared,
                    if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                    null,
                    "kalima-word-${System.nanoTime()}-$index",
                ) ?: TextToSpeech.ERROR
                if (result == TextToSpeech.ERROR) return@let
            }
        }
        return if (result == TextToSpeech.ERROR) {
            PronunciationResult.Failed
        } else {
            PronunciationResult.Started
        }
    }

    fun preferredEnginePackage(): String? = engine?.defaultEngine

    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        initialized = false
        state = State.Unavailable
    }

    companion object {
        const val DEFAULT_RATE = 0.78f
        const val SLOW_RATE = 0.58f
    }
}
