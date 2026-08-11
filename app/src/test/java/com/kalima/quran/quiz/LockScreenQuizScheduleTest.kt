package com.kalima.quran.quiz

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockScreenQuizScheduleTest {
    @Test
    fun defaultIntervalShowsQuizAfterThreeWords() {
        var wordsShown = 0
        repeat(3) {
            assertFalse(LockScreenQuizSchedule.shouldShowQuiz(true, wordsShown, 3))
            wordsShown = LockScreenQuizSchedule.afterWord(true, wordsShown)
        }

        assertTrue(LockScreenQuizSchedule.shouldShowQuiz(true, wordsShown, 3))
    }

    @Test
    fun disabledQuizNeverAccumulatesWords() {
        val wordsShown = LockScreenQuizSchedule.afterWord(false, 8)

        assertTrue(wordsShown == 0)
        assertFalse(LockScreenQuizSchedule.shouldShowQuiz(false, 100, 1))
    }
}
