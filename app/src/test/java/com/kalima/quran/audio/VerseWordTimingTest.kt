package com.kalima.quran.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VerseWordTimingTest {
    @Test
    fun `maps playback progress across every ayah word`() {
        assertEquals(0, verseWordIndexAt(positionMs = 0, durationMs = 1_000, wordCount = 4))
        assertEquals(1, verseWordIndexAt(positionMs = 250, durationMs = 1_000, wordCount = 4))
        assertEquals(2, verseWordIndexAt(positionMs = 500, durationMs = 1_000, wordCount = 4))
        assertEquals(3, verseWordIndexAt(positionMs = 999, durationMs = 1_000, wordCount = 4))
    }

    @Test
    fun `does not highlight without usable active playback timing`() {
        assertNull(verseWordIndexAt(positionMs = 0, durationMs = 0, wordCount = 4))
        assertNull(verseWordIndexAt(positionMs = 1_000, durationMs = 1_000, wordCount = 4))
        assertNull(verseWordIndexAt(positionMs = -1, durationMs = 1_000, wordCount = 4))
        assertNull(verseWordIndexAt(positionMs = 0, durationMs = 1_000, wordCount = 0))
    }
}
