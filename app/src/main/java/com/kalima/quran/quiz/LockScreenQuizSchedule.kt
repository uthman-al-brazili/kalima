package com.kalima.quran.quiz

object LockScreenQuizSchedule {
    fun shouldShowQuiz(enabled: Boolean, wordsShown: Int, interval: Int): Boolean =
        enabled && wordsShown >= interval.coerceIn(1, 10)

    fun afterWord(enabled: Boolean, wordsShown: Int): Int =
        if (enabled) wordsShown + 1 else 0
}
