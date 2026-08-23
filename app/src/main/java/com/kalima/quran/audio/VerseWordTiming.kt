package com.kalima.quran.audio

/** Snapshot used by the verse UI to follow recitation without keeping a MediaPlayer in Compose. */
data class VerseAudioPlaybackProgress(
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
)

/**
 * Maps ayah playback to words. Hussary files bundled by the app do not contain word timestamps,
 * so each spoken word receives an equal, deterministic share of the recorded ayah.
 */
fun verseWordIndexAt(
    positionMs: Int,
    durationMs: Int,
    wordCount: Int,
): Int? {
    if (durationMs <= 0 || wordCount <= 0 || positionMs !in 0 until durationMs) return null
    return ((positionMs.toLong() * wordCount) / durationMs)
        .toInt()
        .coerceIn(0, wordCount - 1)
}
