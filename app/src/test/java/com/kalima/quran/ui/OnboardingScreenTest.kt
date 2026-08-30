package com.kalima.quran.ui

import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.UnderstandPathId
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingScreenTest {
    @Test
    fun `onboarding starts with the vocabulary plan without foundation questions`() {
        val onboarding = File("src/main/java/com/kalima/quran/ui/OnboardingScreen.kt").readText()

        assertFalse(onboarding.contains("knowsArabicAlphabet"))
        assertFalse(onboarding.contains("knowsArabicNumbers"))
        assertTrue(onboarding.contains("R.string.onboarding_path_title"))
    }

    @Test
    fun `alphabet and number courses are not exposed or used as learning gates`() {
        val learn = File("src/main/java/com/kalima/quran/ui/LearnScreen.kt").readText()
        val study = File("src/main/java/com/kalima/quran/ui/StudyScreen.kt").readText()
        val quiz = File("src/main/java/com/kalima/quran/ui/QuizScreen.kt").readText()

        assertEquals(setOf(LearnSection.Dictionary, LearnSection.Quiz), LearnSection.entries.toSet())
        assertFalse(learn.contains("LearnSection.Alphabet"))
        assertFalse(learn.contains("LearnSection.Numbers"))
        assertFalse(study.contains("if (progress.needsAlphabetFoundation)"))
        assertFalse(quiz.contains("needsAlphabetFoundation"))
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
    fun `onboarding and settings use session presets instead of daily goal sliders`() {
        val onboarding = File("src/main/java/com/kalima/quran/ui/OnboardingScreen.kt").readText()
        val settings = File("src/main/java/com/kalima/quran/ui/SettingsScreen.kt").readText()

        assertTrue(onboarding.contains("SessionLevelSelector("))
        assertTrue(onboarding.contains("SessionLevel.Steady"))
        assertFalse(onboarding.contains("onboarding_daily_goal"))
        assertTrue(settings.contains("R.string.default_session_level"))
        assertTrue(settings.contains("SessionLevelSelector("))
        assertFalse(settings.contains("onDailyGoalChange"))
    }
}
