package com.kalima.quran.data

import org.junit.Assert.assertEquals
import org.junit.Test

class QuranVerseAudioLocationTest {
    @Test
    fun `builds the Al Hussary verse recording URL`() {
        val location = QuranVerseAudioLocation(surah = 1, ayah = 1)

        assertEquals("001001.mp3", location.fileName)
        assertEquals(
            "https://everyayah.com/data/Husary_128kbps/001001.mp3",
            location.hussaryUrl,
        )
    }

    @Test
    fun `builds the last Quran ayah recording URL`() {
        val location = QuranVerseAudioLocation(surah = 114, ayah = 6)

        assertEquals("114006.mp3", location.fileName)
        assertEquals(
            "https://everyayah.com/data/Husary_128kbps/114006.mp3",
            location.hussaryUrl,
        )
    }

    @Test
    fun `derives an ayah location from a word location`() {
        assertEquals(
            QuranVerseAudioLocation(surah = 2, ayah = 255),
            QuranVerseAudioLocation.fromWord(QuranWordAudioLocation(2, 255, 12)),
        )
    }
}
