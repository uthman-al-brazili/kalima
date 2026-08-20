package com.kalima.quran.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingScreenTest {
    @Test
    fun `word study setup is shown only to learners who know the alphabet`() {
        assertFalse(shouldShowWordStudySetup(null))
        assertFalse(shouldShowWordStudySetup(false))
        assertTrue(shouldShowWordStudySetup(true))
    }
}
