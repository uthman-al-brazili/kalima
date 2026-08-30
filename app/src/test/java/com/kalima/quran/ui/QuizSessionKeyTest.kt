package com.kalima.quran.ui

import com.kalima.quran.data.ReviewSchedule
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.UnderstandPathId
import com.kalima.quran.data.WordRepository
import com.kalima.quran.quiz.QuizEngine
import com.kalima.quran.quiz.QuizMode
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

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
            progress.quizSessionKey(
                QuizMode.Mixed,
                version = 0,
                understandPath = UnderstandPathId.AlFatihahSevenDays,
            ),
        )
        assertNotEquals(
            original,
            progress.copy(alreadyKnownIds = setOf("word-1"))
                .quizSessionKey(QuizMode.Mixed, version = 0),
        )
        assertNotEquals(
            original,
            progress.copy(studyScope = StudyScope.Custom, customStudyIds = setOf("word-1"))
                .quizSessionKey(QuizMode.Mixed, version = 0),
        )
    }

    @Test
    fun legacySingleWordCollectionCanOpenAQuizWithEligibleDistractors() {
        val selectedWords = WordRepository.words.take(1)
        val optionWords = quizOptionPool(
            selectedWords = selectedWords,
            allWords = WordRepository.words,
            alreadyKnownIds = emptySet(),
        )

        QuizMode.entries.forEach { mode ->
            val session = QuizEngine.createSession(
                words = selectedWords,
                optionWords = optionWords,
                random = Random(73),
                mode = mode,
            )

            assertEquals(mode.name, 1, session.size)
            assertEquals(selectedWords.single().id, session.single().word.id)
            assertEquals(4, session.single().options.toSet().size)
        }
    }

    @Test
    fun caughtUpSchedulesDoNotRemoveQuizTargets() {
        val selectedWords = WordRepository.words.take(4)
        val now = Instant.parse("2026-08-22T12:00:00Z")
        val progress = StudyProgress(
            reviewingIds = selectedWords.mapTo(mutableSetOf()) { it.id },
            reviewSchedules = selectedWords.associate { word ->
                word.id to ReviewSchedule(
                    repetitions = 2,
                    intervalDays = 3,
                    dueAt = now.plusSeconds(86_400),
                    lastReviewedAt = now,
                )
            },
        )
        val optionWords = quizOptionPool(
            selectedWords = selectedWords,
            allWords = WordRepository.words,
            alreadyKnownIds = progress.alreadyKnownIds,
        )

        val session = QuizEngine.createSession(
            words = selectedWords,
            optionWords = optionWords,
            random = Random(79),
        )

        assertTrue(session.isNotEmpty())
        assertTrue(session.all { question -> question.word.id in progress.reviewingIds })
    }
}
