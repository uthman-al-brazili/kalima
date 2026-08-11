package com.kalima.quran.quiz

import com.kalima.quran.data.QUIZ_MASTERY_DAYS
import java.time.LocalDate

object QuizMastery {
    fun recordCorrectDay(existingDays: Set<String>, date: LocalDate): Set<String> =
        existingDays + date.toString()

    fun isMastered(correctDays: Set<String>): Boolean =
        correctDays.size >= QUIZ_MASTERY_DAYS
}
