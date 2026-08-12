package com.kalima.quran.ui

import com.kalima.quran.data.WordStatus
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
    }
}
