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

    @Test
    fun indexedTextTokensIgnoreStandalonePauseMarksWithoutShiftingWordLocations() {
        val selected = word("selected", "نُورٌ").copy(
            audioLocation = QuranWordAudioLocation(1, 1, 2),
        )

        val tokens = VerseExplorer.buildIndexedTextTokens(
            verseArabic = "قُلْ ۞ نُورٌ",
            surahNumber = 1,
            ayahNumber = 1,
            selectedWord = selected,
            resolve = { null },
        )

        assertEquals(listOf("قُلْ", "نُورٌ"), tokens.map(VerseToken::text))
        assertEquals(selected.id, tokens.last().word?.id)
    }

    @Test
    fun indexedTextTokensKeepCorpusCompoundsTogether() {
        val selected = word("ilyasin", "إِلْ يَاسِينَ").copy(
            audioLocation = QuranWordAudioLocation(37, 130, 3),
        )

        val tokens = VerseExplorer.buildIndexedTextTokens(
            verseArabic = "سَلَـٰمٌ عَلَىٰٓ إِلْ يَاسِينَ",
            surahNumber = 37,
            ayahNumber = 130,
            selectedWord = selected,
            resolve = { token -> selected.takeIf { token.wordNumber == 3 } },
        )

        assertEquals(listOf("سَلَـٰمٌ", "عَلَىٰٓ", "إِلْ يَاسِينَ"), tokens.map(VerseToken::text))
        assertEquals(selected.id, tokens.last().word?.id)
    }
}
