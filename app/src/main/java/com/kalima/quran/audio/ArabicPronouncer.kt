package com.kalima.quran.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import com.kalima.quran.data.QuranWordAudioLocation
import java.util.Locale

enum class PronunciationResult {
    Started,
    Initializing,
    Unavailable,
    Failed,
}

class ArabicPronouncer(context: Context) : TextToSpeech.OnInitListener {
    private data class PendingSpeech(
        val text: String,
        val speechRate: Float,
        val repeatCount: Int,
    )

    private enum class State {
        Idle,
        Initializing,
        Ready,
        Unavailable,
    }

    @Volatile
    private var state = State.Idle
    @Volatile
    private var initialized = false
    private val applicationContext = context.applicationContext
    private var engine: TextToSpeech? = null
    private var pendingSpeech: PendingSpeech? = null
    private val wordAudioPlayer = QuranComWordAudioPlayer()

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            state = State.Unavailable
            pendingSpeech = null
            return
        }

        initialized = true
        state = if (configureArabicVoice()) State.Ready else State.Unavailable
        if (state == State.Ready) {
            pendingSpeech?.let { request ->
                speakPrepared(request.text, request.speechRate, request.repeatCount)
            }
        }
        pendingSpeech = null
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
    ): PronunciationResult {
        wordAudioPlayer.stop()
        return when (state) {
        State.Idle -> {
            pendingSpeech = PendingSpeech(text, speechRate, repeatCount)
            startEngine()
            PronunciationResult.Initializing
        }
        State.Initializing -> {
            pendingSpeech = PendingSpeech(text, speechRate, repeatCount)
            PronunciationResult.Initializing
        }
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
    }

    fun speakWord(
        location: QuranWordAudioLocation?,
        playbackRate: Float = WORD_DEFAULT_RATE,
        repeatCount: Int = 1,
        onFailure: () -> Unit = {},
    ): PronunciationResult {
        val resolvedLocation = location ?: return PronunciationResult.Failed
        engine?.stop()
        pendingSpeech = null
        return wordAudioPlayer.play(
            location = resolvedLocation,
            playbackRate = playbackRate,
            repeatCount = repeatCount,
            onFailure = onFailure,
        )
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

    fun refreshEngine() {
        wordAudioPlayer.stop()
        if (state == State.Idle) return
        engine?.stop()
        engine?.shutdown()
        initialized = false
        startEngine()
    }

    fun shutdown() {
        wordAudioPlayer.stop()
        engine?.stop()
        engine?.shutdown()
        engine = null
        initialized = false
        pendingSpeech = null
        state = State.Idle
    }

    private fun startEngine() {
        state = State.Initializing
        engine = TextToSpeech(applicationContext, this)
    }

    companion object {
        const val DEFAULT_RATE = 0.78f
        const val SLOW_RATE = 0.58f
        const val WORD_DEFAULT_RATE = 1f
        const val WORD_SLOW_RATE = 0.7f
    }
}
