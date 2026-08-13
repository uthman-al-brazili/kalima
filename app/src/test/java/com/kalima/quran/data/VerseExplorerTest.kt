package com.kalima.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VerseExplorerTest {
    private fun word(id: String, arabic: String) = QuranWord(
        id, arabic, arabic, "", "", "", "", "", "Quran 1:1", "مِنَ ٱللَّهِ نُورٌ", "", "",
    )

    @Test
    fun mapsVerseTokensToOccurrenceCardsIgnoringMarksAndAlifVariants() {
        val tokens = VerseExplorer.buildTokens(
            "مِنَ ٱللَّهِ نُورٌ",
            listOf(word("one", "مِن"), word("two", "اللَّهِ"), word("three", "نُور")),
        )
        assertEquals(listOf("one", "two", "three"), tokens.map { it.word?.id })
    }

    @Test
    fun leavesPunctuationOrMissingCorpusTokensNonInteractive() {
        val tokens = VerseExplorer.buildTokens("قُلْ ۞ نُورٌ", listOf(word("one", "قُل")))
        assertEquals("one", tokens[0].word?.id)
        assertNull(tokens[1].word)
        assertNull(tokens[2].word)
    }
}
