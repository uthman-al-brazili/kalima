package com.kalima.quran.ui

import com.kalima.quran.data.ReviewSchedule
import com.kalima.quran.data.StudyProgress
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class QuizExperienceRegressionTest {
    @Test
    fun `perfect current quiz reports one hundred percent`() {
        assertEquals(100, quizSessionAccuracy(score = 5, total = 5))
        assertEquals(80, quizSessionAccuracy(score = 4, total = 5))
        assertEquals(0, quizSessionAccuracy(score = 0, total = 0))
    }

    @Test
    fun `path quiz eligibility contains only encountered or claimed known words`() {
        val progress = StudyProgress(
            learnedIds = setOf("learned"),
            reviewingIds = setOf("reviewing"),
            alreadyKnownIds = setOf("known"),
            reviewSchedules = mapOf(
                "scheduled" to ReviewSchedule(
                    repetitions = 0,
                    intervalDays = 0,
                    easeFactor = 2.5,
                    dueAt = Instant.EPOCH,
                    lastReviewedAt = Instant.EPOCH,
                    lapses = 0,
                ),
            ),
        )

        assertEquals(
            setOf("learned", "reviewing", "known", "scheduled"),
            pathQuizEligibleWordIds(progress),
        )
        assertFalse("unseen" in pathQuizEligibleWordIds(progress))
    }
}
