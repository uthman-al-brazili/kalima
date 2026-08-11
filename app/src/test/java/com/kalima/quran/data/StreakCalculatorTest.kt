package com.kalima.quran.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {
    private val today = LocalDate.of(2026, 8, 10)

    @Test
    fun startsAtOneWithoutPreviousStudy() {
        assertEquals(1, StreakCalculator.next(0, null, today))
    }

    @Test
    fun incrementsAfterConsecutiveDay() {
        assertEquals(5, StreakCalculator.next(4, today.minusDays(1), today))
    }

    @Test
    fun doesNotIncrementTwiceOnSameDay() {
        assertEquals(4, StreakCalculator.next(4, today, today))
    }

    @Test
    fun resetsAfterMissedDay() {
        assertEquals(1, StreakCalculator.next(9, today.minusDays(2), today))
    }
}
