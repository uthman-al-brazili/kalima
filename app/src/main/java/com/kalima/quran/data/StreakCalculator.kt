package com.kalima.quran.data

import java.time.LocalDate

object StreakCalculator {
    fun next(current: Int, lastStudyDate: LocalDate?, today: LocalDate): Int = when {
        lastStudyDate == today -> current.coerceAtLeast(1)
        lastStudyDate == today.minusDays(1) -> current + 1
        else -> 1
    }
}
