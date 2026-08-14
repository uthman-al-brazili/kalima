package com.kalima.quran.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class WordAudioSourceTest {
    @Test
    fun `uses cached recording offline when it is available`() {
        assertEquals(
            WordAudioSource.CachedQuranComRecording,
            selectWordAudioSource(
                hasQuranComLocation = true,
                hasOfflineAudio = true,
                hasValidatedInternet = false,
            ),
        )
    }

    @Test
    fun `streams real recording when online and not cached`() {
        assertEquals(
            WordAudioSource.StreamingQuranComRecording,
            selectWordAudioSource(
                hasQuranComLocation = true,
                hasOfflineAudio = false,
                hasValidatedInternet = true,
            ),
        )
    }

    @Test
    fun `does not substitute synthesized speech while offline`() {
        assertEquals(
            WordAudioSource.Unavailable,
            selectWordAudioSource(
                hasQuranComLocation = true,
                hasOfflineAudio = false,
                hasValidatedInternet = false,
            ),
        )
    }

    @Test
    fun `reports unavailable when a recording location is missing`() {
        assertEquals(
            WordAudioSource.Unavailable,
            selectWordAudioSource(
                hasQuranComLocation = false,
                hasOfflineAudio = false,
                hasValidatedInternet = true,
            ),
        )
    }
}
