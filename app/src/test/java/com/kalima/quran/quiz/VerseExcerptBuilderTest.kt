package com.kalima.quran.quiz

import com.kalima.quran.data.WordRepository
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class VerseExcerptBuilderTest {
    @Test
    fun highlightsWordDespiteDifferentAlefAndRecitationMarks() {
        val word = WordRepository.words.first { it.id == "s108-v001-w001" }

        val excerpt = VerseExcerptBuilder.build(word)

        assertTrue(excerpt.hasHighlight)
        assertTrue(excerpt.highlightEnd <= excerpt.text.length)
    }

    @Test
    fun longVerseIsShortenedAroundHighlightedWord() {
        val original = WordRepository.words.first { it.id == "s114-v001-w002" }
        val longVerse = "مقدمة طويلة ".repeat(30) + original.arabic + " نهاية طويلة ".repeat(30)
        val word = original.copy(verseArabic = longVerse)

        val excerpt = VerseExcerptBuilder.build(word, maxChars = 120)

        assertTrue(excerpt.text.length < longVerse.length)
        assertTrue(excerpt.hasHighlight)
        assertTrue(excerpt.text.startsWith("…"))
        assertTrue(excerpt.text.endsWith("…"))
    }

    @Test
    fun clozeReplacesTheTargetWithABlank() {
        val word = WordRepository.words.first { it.id == "s108-v001-w001" }

        val cloze = VerseExcerptBuilder.buildCloze(word)

        assertTrue(cloze.contains("____"))
        assertFalse(cloze.contains(word.arabic))
    }
}
