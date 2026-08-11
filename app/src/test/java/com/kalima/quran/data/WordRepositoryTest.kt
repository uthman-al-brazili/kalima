package com.kalima.quran.data

import com.kalima.quran.ui.buildStudySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WordRepositoryTest {
    @Test
    fun wordIdsAreUniqueAndRequiredFieldsArePresent() {
        val words = WordRepository.words
        assertEquals(words.size, words.map { it.id }.toSet().size)
        assertTrue(words.all { it.arabic.isNotBlank() })
        assertTrue(words.all { it.root.isNotBlank() })
        assertTrue(words.all { it.reference.matches(Regex(".+ \\d+:\\d+")) })
        val formsNotFoundInVerse = words.filterNot {
            normalizeMarks(it.verseArabic).contains(normalizeMarks(it.arabic)) || it.id in INFLECTED_FORMS
        }.map { it.id }
        assertTrue(formsNotFoundInVerse.toString(), formsNotFoundInVerse.isEmpty())
    }

    @Test
    fun dailyWordIsDeterministic() {
        val date = LocalDate.of(2026, 8, 10)
        assertEquals(WordRepository.wordFor(date), WordRepository.wordFor(date))
    }

    @Test
    fun lockScreenSequenceAdvancesAndWraps() {
        assertEquals(WordRepository.words[0], WordRepository.wordAtSequence(0))
        assertEquals(WordRepository.words[1], WordRepository.wordAtSequence(1))
        assertEquals(WordRepository.words[0], WordRepository.wordAtSequence(WordRepository.words.size))
    }

    @Test
    fun searchFindsPortugueseArabicAndRoots() {
        assertTrue(WordRepository.search("oração").any { it.id == "salah" })
        assertTrue(WordRepository.search("ٱلْغَيْبِ").any { it.id == "ghayb" })
        assertTrue(WordRepository.search("ك ت ب").any { it.id == "kitab" })
    }

    @Test
    fun generatedCorpusContainsFrequentWordsAndEveryLastSurah() {
        assertEquals((1..114).toList(), WordRepository.selectableSurahs.map(QuranSurah::number))
        assertEquals(100, WordRepository.frequentWords.size)
        assertEquals(
            (101..114).toSet(),
            WordRepository.words.mapNotNull(QuranWord::surahNumber).toSet(),
        )
        (101..114).forEach { surah ->
            assertTrue(
                "A sura $surah deve conter palavras",
                WordRepository.words.any { it.surahNumber == surah },
            )
        }
    }

    @Test
    fun surahSelectionSupportsOneOrSeveralSurahs() {
        val only114 = WordRepository.wordsFor(StudyScope.Surahs, setOf(114))
        assertTrue(only114.isNotEmpty())
        assertTrue(only114.all { it.surahNumber == 114 })

        val selected = WordRepository.wordsFor(StudyScope.Surahs, setOf(103, 110))
        assertTrue(selected.isNotEmpty())
        assertEquals(setOf(103, 110), selected.mapNotNull(QuranWord::surahNumber).toSet())
        assertTrue(selected.all { it.surahNumber in setOf(103, 110) })
    }

    @Test
    fun requestedLockScreenWordStartsTheStudySession() {
        val words = WordRepository.words.take(4)
        val requested = words[2]

        val session = buildStudySession(words, defaultWord = words[0], requestedWord = requested)

        assertEquals(requested.id, session.first().id)
        assertEquals(words.map(QuranWord::id).toSet(), session.map(QuranWord::id).toSet())
    }

    @Test
    fun requestedWordOutsideTheCurrentScopeIsStillShownFirst() {
        val scopedWords = WordRepository.words.take(3)
        val requested = WordRepository.words[4]

        val session = buildStudySession(
            words = scopedWords,
            defaultWord = scopedWords[0],
            requestedWord = requested,
        )

        assertEquals(requested.id, session.first().id)
        assertEquals(scopedWords.size + 1, session.size)
    }

    private companion object {
        fun normalizeMarks(value: String): String = value
            .replace(Regex("\\p{M}"), "")
            .replace(Regex("[أإآٱ]"), "ا")
            .replace("ى", "ي")

        val INFLECTED_FORMS = setOf(
            "allah",
            "rabb",
            "rahman",
            "din",
            "kitab",
            "muttaqin",
            "ghayb",
            "akhirah",
            "haqq",
            "sabirin",
        )
    }
}
