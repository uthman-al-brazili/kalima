package com.kalima.quran.ui

import com.kalima.quran.data.SessionLevel
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.UnderstandPathId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OnboardingScreenTest {
    @Test
    fun `learning navigation exposes only dictionary and quiz`() {
        assertEquals(setOf(LearnSection.Dictionary, LearnSection.Quiz), LearnSection.entries.toSet())
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

    @Test
    fun `onboarding omits Quick because a new user has no reviews`() {
        assertEquals(listOf(SessionLevel.Steady, SessionLevel.Deep), onboardingSessionLevels)
    }
}
