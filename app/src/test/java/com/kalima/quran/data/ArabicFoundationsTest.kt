package com.kalima.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicFoundationsTest {
    @Test
    fun `alphabet plan covers every letter once before the vowel lesson`() {
        val letters = ArabicFoundations.alphabetLessons
            .filterNot(AlphabetLesson::teachesVowels)
            .flatMap(AlphabetLesson::symbols)
            .map(FoundationSymbol::arabic)

        assertEquals(28, letters.size)
        assertEquals(28, letters.toSet().size)
        assertEquals("ا", letters.first())
        assertEquals("ي", letters.last())
    }

    @Test
    fun `alphabet lessons block complete words until finished`() {
        val starting = StudyProgress(
            alphabetCourseRequested = true,
            alphabetFoundationRequired = true,
            completedAlphabetLessons = 0,
        )
        val finished = starting.copy(
            completedAlphabetLessons = ArabicFoundations.alphabetLessonCount,
        )

        assertTrue(starting.needsAlphabetFoundation)
        assertFalse(finished.needsAlphabetFoundation)
    }

    @Test
    fun `every alphabet lesson has a complete recognition check`() {
        ArabicFoundations.alphabetLessons.forEach { lesson ->
            val questions = lesson.practiceQuestions()

            assertEquals(lesson.symbols.size, questions.size)
            assertEquals(
                lesson.symbols,
                questions.map(AlphabetPracticeQuestion::symbol),
            )
            questions.forEach { question ->
                assertEquals(lesson.symbols.size, question.options.size)
                assertEquals(lesson.symbols.size, question.options.toSet().size)
                assertEquals(
                    question.symbol.transliteration,
                    question.options[question.correctOptionIndex],
                )
            }
            assertEquals(
                lesson.symbols.indices.toSet(),
                questions.map(AlphabetPracticeQuestion::correctOptionIndex).toSet(),
            )
        }
    }

    @Test
    fun `every foundation symbol provides Arabic speech text`() {
        val symbols = ArabicFoundations.alphabetLessons.flatMap(AlphabetLesson::symbols)

        assertTrue(symbols.all { it.spokenArabic.isNotBlank() })
        assertEquals("أَلِف", symbols.first().spokenArabic)
        assertEquals("يَاء", symbols.dropLast(4).last().spokenArabic)
    }

    @Test
    fun `alphabet reference covers all letters and four vowel forms with speech`() {
        val reference = ArabicFoundations.alphabetReference

        assertEquals(28, reference.size)
        assertEquals(28, reference.map { it.letter.arabic }.toSet().size)
        reference.forEach { row ->
            assertEquals(4, row.vowelVariants.size)
            assertEquals(
                listOf('َ', 'ِ', 'ُ', 'ْ'),
                row.vowelVariants.map { it.arabic.last() },
            )
            assertTrue(row.vowelVariants.all { it.spokenArabic == it.arabic })
            assertTrue(row.vowelVariants.all { it.transliteration.isNotBlank() })
        }
    }

    @Test
    fun `alphabet reference uses accurate hamza forms for initial alif vowels`() {
        val alif = ArabicFoundations.alphabetReference.first()

        assertEquals(listOf("أَ", "إِ", "أُ", "أْ"), alif.vowelVariants.map(FoundationSymbol::arabic))
        assertEquals(listOf("ʾa", "ʾi", "ʾu", "ʾ"), alif.vowelVariants.map(FoundationSymbol::transliteration))
    }

    @Test
    fun `alphabet reference search accepts Arabic and transliterated letter names`() {
        assertEquals(listOf("ق"), ArabicFoundations.alphabetReferenceMatching("ق").map { it.letter.arabic })
        assertEquals(listOf("ق"), ArabicFoundations.alphabetReferenceMatching("qaf").map { it.letter.arabic })
        assertEquals(listOf("ح"), ArabicFoundations.alphabetReferenceMatching("ḥāʾ").map { it.letter.arabic })
        assertEquals(ArabicFoundations.alphabetReference, ArabicFoundations.alphabetReferenceMatching(""))
    }

    @Test
    fun `alphabet reference has compact pages of four letters`() {
        val pages = ArabicFoundations.alphabetReference.chunked(ArabicFoundations.alphabetReferencePageSize)

        assertEquals(7, pages.size)
        assertTrue(pages.all { it.size == ArabicFoundations.alphabetReferencePageSize })
        assertEquals(listOf("ا", "ب", "ت", "ث"), pages.first().map { it.letter.arabic })
    }

    @Test
    fun `skipping alphabet keeps lesson progress and resuming continues it`() {
        val inProgress = StudyProgress(
            alphabetCourseRequested = true,
            alphabetFoundationRequired = true,
            completedAlphabetLessons = 3,
        )

        val skipped = inProgress.skipAlphabetFoundation()
        val resumed = skipped.startAlphabetFoundation()

        assertFalse(skipped.needsAlphabetFoundation)
        assertEquals(3, skipped.completedAlphabetLessons)
        assertFalse(resumed.needsAlphabetFoundation)
        assertTrue(resumed.hasAlphabetFoundationLesson)
        assertEquals(3, resumed.completedAlphabetLessons)
    }

    @Test
    fun `a completed or previously declined alphabet course can be studied from the beginning`() {
        val previouslyKnown = StudyProgress(
            alphabetCourseRequested = false,
            completedAlphabetLessons = ArabicFoundations.alphabetLessonCount,
        )

        val restarted = previouslyKnown.startAlphabetFoundation()

        assertFalse(restarted.needsAlphabetFoundation)
        assertTrue(restarted.hasAlphabetFoundationLesson)
        assertEquals(0, restarted.completedAlphabetLessons)
    }

    @Test
    fun `number lessons remain parallel and cover zero through nine`() {
        val progress = StudyProgress(
            numberCourseRequested = true,
            completedNumberLessons = 4,
        )

        assertFalse(progress.needsAlphabetFoundation)
        assertTrue(progress.hasNumberFoundationLesson)
        assertEquals((0..9).toList(), ArabicFoundations.numberLessons.map(NumberLesson::westernDigit))
        assertEquals(
            listOf("٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩"),
            ArabicFoundations.numberLessons.map(NumberLesson::arabicDigit),
        )
    }

    @Test
    fun `a completed or previously declined number course can be studied from the beginning`() {
        val previouslyKnown = StudyProgress(
            numberCourseRequested = false,
            completedNumberLessons = ArabicFoundations.numberLessonCount,
        )

        val restarted = previouslyKnown.startNumberFoundation()

        assertTrue(restarted.hasNumberFoundationLesson)
        assertEquals(0, restarted.completedNumberLessons)
    }
}
