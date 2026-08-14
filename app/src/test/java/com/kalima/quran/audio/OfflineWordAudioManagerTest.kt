package com.kalima.quran.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineWordAudioManagerTest {
    @Test
    fun `estimates selected real audio pack size in decimal megabytes`() {
        assertEquals(0L, OfflineWordAudioManager.estimatedMegabytes(0))
        assertEquals(7L, OfflineWordAudioManager.estimatedMegabytes(100))
        assertEquals(34L, OfflineWordAudioManager.estimatedMegabytes(500))
    }

    @Test
    fun `includes unique ayah recitations in the download estimate`() {
        assertEquals(2L, OfflineWordAudioManager.estimatedMegabytes(wordCount = 10, verseCount = 2))
    }
}
