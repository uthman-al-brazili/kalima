package com.kalima.quran.ui

import com.kalima.quran.data.WordRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class StudyPresentationTest {
    private val words = WordRepository.words.take(2)

    @Test
    fun `session progress shows the current word as a one based position`() {
        assertEquals(1, studySessionPosition(completedWords = 0, sessionWords = 4))
        assertEquals(4, studySessionPosition(completedWords = 3, sessionWords = 4))
    }

    @Test
    fun `active introduction remains visible after it leaves the review queue`() {
        assertEquals(
            listOf(words.first()),
            studyWordsForPresentation(
                queuedWords = emptyList(),
                availableWords = words,
                activeIntroductionId = words.first().id,
            ),
        )
    }

    @Test
    fun `empty queue remains caught up without an active introduction`() {
        assertEquals(
            emptyList<Any>(),
            studyWordsForPresentation(
                queuedWords = emptyList(),
                availableWords = words,
                activeIntroductionId = null,
            ),
        )
    }
}
