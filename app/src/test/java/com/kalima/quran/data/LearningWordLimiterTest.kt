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

    @Test
    fun alreadyKnownWordsAreExcludedWithoutUsingLearningSlots() {
        val result = LearningWordLimiter.apply(
            words = words,
            learnedIds = emptySet(),
            reviewingIds = emptySet(),
            alreadyKnownIds = setOf(words[0].id, words[2].id),
            maximumWords = 3,
        )

        assertEquals(listOf(words[1], words[3], words[4]), result)
    }

    @Test
    fun alreadyKnownWordsAreExcludedWhenTheLimitIsUnlimited() {
        val progress = StudyProgress(alreadyKnownIds = setOf(words[1].id))

        assertEquals(words.filterNot { it.id == words[1].id }, progress.limitNewWords(words))
    }

    @Test
    fun excludedEstablishedWordsNoLongerUseLearningSlots() {
        val result = LearningWordLimiter.apply(
            words = words,
            learnedIds = setOf(words[0].id),
            reviewingIds = emptySet(),
            alreadyKnownIds = setOf(words[0].id),
            maximumWords = 2,
        )

        assertEquals(listOf(words[1], words[2]), result)
    }
}
