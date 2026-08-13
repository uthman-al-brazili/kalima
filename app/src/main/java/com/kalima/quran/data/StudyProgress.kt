package com.kalima.quran.data

import java.time.Instant

const val QUIZ_MASTERY_DAYS = 3

enum class AppThemeMode {
    Auto,
    Light,
    Dark,
}

data class StudyProgress(
    val learnedIds: Set<String> = emptySet(),
    val reviewingIds: Set<String> = emptySet(),
    val todayAnsweredIds: Set<String> = emptySet(),
    val dailyGoal: Int = 5,
    val maximumWords: Int = LearningWordLimiter.UNLIMITED,
    val streakDays: Int = 0,
    val reminderEnabled: Boolean = false,
    val lockScreenEnabled: Boolean = false,
    val studyScope: StudyScope = StudyScope.All,
    val selectedSurahs: Set<Int> = emptySet(),
    val quizCorrectDays: Map<String, Set<String>> = emptyMap(),
    val quizCorrectAnswers: Int = 0,
    val quizTotalAnswers: Int = 0,
    val lockScreenQuizEnabled: Boolean = false,
    val lockScreenQuizInterval: Int = 3,
    val themeMode: AppThemeMode = AppThemeMode.Auto,
    val advancedSettingsVisible: Boolean = false,
    val spacedRepetitionEnabled: Boolean = true,
    val currentStudyWordId: String? = null,
    val reviewSchedules: Map<String, ReviewSchedule> = emptyMap(),
    val favoriteIds: Set<String> = emptySet(),
    val customStudyIds: Set<String> = emptySet(),
    val onboardingComplete: Boolean = true,
    val reviewEvents: List<ReviewEvent> = emptyList(),
    val quietHoursEnabled: Boolean = true,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 7,
    val lockScreenDailyLimit: Int = 20,
    val lockScreenCardsToday: Int = 0,
    val lockScreenPausedUntil: Instant? = null,
) {
    val todayCompleted: Int get() = todayAnsweredIds.size

    fun statusFor(id: String): WordStatus = when (id) {
        in learnedIds -> WordStatus.Learned
        in reviewingIds -> WordStatus.Reviewing
        else -> WordStatus.New
    }

    fun quizCorrectDayCount(id: String): Int = quizCorrectDays[id].orEmpty().size

    fun scheduleFor(id: String): ReviewSchedule? = reviewSchedules[id]

    fun dueReviewCount(wordIds: Set<String>, now: Instant = Instant.now()): Int =
        if (spacedRepetitionEnabled) {
            wordIds.count { id -> reviewSchedules[id]?.isDue(now) == true }
        } else {
            0
        }

    fun accuracy(days: Long, now: Instant = Instant.now()): Int? =
        ReviewHistory.accuracy(reviewEvents, days, now)
}
