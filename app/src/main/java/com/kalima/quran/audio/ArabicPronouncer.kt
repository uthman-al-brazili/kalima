package com.kalima.quran.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.text.Normalizer
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
    private var engine: TextToSpeech? = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        val currentEngine = engine ?: return
        if (status != TextToSpeech.SUCCESS) {
            state = State.Unavailable
            return
        }

        val arabic = Locale.forLanguageTag("ar")
        val available = currentEngine.isLanguageAvailable(arabic)
        if (available < TextToSpeech.LANG_AVAILABLE ||
            currentEngine.setLanguage(arabic) < TextToSpeech.LANG_AVAILABLE
        ) {
            state = State.Unavailable
            return
        }

        currentEngine.setSpeechRate(0.78f)
        state = State.Ready
    }

    fun speak(text: String): PronunciationResult = when (state) {
        State.Initializing -> PronunciationResult.Initializing
        State.Unavailable -> PronunciationResult.Unavailable
        State.Ready -> {
            val prepared = ArabicSpeechText.prepare(text)
            val result = engine?.speak(
                prepared,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "kalima-word-${System.nanoTime()}",
            ) ?: TextToSpeech.ERROR
            if (result == TextToSpeech.ERROR) {
                PronunciationResult.Failed
            } else {
                PronunciationResult.Started
            }
        }
    }

    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        state = State.Unavailable
    }
}

object ArabicSpeechText {
    fun prepare(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFC)
        .filterNot(::isQuranicAnnotation)
        .replace("\u0640", "")
        .trim()

    private fun isQuranicAnnotation(character: Char): Boolean =
        character.code in 0x0610..0x061A ||
            character.code in 0x06D6..0x06ED ||
            character.code in 0x08D3..0x08E1
}
