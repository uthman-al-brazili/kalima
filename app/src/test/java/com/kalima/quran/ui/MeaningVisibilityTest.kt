package com.kalima.quran.ui

import com.kalima.quran.data.WordStatus
import org.junit.Assert.assertFalse
import org.junit.Test

class MeaningVisibilityTest {
    @Test
    fun `new words ask for recall before revealing their meaning`() {
        assertFalse(shouldRevealMeaningInitially(WordStatus.New))
    }

    @Test
    fun `review words hide their meaning initially`() {
        assertFalse(shouldRevealMeaningInitially(WordStatus.Reviewing))
        assertFalse(shouldRevealMeaningInitially(WordStatus.Learned))
        assertFalse(shouldRevealMeaningInitially(WordStatus.AlreadyKnown))
    }
}
