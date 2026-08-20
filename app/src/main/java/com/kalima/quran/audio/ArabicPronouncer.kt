package com.kalima.quran.audio

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.kalima.quran.data.QuranVerseAudioLocation
import com.kalima.quran.data.QuranWordAudioLocation

enum class PronunciationResult {
    Started,
    OfflineAudioMissing,
    DeviceVoiceUnavailable,
    Failed,
}

class ArabicPronouncer(context: Context) {
    private val applicationContext = context.applicationContext
    private val wordAudioPlayer = QuranComWordAudioPlayer(applicationContext)
    private val verseAudioPlayer = HussaryVerseAudioPlayer(applicationContext)
    private val foundationVoice = ArabicFoundationVoice(applicationContext)
    private val connectivityManager = applicationContext.getSystemService(ConnectivityManager::class.java)

    fun speakWord(
        location: QuranWordAudioLocation?,
        playbackRate: Float = WORD_DEFAULT_RATE,
        repeatCount: Int = 1,
        onPlaybackResult: (PronunciationResult) -> Unit = {},
    ): PronunciationResult {
        verseAudioPlayer.stop()
        foundationVoice.stop()
        val source = selectWordAudioSource(
            hasQuranComLocation = location != null,
            hasOfflineAudio = location?.let(wordAudioPlayer::hasOfflineAudio) == true,
            hasValidatedInternet = hasValidatedInternet(),
        )
        if (source == WordAudioSource.Unavailable) return PronunciationResult.OfflineAudioMissing

        return wordAudioPlayer.play(
            location = requireNotNull(location),
            playbackRate = playbackRate,
            repeatCount = repeatCount,
            allowStreaming = source == WordAudioSource.StreamingQuranComRecording,
            onFailure = { onPlaybackResult(PronunciationResult.Failed) },
        )
    }

    fun speakVerse(
        location: QuranVerseAudioLocation?,
        playbackRate: Float = VERSE_DEFAULT_RATE,
        repeatCount: Int = 1,
        onPlaybackResult: (PronunciationResult) -> Unit = {},
    ): PronunciationResult {
        wordAudioPlayer.stop()
        foundationVoice.stop()
        val hasOfflineAudio = location?.let(verseAudioPlayer::hasOfflineAudio) == true
        if (location == null || (!hasOfflineAudio && !hasValidatedInternet())) {
            return PronunciationResult.OfflineAudioMissing
        }

        return verseAudioPlayer.play(
            location = location,
            playbackRate = playbackRate,
            repeatCount = repeatCount,
            allowStreaming = !hasOfflineAudio,
            onFailure = { onPlaybackResult(PronunciationResult.Failed) },
        )
    }

    fun speakFoundation(
        text: String,
        playbackRate: Float = FOUNDATION_DEFAULT_RATE,
        onPlaybackResult: (PronunciationResult) -> Unit = {},
    ): PronunciationResult {
        wordAudioPlayer.stop()
        verseAudioPlayer.stop()
        return foundationVoice.speak(text, playbackRate, onPlaybackResult)
    }

    private fun hasValidatedInternet(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun shutdown() {
        wordAudioPlayer.stop()
        verseAudioPlayer.stop()
        foundationVoice.shutdown()
    }

    companion object {
        const val WORD_DEFAULT_RATE = 1f
        const val WORD_SLOW_RATE = 0.7f
        const val VERSE_DEFAULT_RATE = 1f
        const val FOUNDATION_DEFAULT_RATE = 0.8f
    }
}
