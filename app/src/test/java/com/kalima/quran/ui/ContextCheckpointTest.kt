package com.kalima.quran.ui

import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.ReviewSchedule
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.quiz.QuizProgress
import com.kalima.quran.quiz.VerseExcerptBuilder
import java.io.File
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextCheckpointTest {
    private val words = listOf(
        word("one", "كَلِمَةٌ"),
        word("two", "كِتَابٌ"),
        word("three", "نُورٌ"),
        word("four", "حَقٌّ"),
        word("five", "عِلْمٌ"),
    )

    @Test
    fun `cloze removes the correct word and restores the complete ayah`() {
        val target = words.first()

        val cloze = VerseExcerptBuilder.buildFullCloze(target)

        assertNotNull(cloze)
        requireNotNull(cloze)
        assertTrue(cloze.text.contains(VerseExcerptBuilder.CLOZE_BLANK))
        assertFalse(cloze.text.contains(target.arabic))
        assertEquals(target.verseArabic, cloze.restore())
    }

    @Test
    fun `checkpoint has four unique meaning choices including the answer`() {
        val checkpoint = buildContextCheckpointQuestion(
            practicedWords = listOf(words.first()),
            activeCollection = words,
            seed = 19,
        )

        assertNotNull(checkpoint)
        requireNotNull(checkpoint)
        assertEquals(4, checkpoint.options.size)
        assertEquals(4, checkpoint.options.toSet().size)
        assertTrue(checkpoint.word.meaning in checkpoint.options)
        assertEquals(checkpoint.word.meaning, checkpoint.options[checkpoint.correctOptionIndex])
        assertEquals(checkpoint.word.verseArabic, checkpoint.ayah.text)
        assertTrue(checkpoint.ayah.hasHighlight)
    }

    @Test
    fun `checkpoint generation is deterministic for the same inputs and seed`() {
        val practiced = listOf(words[2], words[0], words[1])

        val first = buildContextCheckpointQuestion(practiced, words, seed = 31)
        val second = buildContextCheckpointQuestion(practiced.reversed(), words.reversed(), seed = 31)

        assertNotNull(first)
        assertEquals(first, second)
    }

    @Test
    fun `guided checkpoint prefers a current ayah word`() {
        val checkpoint = buildContextCheckpointQuestion(
            practicedWords = words.take(3),
            activeCollection = words,
            seed = 31,
            preferredWordIds = setOf(words[1].id),
        )

        assertNotNull(checkpoint)
        assertEquals(words[1].id, checkpoint?.word?.id)
    }

    @Test
    fun `checkpoint is skipped when three distractors cannot be produced`() {
        val checkpoint = buildContextCheckpointQuestion(
            practicedWords = listOf(words.first()),
            activeCollection = words.take(3),
            seed = 43,
        )

        assertNull(checkpoint)
    }

    @Test
    fun `all repeated occurrences of a word are replaced`() {
        val target = words.first().copy(
            verseArabic = "${words.first().arabic} فِي آيَةٍ ${words.first().arabic}",
        )

        val cloze = VerseExcerptBuilder.buildFullCloze(target)

        assertNotNull(cloze)
        requireNotNull(cloze)
        assertEquals(2, cloze.removedOccurrences)
        assertEquals(
            2,
            Regex(Regex.escape(VerseExcerptBuilder.CLOZE_BLANK)).findAll(cloze.text).count(),
        )
        assertEquals(target.verseArabic, cloze.restore())
    }

    @Test
    fun `checkpoint quiz result does not increase daily word count or reschedule review`() {
        val schedule = ReviewSchedule(
            dueAt = Instant.parse("2026-08-30T12:00:00Z"),
            lastReviewedAt = Instant.parse("2026-08-29T12:00:00Z"),
        )
        val before = StudyProgress(
            todayAnsweredIds = setOf("already-complete"),
            reviewSchedules = mapOf(words.first().id to schedule),
        )

        val after = QuizProgress.record(
            progress = before,
            wordId = words.first().id,
            correct = true,
            date = LocalDate.of(2026, 8, 29),
        )

        assertEquals(before.todayCompleted, after.todayCompleted)
        assertEquals(before.todayAnsweredIds, after.todayAnsweredIds)
        assertEquals(before.reviewSchedules, after.reviewSchedules)
        assertEquals(before.quizTotalAnswers + 1, after.quizTotalAnswers)
    }

    @Test
    fun `checkpoint routes its answer through quiz-only progress`() {
        val studySource = File("src/main/java/com/kalima/quran/ui/StudyScreen.kt").readText()
        val appSource = File("src/main/java/com/kalima/quran/ui/KalimaApp.kt").readText()

        assertTrue(studySource.contains("onAnswer = onCheckpointAnswer"))
        assertTrue(appSource.contains("onCheckpointAnswer = onQuizAnswer"))
    }

    @Test
    fun `feedback distinguishes correct and incorrect answers`() {
        assertEquals(
            ContextCheckpointFeedbackState.Correct,
            contextCheckpointFeedbackState(selectedOptionIndex = 2, correctOptionIndex = 2),
        )
        assertEquals(
            ContextCheckpointFeedbackState.Incorrect,
            contextCheckpointFeedbackState(selectedOptionIndex = 1, correctOptionIndex = 2),
        )
    }

    private fun word(id: String, arabic: String) = QuranWord(
        id = id,
        arabic = arabic,
        lemma = arabic,
        transliteration = id,
        meaning = "meaning-$id",
        root = "root",
        grammar = "noun",
        category = "test",
        reference = "Test 1:1",
        verseArabic = "$arabic فِي آيَةٍ",
        verseMeaning = "A contextual test ayah.",
        insight = "",
    )
}
