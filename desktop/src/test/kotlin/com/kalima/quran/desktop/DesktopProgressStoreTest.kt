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
}
