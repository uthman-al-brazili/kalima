package com.kalima.quran.ui

import com.kalima.quran.data.ReviewSchedule
import com.kalima.quran.data.StudyProgress
import java.time.Instant
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizExperienceRegressionTest {
    @Test
    fun `perfect current quiz reports one hundred percent`() {
        assertEquals(100, quizSessionAccuracy(score = 5, total = 5))
        assertEquals(80, quizSessionAccuracy(score = 4, total = 5))
        assertEquals(0, quizSessionAccuracy(score = 0, total = 0))
    }

    @Test
    fun `tabs save quiz and study state instead of recreating them`() {
        val app = source("ui/KalimaApp.kt")
        val quiz = source("ui/QuizScreen.kt")
        val study = source("ui/StudyScreen.kt")

        assertTrue(app.contains("stateHolder.SaveableStateProvider(selected.name, content)"))
        assertFalse(app.contains("if (selected == AppTab.Quiz)"))
        assertTrue(quiz.contains("rememberSaveable(sessionKey)"))
        assertTrue(quiz.contains("Random(sessionSeed)"))
        assertTrue(study.contains("scrollPositionWordId"))
    }

    @Test
    fun `quiz feedback always includes the tested words translation`() {
        val quiz = source("ui/QuizScreen.kt")
        assertTrue(quiz.contains("R.string.quiz_word_translation, question.word.meaning"))
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

    private fun source(relative: String): String = sequenceOf(
        File("src/main/java/com/kalima/quran/$relative"),
        File("app/src/main/java/com/kalima/quran/$relative"),
    ).firstOrNull(File::isFile)?.readText()
        ?: error("Android source not found: $relative")
}
