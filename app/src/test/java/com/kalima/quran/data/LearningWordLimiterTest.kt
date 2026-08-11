package com.kalima.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningWordLimiterTest {
    private val words = WordRepository.words.take(8)

    @Test
    fun unlimitedKeepsTheWholeSelectedSource() {
        val result = LearningWordLimiter.apply(
            words = words,
            learnedIds = emptySet(),
            reviewingIds = emptySet(),
            maximumWords = LearningWordLimiter.UNLIMITED,
        )

        assertEquals(words, result)
    }

    @Test
    fun onlyEnoughNewCardsAreAddedToFillTheGlobalLimit() {
        val result = LearningWordLimiter.apply(
            words = words,
            learnedIds = setOf(words[2].id),
            reviewingIds = setOf("established-outside-current-scope"),
            maximumWords = 4,
        )

        assertEquals(listOf(words[0], words[1], words[2]), result)
        assertEquals(2, result.count { it.id !in setOf(words[2].id) })
    }

    @Test
    fun establishedCardsRemainWhenTheLimitIsLoweredBelowCurrentProgress() {
        val established = words.take(4).map(QuranWord::id).toSet()

        val result = LearningWordLimiter.apply(
            words = words,
            learnedIds = established,
            reviewingIds = emptySet(),
            maximumWords = 2,
        )

        assertEquals(words.take(4), result)
        assertTrue(result.none { it.id !in established })
    }

    @Test
    fun reachingTheLimitBlocksNewCardsInASelectionWithoutEstablishedWords() {
        val result = LearningWordLimiter.apply(
            words = words,
            learnedIds = setOf("learned-elsewhere"),
            reviewingIds = setOf("reviewing-elsewhere"),
            maximumWords = 2,
        )

        assertTrue(result.isEmpty())
    }
}
