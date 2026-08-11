package com.kalima.quran.quiz

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class QuizMasteryTest {
    @Test
    fun repeatedAnswersOnSameDayCountOnlyOnce() {
        val day = LocalDate.of(2026, 8, 11)
        val first = QuizMastery.recordCorrectDay(emptySet(), day)
        val repeated = QuizMastery.recordCorrectDay(first, day)

        assertTrue(repeated.size == 1)
        assertFalse(QuizMastery.isMastered(repeated))
    }

    @Test
    fun wordIsMasteredAfterThreeDifferentDays() {
        var days = emptySet<String>()
        days = QuizMastery.recordCorrectDay(days, LocalDate.of(2026, 8, 11))
        days = QuizMastery.recordCorrectDay(days, LocalDate.of(2026, 8, 12))
        assertFalse(QuizMastery.isMastered(days))
        days = QuizMastery.recordCorrectDay(days, LocalDate.of(2026, 8, 13))

        assertTrue(QuizMastery.isMastered(days))
    }
}
