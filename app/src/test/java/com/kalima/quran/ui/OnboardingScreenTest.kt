package com.kalima.quran.ui

import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.UnderstandPathId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingScreenTest {
    @Test
    fun `word study setup is shown only to learners who know the alphabet`() {
        assertFalse(shouldShowWordStudySetup(null))
        assertFalse(shouldShowWordStudySetup(false))
        assertTrue(shouldShowWordStudySetup(true))
    }

    @Test
    fun `starter plans activate guided paths only for finite goals`() {
        assertEquals(
            listOf(
                StudyScope.Prayer to UnderstandPathId.AlFatihahSevenDays,
                StudyScope.ShortSurahs to UnderstandPathId.LastTenSurahs,
            ),
            starterPlans.take(2).map { it.scope to it.understandPath },
        )
        assertEquals(StudyScope.Frequent, starterPlans[2].scope)
        assertNull(starterPlans[2].understandPath)
        assertEquals(StudyScope.All, starterPlans[3].scope)
        assertNull(starterPlans[3].understandPath)
    }
}
