package com.kalima.quran.data

import java.time.Instant
import java.time.LocalDate

const val QUIZ_MASTERY_DAYS = 3

enum class AppThemeMode {
    Auto,
    Light,
    Dark,
}

enum class SessionLevel(val newWordLimit: Int, val requestsContextCheckpoint: Boolean) {
    Quick(newWordLimit = 0, requestsContextCheckpoint = false),
    Steady(newWordLimit = 2, requestsContextCheckpoint = false),
    Deep(newWordLimit = 5, requestsContextCheckpoint = true),
    ;

    companion object {
        fun fromPersistedName(stored: String?): SessionLevel =
            entries.firstOrNull { it.name == stored } ?: Steady
    }
}

data class StudyProgress(
    val learnedIds: Set<String> = emptySet(),
    val reviewingIds: Set<String> = emptySet(),
    val alreadyKnownIds: Set<String> = emptySet(),
    val todayAnsweredIds: Set<String> = emptySet(),
    val sessionLevel: SessionLevel = SessionLevel.Steady,
    /** Retained so older preferences and backups can still round-trip. */
    val dailyGoal: Int = 5,
    val maximumWords: Int = LearningWordLimiter.UNLIMITED,
    val streakDays: Int = 0,
    val reminderEnabled: Boolean = false,
    val lockScreenEnabled: Boolean = false,
    /**
     * The guided paths selected together. Empty is a compatibility marker for
     * progress saved before multi-path selection; [studyScope] remains its
     * authoritative single-path value in that case.
     */
    val selectedStudyScopes: Set<StudyScope> = emptySet(),
    val studyScope: StudyScope = StudyScope.All,
    val selectedSurahs: Set<Int> = emptySet(),
    val activeUnderstandPath: UnderstandPathId? = null,
    val understandPathStartedOn: LocalDate? = null,
    val activeUnderstandPathStage: Int = 0,
    val completedUnderstandPaths: Set<UnderstandPathId> = emptySet(),
    val quizCorrectDays: Map<String, Set<String>> = emptyMap(),
    val quizCorrectAnswers: Int = 0,
    val quizTotalAnswers: Int = 0,
    val lockScreenQuizEnabled: Boolean = false,
    val lockScreenQuizInterval: Int = 3,
    val themeMode: AppThemeMode = AppThemeMode.Auto,
    val quranFontSizeSp: Int = QuranReaderTypography.DEFAULT_FONT_SIZE_SP,
    val quranLearningOverlayEnabled: Boolean = false,
    val advancedSettingsVisible: Boolean = false,
    val showCompleteAyah: Boolean = false,
    val spacedRepetitionEnabled: Boolean = true,
    val currentStudyWordId: String? = null,
    val reviewSchedules: Map<String, ReviewSchedule> = emptyMap(),
    /** Per-letter schedules keyed by letter and recognition dimension. */
    val alphabetReviewSchedules: Map<String, ReviewSchedule> = emptyMap(),
    val customStudyIds: Set<String> = emptySet(),
    val onboardingComplete: Boolean = true,
    val alphabetCourseRequested: Boolean = false,
    val alphabetFoundationRequired: Boolean = false,
    val numberCourseRequested: Boolean = false,
    val completedAlphabetLessons: Int = 0,
    val completedNumberLessons: Int = 0,
    val reviewEvents: List<ReviewEvent> = emptyList(),
    val quietHoursEnabled: Boolean = true,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 7,
    val lockScreenDailyLimit: Int = 20,
    val lockScreenCooldownMinutes: Int = 5,
    val lockScreenCardsToday: Int = 0,
    val lockScreenPausedUntil: Instant? = null,
    val lastLockScreenShownAt: Instant? = null,
    val lastLockScreenLatencyMs: Long? = null,
    val lockScreenSafetySkips: Int = 0,
) {
    val studyScopes: Set<StudyScope>
        get() = selectedStudyScopes.ifEmpty { setOf(studyScope) }

    val todayCompleted: Int get() = todayAnsweredIds.size

    fun statusFor(id: String): WordStatus = when (id) {
        in alreadyKnownIds -> WordStatus.AlreadyKnown
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
