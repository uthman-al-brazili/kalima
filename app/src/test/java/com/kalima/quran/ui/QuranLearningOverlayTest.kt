package com.kalima.quran.ui

import com.kalima.quran.data.ReviewSchedule
import com.kalima.quran.data.StudyProgress
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranLearningOverlayTest {
    private val now = Instant.parse("2026-08-27T12:00:00Z")

    @Test
    fun `learned word is recognized`() {
        assertEquals(
            QuranWordLearningState.Recognized,
            classifyQuranReaderWord("word", StudyProgress(learnedIds = setOf("word")), now),
        )
    }

    @Test
    fun `already known word is recognized`() {
        assertEquals(
            QuranWordLearningState.Recognized,
            classifyQuranReaderWord(
                "word",
                StudyProgress(alreadyKnownIds = setOf("word")),
                now,
            ),
        )
    }

    @Test
    fun `reviewing word not yet due remains reviewing`() {
        val progress = reviewingProgress(dueAt = now.plusSeconds(60))

        assertEquals(
            QuranWordLearningState.Reviewing,
            classifyQuranReaderWord("word", progress, now),
        )
    }

    @Test
    fun `reviewing word due now is due`() {
        val progress = reviewingProgress(dueAt = now)

        assertEquals(
            QuranWordLearningState.Due,
            classifyQuranReaderWord("word", progress, now),
        )
    }

    @Test
    fun `new indexed word is unknown`() {
        assertEquals(
            QuranWordLearningState.Unknown,
            classifyQuranReaderWord("word", StudyProgress(), now),
        )
    }

    @Test
    fun `missing vocabulary match is unindexed`() {
        assertEquals(
            QuranWordLearningState.Unindexed,
            classifyQuranReaderWord(null, StudyProgress(), now),
        )
    }

    @Test
    fun `disabled overlay exposes no learning presentation state`() {
        assertNull(
            displayedQuranWordLearningState(
                overlayEnabled = false,
                state = QuranWordLearningState.Due,
            ),
        )
    }

    @Test
    fun `indexed actions route the exact word with state-specific action`() {
        val routedIds = mutableListOf<String>()

        assertEquals(
            QuranReaderStudyAction.Learn,
            launchQuranReaderWordStudy("new-word", QuranWordLearningState.Unknown, routedIds::add),
        )
        assertEquals(
            QuranReaderStudyAction.Review,
            launchQuranReaderWordStudy(
                "review-word",
                QuranWordLearningState.Reviewing,
                routedIds::add,
            ),
        )
        assertEquals(
            QuranReaderStudyAction.Review,
            launchQuranReaderWordStudy("due-word", QuranWordLearningState.Due, routedIds::add),
        )
        assertEquals(
            QuranReaderStudyAction.PracticeAgain,
            launchQuranReaderWordStudy(
                "learned-word",
                QuranWordLearningState.Recognized,
                routedIds::add,
            ),
        )
        assertEquals(
            listOf("new-word", "review-word", "due-word", "learned-word"),
            routedIds,
        )
    }

    @Test
    fun `reader conceals details only for recall actions`() {
        assertFalse(shouldConcealQuranReaderWordDetails(QuranReaderStudyAction.Learn))
        assertTrue(shouldConcealQuranReaderWordDetails(QuranReaderStudyAction.Review))
        assertTrue(shouldConcealQuranReaderWordDetails(QuranReaderStudyAction.PracticeAgain))
        assertFalse(shouldConcealQuranReaderWordDetails(null))
    }

    @Test
    fun `unindexed word has no study action or callback`() {
        var routedId: String? = null

        assertNull(
            launchQuranReaderWordStudy(null, QuranWordLearningState.Unindexed) { routedId = it },
        )
        assertNull(routedId)
    }

    private fun reviewingProgress(dueAt: Instant) = StudyProgress(
        reviewingIds = setOf("word"),
        reviewSchedules = mapOf(
            "word" to ReviewSchedule(dueAt = dueAt, lastReviewedAt = now.minusSeconds(60)),
        ),
    )
}
