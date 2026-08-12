package com.kalima.quran.desktop

import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.WordRepository
import com.kalima.quran.localization.AppLanguage
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopProgressStoreTest {
    @Test
    fun loadsCompleteOfflineCorpus() {
        val store = DesktopProgressStore(Files.createTempDirectory("kalima-corpus-test"))

        assertEquals(42_117, WordRepository.words.size)
        assertEquals(114, WordRepository.selectableSurahs.size)
        assertFalse(store.progress.onboardingComplete)
    }

    @Test
    fun searchesArabicWithoutVowelMarksOrRootSpaces() {
        assertTrue(WordRepository.search("الرحمن").any { it.id == "rahman" })
        assertTrue(WordRepository.search("الغيب").any { it.id == "ghayb" })
        assertTrue(WordRepository.search("كتب").any { it.id == "kitab" })
    }

    @Test
    fun persistsStudyProgressAndSettings() {
        val directory = Files.createTempDirectory("kalima-progress-test")
        val first = DesktopProgressStore(directory)
        first.completeOnboarding(StudyScope.Frequent50, 10)
        first.changeLanguage(AppLanguage.English)
        first.answer(WordRepository.words.first().id, true)

        val restored = DesktopProgressStore(directory)

        assertTrue(restored.progress.onboardingComplete)
        assertEquals(StudyScope.Frequent50, restored.progress.studyScope)
        assertEquals(10, restored.progress.dailyGoal)
        assertEquals(AppLanguage.English, restored.language)
        assertEquals(1, restored.progress.reviewSchedules.size)
        assertEquals(1, restored.progress.todayCompleted)
    }

    @Test
    fun persistsWelcomeBackCardSettingsWithAndroidCompatibleFields() {
        val directory = Files.createTempDirectory("kalima-return-settings-test")
        val first = DesktopProgressStore(directory)
        first.setReturnCardsEnabled(true)
        first.changeReturnCardIdleMinutes(20)
        first.setReturnCardQuizEnabled(true)
        first.setReturnCardQuizInterval(5)
        first.setReturnCardDailyLimit(10)
        first.setReturnCardQuietHoursEnabled(false)

        val restored = DesktopProgressStore(directory)

        assertTrue(restored.progress.lockScreenEnabled)
        assertEquals(20, restored.returnCardIdleMinutes)
        assertTrue(restored.progress.lockScreenQuizEnabled)
        assertEquals(5, restored.progress.lockScreenQuizInterval)
        assertEquals(10, restored.progress.lockScreenDailyLimit)
        assertFalse(restored.progress.quietHoursEnabled)
    }

    @Test
    fun previewBuildsReturnCardWithoutUsingDailyAllowance() {
        val store = DesktopProgressStore(Files.createTempDirectory("kalima-return-preview-test"))

        val content = store.nextReturnCardContent(preview = true)

        assertTrue(content != null)
        assertEquals(0, store.progress.lockScreenCardsToday)
    }
}
