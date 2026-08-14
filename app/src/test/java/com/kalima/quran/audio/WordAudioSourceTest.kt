package com.kalima.quran.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class WordAudioSourceTest {
    @Test
    fun `uses exact recording when a location and internet are available`() {
        assertEquals(
            WordAudioSource.QuranComRecording,
            selectWordAudioSource(
                hasQuranComLocation = true,
                hasValidatedInternet = true,
            ),
        )
    }

    @Test
    fun `uses Android Arabic voice while offline`() {
        assertEquals(
            WordAudioSource.AndroidArabicVoice,
            selectWordAudioSource(
                hasQuranComLocation = true,
                hasValidatedInternet = false,
            ),
        )
    }

    @Test
    fun `uses Android Arabic voice when a recording location is missing`() {
        assertEquals(
            WordAudioSource.AndroidArabicVoice,
            selectWordAudioSource(
                hasQuranComLocation = false,
                hasValidatedInternet = true,
            ),
        )
    }
}
