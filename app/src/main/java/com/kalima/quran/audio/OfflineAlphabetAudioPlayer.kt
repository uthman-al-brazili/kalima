package com.kalima.quran.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import androidx.annotation.RawRes
import com.kalima.quran.R
import com.kalima.quran.data.AlphabetAudio

/** Plays the bundled, approved Arabic-letter recordings without a network or TTS engine. */
internal class OfflineAlphabetAudioPlayer(context: Context) {
    private val applicationContext = context.applicationContext
    private val audioManager = applicationContext.getSystemService(AudioManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val loadedSoundIds = mutableSetOf<Int>()
    private val soundIds = mutableMapOf<AlphabetAudio, Int>()
    private val audioBySoundId = mutableMapOf<Int, AlphabetAudio>()
    private var currentStreamId = 0
    private var remainingPlays = 0
    private var pendingReplay: Runnable? = null
    private var pendingAudio: AlphabetAudio? = null
    private var onFailure: (() -> Unit)? = null
    private var sequenceActive = false
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(audioAttributes)
        .setOnAudioFocusChangeListener { }
        .build()
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(audioAttributes)
        .build()

    init {
        soundPool.setOnLoadCompleteListener { _, soundId, status ->
            mainHandler.post { handleLoadCompleted(soundId, status) }
        }
        AlphabetAudio.entries.forEach { audio ->
            val soundId = soundPool.load(applicationContext, alphabetAudioResource(audio), 1)
            if (soundId != 0) {
                soundIds[audio] = soundId
                audioBySoundId[soundId] = audio
            }
        }
    }

    fun play(
        audio: AlphabetAudio,
        onFailure: () -> Unit,
    ): PronunciationResult {
        if (sequenceActive) {
            return PronunciationResult.Started
        }
        stop()
        val soundId = soundIds[audio] ?: return PronunciationResult.Failed
        remainingPlays = LETTER_NAME_PLAY_COUNT
        sequenceActive = true
        this.onFailure = onFailure

        return runCatching {
            audioManager.requestAudioFocus(audioFocusRequest)
            if (soundId in loadedSoundIds) {
                playNext(soundId)
            } else {
                pendingAudio = audio
            }
            PronunciationResult.Started
        }.getOrElse {
            stop()
            PronunciationResult.Failed
        }
    }

    fun stop() {
        pendingReplay?.let(mainHandler::removeCallbacks)
        pendingReplay = null
        if (currentStreamId != 0) soundPool.stop(currentStreamId)
        currentStreamId = 0
        remainingPlays = 0
        pendingAudio = null
        onFailure = null
        sequenceActive = false
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
    }

    fun shutdown() {
        stop()
        soundPool.setOnLoadCompleteListener(null)
        soundPool.release()
    }

    private fun handleLoadCompleted(soundId: Int, status: Int) {
        val audio = audioBySoundId[soundId] ?: return
        if (status != 0) {
            if (pendingAudio == audio) failSequence()
            return
        }
        loadedSoundIds += soundId
        if (sequenceActive && pendingAudio == audio) {
            pendingAudio = null
            playNext(soundId)
        }
    }

    private fun playNext(soundId: Int) {
        if (!sequenceActive) return
        currentStreamId = soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        if (currentStreamId == 0) {
            failSequence()
            return
        }
        remainingPlays -= 1
        val nextStep = Runnable {
            pendingReplay = null
            if (!sequenceActive) return@Runnable
            if (remainingPlays > 0) {
                playNext(soundId)
            } else {
                currentStreamId = 0
                sequenceActive = false
                onFailure = null
                audioManager.abandonAudioFocusRequest(audioFocusRequest)
            }
        }
        pendingReplay = nextStep
        mainHandler.postDelayed(nextStep, LETTER_NAME_REPLAY_INTERVAL_MS)
    }

    private fun failSequence() {
        val callback = onFailure
        stop()
        callback?.invoke()
    }

    private companion object {
        const val LETTER_NAME_PLAY_COUNT = 3
        const val LETTER_NAME_REPLAY_INTERVAL_MS = 1_100L
    }
}

@RawRes
internal fun alphabetAudioResource(audio: AlphabetAudio): Int = when (audio) {
    AlphabetAudio.ALIF -> R.raw.arabic_alphabet_01_alif
    AlphabetAudio.BAA -> R.raw.arabic_alphabet_02_baa
    AlphabetAudio.TAA -> R.raw.arabic_alphabet_03_taa
    AlphabetAudio.THAA -> R.raw.arabic_alphabet_04_thaa
    AlphabetAudio.JIIM -> R.raw.arabic_alphabet_05_jiim
    AlphabetAudio.HAA_PHARYNGEAL -> R.raw.arabic_alphabet_06_haa
    AlphabetAudio.KHAA -> R.raw.arabic_alphabet_07_khaa
    AlphabetAudio.DAAL -> R.raw.arabic_alphabet_08_daal
    AlphabetAudio.DHAAL -> R.raw.arabic_alphabet_09_dhaal
    AlphabetAudio.RAA -> R.raw.arabic_alphabet_10_raa
    AlphabetAudio.ZAAY -> R.raw.arabic_alphabet_11_zaay
    AlphabetAudio.SIIN -> R.raw.arabic_alphabet_12_siin
    AlphabetAudio.SHIIN -> R.raw.arabic_alphabet_13_shiin
    AlphabetAudio.SAAD -> R.raw.arabic_alphabet_14_saad
    AlphabetAudio.DAAD -> R.raw.arabic_alphabet_15_daad
    AlphabetAudio.TAA_EMPHATIC -> R.raw.arabic_alphabet_16_taa_emphatic
    AlphabetAudio.ZAA_EMPHATIC -> R.raw.arabic_alphabet_17_zaa_emphatic
    AlphabetAudio.AYN -> R.raw.arabic_alphabet_18_ayn
    AlphabetAudio.GHAYN -> R.raw.arabic_alphabet_19_ghayn
    AlphabetAudio.FAA -> R.raw.arabic_alphabet_20_faa
    AlphabetAudio.QAAF -> R.raw.arabic_alphabet_21_qaaf
    AlphabetAudio.KAAF -> R.raw.arabic_alphabet_22_kaaf
    AlphabetAudio.LAAM -> R.raw.arabic_alphabet_23_laam
    AlphabetAudio.MIIM -> R.raw.arabic_alphabet_24_miim
    AlphabetAudio.NUUN -> R.raw.arabic_alphabet_25_nuun
    AlphabetAudio.HAA -> R.raw.arabic_alphabet_26_haa
    AlphabetAudio.WAAW -> R.raw.arabic_alphabet_27_waaw
    AlphabetAudio.YAA -> R.raw.arabic_alphabet_28_yaa
}
