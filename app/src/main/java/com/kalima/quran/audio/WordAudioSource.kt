package com.kalima.quran.audio

internal enum class WordAudioSource {
    CachedQuranComRecording,
    StreamingQuranComRecording,
    Unavailable,
}

internal fun selectWordAudioSource(
    hasQuranComLocation: Boolean,
    hasOfflineAudio: Boolean,
    hasValidatedInternet: Boolean,
): WordAudioSource = when {
    !hasQuranComLocation -> WordAudioSource.Unavailable
    hasOfflineAudio -> WordAudioSource.CachedQuranComRecording
    hasValidatedInternet -> WordAudioSource.StreamingQuranComRecording
    else -> WordAudioSource.Unavailable
}
