package com.kalima.quran.audio

import com.kalima.quran.data.AlphabetAudio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineAlphabetAudioPlayerTest {
    @Test
    fun `all letter recordings map to distinct bundled resources`() {
        val resources = AlphabetAudio.entries.map(::alphabetAudioResource)

        assertEquals(28, resources.size)
        assertEquals(28, resources.toSet().size)
        assertTrue(resources.all { it != 0 })
    }

    @Test
    fun `approved Arabic names select every offline recording`() {
        AlphabetAudio.entries.forEach { audio ->
            assertEquals(audio, AlphabetAudio.fromSpokenArabic(audio.spokenArabic))
        }
        assertEquals(null, AlphabetAudio.fromSpokenArabic("بَ"))
    }
}
