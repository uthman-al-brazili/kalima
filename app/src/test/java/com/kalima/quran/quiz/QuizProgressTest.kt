package com.kalima.quran.quiz

import com.kalima.quran.data.ReviewSchedule
import com.kalima.quran.data.StudyProgress
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizProgressTest {
    @Test
    fun quizResultDoesNotChangeStudyOrSpacedRepetitionState() {
        val wordId = "word-1"
        val now = Instant.parse("2026-08-13T12:00:00Z")
        val schedule = ReviewSchedule(
            repetitions = 1,
            intervalDays = 1,
            dueAt = now.plusSeconds(86_400),
            lastReviewedAt = now,
        )
        val before = StudyProgress(
            learnedIds = setOf("learned"),
            reviewingIds = setOf(wordId),
            todayAnsweredIds = setOf("studied"),
            streakDays = 7,
            reviewSchedules = mapOf(wordId to schedule),
        )

        val after = QuizProgress.record(
            progress = before,
            wordId = wordId,
            correct = true,
            date = LocalDate.of(2026, 8, 13),
        )

        assertEquals(before.learnedIds, after.learnedIds)
        assertEquals(before.reviewingIds, after.reviewingIds)
        assertEquals(before.todayAnsweredIds, after.todayAnsweredIds)
        assertEquals(before.streakDays, after.streakDays)
        assertEquals(before.reviewSchedules, after.reviewSchedules)
        assertEquals(before.reviewEvents, after.reviewEvents)
        assertEquals(1, after.quizCorrectAnswers)
        assertEquals(1, after.quizTotalAnswers)
        assertTrue("2026-08-13" in after.quizCorrectDays.getValue(wordId))
    }
}
