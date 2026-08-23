package com.kalima.quran.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class PronunciationResultTest {
    @Test
    fun `streaming failure without internet is reported as offline`() {
        assertEquals(
            PronunciationResult.OfflineAudioMissing,
            classifyPlaybackFailure(isStreaming = true, hasValidatedInternet = false),
        )
    }

    @Test
    fun `cached and connected failures remain generic playback failures`() {
        assertEquals(
            PronunciationResult.Failed,
            classifyPlaybackFailure(isStreaming = false, hasValidatedInternet = false),
        )
        assertEquals(
            PronunciationResult.Failed,
            classifyPlaybackFailure(isStreaming = true, hasValidatedInternet = true),
        )
    }
}
