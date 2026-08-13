package com.kalima.quran.ui

import com.kalima.quran.data.ReviewSchedule
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.quiz.QuizMode
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class QuizSessionKeyTest {
    @Test
    fun recordingAnAnswerDoesNotReplaceTheVisibleSession() {
        val before = StudyProgress()
        val now = Instant.parse("2026-08-12T12:00:00Z")
        val after = before.copy(
            reviewingIds = setOf("word-1"),
            todayAnsweredIds = setOf("word-1"),
            quizCorrectAnswers = 1,
            quizTotalAnswers = 1,
            reviewSchedules = mapOf(
                "word-1" to ReviewSchedule(
                    repetitions = 1,
                    intervalDays = 1,
                    dueAt = now.plusSeconds(86_400),
                    lastReviewedAt = now,
                ),
            ),
        )

        assertEquals(
            before.quizSessionKey(QuizMode.Mixed, version = 0),
            after.quizSessionKey(QuizMode.Mixed, version = 0),
        )
    }

    @Test
    fun explicitQuizAndStudyChangesReplaceTheSession() {
        val progress = StudyProgress()
        val original = progress.quizSessionKey(QuizMode.Mixed, version = 0)

        assertNotEquals(original, progress.quizSessionKey(QuizMode.Listening, version = 0))
        assertNotEquals(original, progress.quizSessionKey(QuizMode.Mixed, version = 1))
        assertNotEquals(
            original,
            progress.copy(studyScope = StudyScope.Custom, customStudyIds = setOf("word-1"))
                .quizSessionKey(QuizMode.Mixed, version = 0),
        )
    }
}
