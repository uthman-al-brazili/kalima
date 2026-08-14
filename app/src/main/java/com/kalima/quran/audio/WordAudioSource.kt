package com.kalima.quran.audio

internal enum class WordAudioSource {
    QuranComRecording,
    AndroidArabicVoice,
}

internal fun selectWordAudioSource(
    hasQuranComLocation: Boolean,
    hasValidatedInternet: Boolean,
): WordAudioSource = if (hasQuranComLocation && hasValidatedInternet) {
    WordAudioSource.QuranComRecording
} else {
    WordAudioSource.AndroidArabicVoice
}
