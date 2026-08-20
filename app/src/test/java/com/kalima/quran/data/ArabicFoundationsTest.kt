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
            completedAlphabetLessons = 0,
        )
        val finished = starting.copy(
            completedAlphabetLessons = ArabicFoundations.alphabetLessonCount,
        )

        assertTrue(starting.needsAlphabetFoundation)
        assertFalse(finished.needsAlphabetFoundation)
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
}
