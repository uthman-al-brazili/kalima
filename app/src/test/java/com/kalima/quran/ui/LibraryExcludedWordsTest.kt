package com.kalima.quran.ui

import com.kalima.quran.data.QuranWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LibraryExcludedWordsTest {
    @Test
    fun `excluded view includes words outside the active study set`() {
        val activeWord = word("active")
        val excludedWord = word("excluded")
        val otherWord = word("other")

        val result = librarySearchSource(
            activeWords = listOf(activeWord),
            allWords = listOf(activeWord, excludedWord, otherWord),
            alreadyKnownIds = setOf(excludedWord.id),
            excludedOnly = true,
        )

        assertEquals(listOf(excludedWord), result)
    }

    @Test
    fun `regular filters keep using the active study set`() {
        val activeWords = listOf(word("first"), word("second"))

        val result = librarySearchSource(
            activeWords = activeWords,
            allWords = activeWords + word("outside"),
            alreadyKnownIds = setOf("outside"),
            excludedOnly = false,
        )

        assertSame(activeWords, result)
    }

    private fun word(id: String) = QuranWord(
        id = id,
        arabic = id,
        lemma = id,
        transliteration = id,
        meaning = id,
        root = id,
        grammar = id,
        category = id,
        reference = id,
        verseArabic = id,
        verseMeaning = id,
        insight = id,
    )
}
