package com.kalima.quran.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.annotation.RawRes
import com.kalima.quran.R
import com.kalima.quran.data.AlphabetAudio

/** Plays the bundled, approved Arabic-letter recordings without a network or TTS engine. */
internal class OfflineAlphabetAudioPlayer(context: Context) {
    private val applicationContext = context.applicationContext
    private var player: MediaPlayer? = null
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    fun play(
        audio: AlphabetAudio,
        onFailure: () -> Unit,
    ): PronunciationResult {
        stop()
        val newPlayer = runCatching {
            MediaPlayer.create(
                applicationContext,
                alphabetAudioResource(audio),
                audioAttributes,
                0,
            )
        }.getOrNull() ?: return PronunciationResult.Failed
        player = newPlayer

        return runCatching {
            newPlayer.setOnCompletionListener(::releaseCompleted)
            newPlayer.setOnErrorListener { failedPlayer, _, _ ->
                fail(failedPlayer, onFailure)
                true
            }
            newPlayer.start()
            PronunciationResult.Started
        }.getOrElse {
            if (player === newPlayer) stop() else newPlayer.release()
            PronunciationResult.Failed
        }
    }

    fun stop() {
        val current = player
        player = null
        current?.setOnCompletionListener(null)
        current?.setOnErrorListener(null)
        runCatching { current?.stop() }
        current?.release()
    }

    private fun releaseCompleted(completedPlayer: MediaPlayer) {
        if (player === completedPlayer) {
            player = null
            completedPlayer.setOnCompletionListener(null)
            completedPlayer.setOnErrorListener(null)
            completedPlayer.release()
        } else {
            completedPlayer.release()
        }
    }

    private fun fail(failedPlayer: MediaPlayer, onFailure: () -> Unit) {
        if (player === failedPlayer) {
            stop()
            onFailure()
        } else {
            failedPlayer.release()
        }
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
