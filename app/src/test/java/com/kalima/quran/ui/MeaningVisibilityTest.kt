package com.kalima.quran.ui

import com.kalima.quran.data.WordStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningVisibilityTest {
    @Test
    fun `new words reveal their meaning initially`() {
        assertTrue(shouldRevealMeaningInitially(WordStatus.New))
    }

    @Test
    fun `review words hide their meaning initially`() {
        assertFalse(shouldRevealMeaningInitially(WordStatus.Reviewing))
        assertFalse(shouldRevealMeaningInitially(WordStatus.Learned))
        assertFalse(shouldRevealMeaningInitially(WordStatus.AlreadyKnown))
    }

    @Test
    fun `first presentation keeps new status after introduction is recorded`() {
        assertEquals(
            WordStatus.New,
            studyPresentationStatus(
                status = WordStatus.Reviewing,
                isNewPresentation = true,
            ),
        )
    }

    @Test
    fun `later presentations show current progress status`() {
        WordStatus.entries.forEach { status ->
            assertEquals(
                status,
                studyPresentationStatus(status, isNewPresentation = false),
            )
        }
    }
}
