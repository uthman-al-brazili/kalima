package com.kalima.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class QuranWordLearnerInsightTest {
    @Test
    fun `internal lemma note is never exposed to learners`() {
        val english = word("This form appears 3 times in the Quran. Recorded lemma: كَتَبَ.")
        val portuguese = word("Esta forma aparece 3 vezes no Alcorão. Lema registrado: كَتَبَ.")

        assertEquals("This form appears 3 times in the Quran.", english.learnerInsight)
        assertEquals("Esta forma aparece 3 vezes no Alcorão.", portuguese.learnerInsight)
        assertFalse(english.learnerInsight.contains("lemma", ignoreCase = true))
        assertFalse(portuguese.learnerInsight.contains("lema", ignoreCase = true))
    }

    private fun word(insight: String) = QuranWord(
        id = "test",
        arabic = "كَتَبَ",
        lemma = "كَتَبَ",
        transliteration = "kataba",
        meaning = "he wrote",
        root = "ك ت ب",
        grammar = "verb",
        category = "test",
        reference = "Test 1:1",
        verseArabic = "كَتَبَ",
        verseMeaning = "he wrote",
        insight = insight,
    )
}
