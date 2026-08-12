package com.kalima.quran.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.kalima.quran.quiz.LockScreenContent
import com.kalima.quran.quiz.LockScreenQuizSchedule
import com.kalima.quran.quiz.QuizEngine
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ProgressStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    init {
        WordRepository.initialize(context.applicationContext)
    }

    private val today = { LocalDate.now() }
    private val _progress = MutableStateFlow(load())
    val progress: StateFlow<StudyProgress> = _progress.asStateFlow()

    fun answer(wordId: String, learned: Boolean) {
        recordAnswer(wordId, learned, ReviewSource.Study)
    }

    fun answerFromLockScreen(wordId: String, learned: Boolean) {
        recordAnswer(wordId, learned, ReviewSource.LockScreen)
    }

    private fun recordAnswer(wordId: String, learned: Boolean, source: ReviewSource) {
        val moment = Instant.now()
        val date = today()
        val previous = refreshDayIfNeeded(_progress.value, date)
        val wasNew = wordId !in previous.reviewSchedules
        val schedules = previous.reviewSchedules + (
            wordId to SpacedRepetition.review(
                previous = previous.reviewSchedules[wordId],
                grade = if (learned) ReviewGrade.Good else ReviewGrade.Again,
                now = moment,
            )
        )
        val (learnedIds, reviewingIds) = statusSets(schedules)

        val answeredToday = previous.todayAnsweredIds + wordId
        val lastStudyDate = preferences.getString(KEY_LAST_STUDY_DATE, null)
            ?.let(LocalDate::parse)
        val updated = previous.copy(
            learnedIds = learnedIds,
            reviewingIds = reviewingIds,
            todayAnsweredIds = answeredToday,
            streakDays = StreakCalculator.next(previous.streakDays, lastStudyDate, date),
            reviewSchedules = schedules,
            reviewEvents = ReviewHistory.append(
                previous.reviewEvents,
                ReviewEvent(moment, wordId, learned, wasNew, source),
            ),
        )
        preferences.edit { putString(KEY_LAST_STUDY_DATE, date.toString()) }
        persist(updated, date)
    }

    fun answerQuiz(wordId: String, correct: Boolean) {
        recordQuizAnswer(wordId, correct, ReviewSource.Quiz)
    }

    fun answerQuizFromLockScreen(wordId: String, correct: Boolean) {
        recordQuizAnswer(wordId, correct, ReviewSource.LockScreen)
    }

    private fun recordQuizAnswer(wordId: String, correct: Boolean, source: ReviewSource) {
        val moment = Instant.now()
        val date = today()
        val previous = refreshDayIfNeeded(_progress.value, date)
        val wasNew = wordId !in previous.reviewSchedules
        val schedules = previous.reviewSchedules + (
            wordId to SpacedRepetition.review(
                previous = previous.reviewSchedules[wordId],
                grade = if (correct) ReviewGrade.Good else ReviewGrade.Again,
                now = moment,
            )
        )
        val (learnedIds, reviewingIds) = statusSets(schedules)
        val lastStudyDate = preferences.getString(KEY_LAST_STUDY_DATE, null)
            ?.let(LocalDate::parse)
        val updated = previous.copy(
            learnedIds = learnedIds,
            reviewingIds = reviewingIds,
            todayAnsweredIds = previous.todayAnsweredIds + wordId,
            streakDays = StreakCalculator.next(previous.streakDays, lastStudyDate, date),
            quizCorrectAnswers = previous.quizCorrectAnswers + if (correct) 1 else 0,
            quizTotalAnswers = previous.quizTotalAnswers + 1,
            quizCorrectDays = if (correct) {
                previous.quizCorrectDays + (
                    wordId to (previous.quizCorrectDays[wordId].orEmpty() + date.toString())
                )
            } else {
                previous.quizCorrectDays
            },
            reviewSchedules = schedules,
            reviewEvents = ReviewHistory.append(
                previous.reviewEvents,
                ReviewEvent(moment, wordId, correct, wasNew, source),
            ),
        )
        preferences.edit { putString(KEY_LAST_STUDY_DATE, date.toString()) }
        persist(updated, date)
    }

    fun setReminderEnabled(enabled: Boolean) {
        val updated = _progress.value.copy(reminderEnabled = enabled)
        persist(updated, today())
    }

    fun setLockScreenEnabled(enabled: Boolean) {
        val updated = _progress.value.copy(lockScreenEnabled = enabled)
        persist(updated, today())
    }

    fun setLockScreenQuizEnabled(enabled: Boolean) {
        preferences.edit { putInt(KEY_LOCK_SCREEN_WORDS_SINCE_QUIZ, 0) }
        persist(_progress.value.copy(lockScreenQuizEnabled = enabled), today())
    }

    fun setLockScreenQuizInterval(interval: Int) {
        preferences.edit { putInt(KEY_LOCK_SCREEN_WORDS_SINCE_QUIZ, 0) }
        persist(
            _progress.value.copy(lockScreenQuizInterval = interval.coerceIn(1, 10)),
            today(),
        )
    }

    fun setStudyScope(scope: StudyScope) {
        val selectedSurahs = if (scope == StudyScope.Surahs && _progress.value.selectedSurahs.isEmpty()) {
            setOf(DEFAULT_SURAH)
        } else {
            _progress.value.selectedSurahs
        }
        persist(
            _progress.value.copy(studyScope = scope, selectedSurahs = selectedSurahs),
            today(),
        )
    }

    fun toggleFavorite(wordId: String) {
        if (WordRepository.words.none { it.id == wordId }) return
        val ids = _progress.value.favoriteIds.toMutableSet().apply {
            if (!add(wordId)) remove(wordId)
        }
        persist(_progress.value.copy(favoriteIds = ids), today())
    }

    fun toggleCustomStudy(wordId: String) {
        if (WordRepository.words.none { it.id == wordId }) return
        val ids = _progress.value.customStudyIds.toMutableSet().apply {
            if (!add(wordId)) remove(wordId)
        }
        persist(_progress.value.copy(customStudyIds = ids), today())
    }

    fun completeOnboarding(scope: StudyScope, dailyGoal: Int) {
        persist(
            _progress.value.copy(
                onboardingComplete = true,
                studyScope = scope,
                dailyGoal = dailyGoal.coerceIn(3, 20),
            ),
            today(),
        )
    }

    fun toggleSurah(surahNumber: Int) {
        if (surahNumber !in AVAILABLE_SURAHS) return
        val selected = _progress.value.selectedSurahs.toMutableSet().apply {
            if (!add(surahNumber)) remove(surahNumber)
        }
        val updated = if (selected.isEmpty()) {
            _progress.value.copy(studyScope = StudyScope.All, selectedSurahs = emptySet())
        } else {
            _progress.value.copy(studyScope = StudyScope.Surahs, selectedSurahs = selected)
        }
        persist(updated, today())
    }

    fun canShowLockScreenCard(now: Instant = Instant.now()): Boolean =
        lockScreenBlockReason(now) == null

    fun lockScreenBlockReason(now: Instant = Instant.now()): LockScreenBlockReason? {
        val current = refreshDayIfNeeded(_progress.value, today())
        return LockScreenPolicy.blockReason(
            enabled = current.lockScreenEnabled,
            pausedUntil = current.lockScreenPausedUntil,
            quietHoursEnabled = current.quietHoursEnabled,
            quietStartHour = current.quietStartHour,
            quietEndHour = current.quietEndHour,
            dailyLimit = current.lockScreenDailyLimit,
            shownToday = current.lockScreenCardsToday,
            now = now,
        )
    }

    fun nextLockScreenContent(preview: Boolean = false): LockScreenContent? {
        val current = _progress.value
        if (!preview && !canShowLockScreenCard()) return null
        val selectedSource = WordRepository.wordsFor(
            current.studyScope,
            current.selectedSurahs,
            current.favoriteIds,
            current.customStudyIds,
        )
        val available = current.limitNewWords(selectedSource)
        val moment = Instant.now()
        val dueWords = ReviewQueue.dueWords(available, current.reviewSchedules, moment)
        val source = dueWords.ifEmpty { ReviewQueue.newWords(available, current.reviewSchedules) }
        if (source.isEmpty()) return null
        val wordsSinceQuiz = preferences.getInt(KEY_LOCK_SCREEN_WORDS_SINCE_QUIZ, 0)
        if (
            LockScreenQuizSchedule.shouldShowQuiz(
                enabled = current.lockScreenQuizEnabled,
                wordsShown = wordsSinceQuiz,
                interval = current.lockScreenQuizInterval,
            )
        ) {
            val quizSequence = preferences.getInt(KEY_LOCK_SCREEN_QUIZ_SEQUENCE, 0)
            val question = runCatching {
                QuizEngine.createLockScreenQuestion(
                    words = source,
                    statusFor = { id ->
                        if (current.reviewSchedules.containsKey(id)) {
                            WordStatus.Reviewing
                        } else {
                            WordStatus.New
                        }
                    },
                    sequence = quizSequence,
                    optionWords = selectedSource,
                )
            }.getOrNull()
            if (question != null) {
                preferences.edit {
                    putInt(KEY_LOCK_SCREEN_QUIZ_SEQUENCE, quizSequence + 1)
                    putInt(KEY_LOCK_SCREEN_WORDS_SINCE_QUIZ, 0)
                }
                if (!preview) recordLockScreenCardShown()
                return LockScreenContent.QuizCard(question)
            }
        }

        val sequence = preferences.getInt(KEY_LOCK_SCREEN_SEQUENCE, 0)
        val word = WordRepository.wordAtSequence(sequence, source)
        preferences.edit {
            putInt(KEY_LOCK_SCREEN_SEQUENCE, sequence + 1)
            putInt(
                KEY_LOCK_SCREEN_WORDS_SINCE_QUIZ,
                LockScreenQuizSchedule.afterWord(current.lockScreenQuizEnabled, wordsSinceQuiz),
            )
        }
        if (!preview) recordLockScreenCardShown()
        return LockScreenContent.WordCard(word)
    }

    private fun recordLockScreenCardShown() {
        val current = refreshDayIfNeeded(_progress.value, today())
        persist(current.copy(lockScreenCardsToday = current.lockScreenCardsToday + 1), today())
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        persist(_progress.value.copy(quietHoursEnabled = enabled), today())
    }

    fun setQuietHours(startHour: Int, endHour: Int) {
        persist(
            _progress.value.copy(
                quietStartHour = startHour.coerceIn(0, 23),
                quietEndHour = endHour.coerceIn(0, 23),
            ),
            today(),
        )
    }

    fun setLockScreenDailyLimit(limit: Int) {
        persist(_progress.value.copy(lockScreenDailyLimit = limit.coerceIn(0, 100)), today())
    }

    fun pauseLockScreenForHour() {
        persist(
            _progress.value.copy(lockScreenPausedUntil = Instant.now().plus(1, ChronoUnit.HOURS)),
            today(),
        )
    }

    fun pauseLockScreenUntilTomorrow() {
        persist(
            _progress.value.copy(lockScreenPausedUntil = LockScreenPolicy.pauseUntilTomorrow()),
            today(),
        )
    }

    fun resumeLockScreen() {
        persist(_progress.value.copy(lockScreenPausedUntil = null), today())
    }

    fun setDailyGoal(goal: Int) {
        val updated = _progress.value.copy(dailyGoal = goal.coerceIn(3, 20))
        persist(updated, today())
    }

    fun setMaximumWords(maximumWords: Int) {
        val normalized = if (maximumWords == LearningWordLimiter.UNLIMITED) {
            LearningWordLimiter.UNLIMITED
        } else {
            maximumWords.coerceIn(LearningWordLimiter.MINIMUM_LIMIT, WordRepository.words.size)
        }
        preferences.edit {
            putInt(KEY_LOCK_SCREEN_SEQUENCE, 0)
            putInt(KEY_LOCK_SCREEN_QUIZ_SEQUENCE, 0)
        }
        persist(_progress.value.copy(maximumWords = normalized), today())
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        if (_progress.value.themeMode == themeMode) return
        persist(_progress.value.copy(themeMode = themeMode), today())
    }

    fun setAdvancedSettingsVisible(visible: Boolean) {
        if (_progress.value.advancedSettingsVisible == visible) return
        persist(_progress.value.copy(advancedSettingsVisible = visible), today())
    }

    fun setCurrentStudyWord(wordId: String) {
        if (_progress.value.currentStudyWordId == wordId) return
        if (WordRepository.words.none { it.id == wordId }) return
        persist(_progress.value.copy(currentStudyWordId = wordId), today())
    }

    private fun load(): StudyProgress {
        val date = today()
        val moment = Instant.now()
        val storedDay = preferences.getString(KEY_TODAY, null)
        val answered = if (storedDay == date.toString()) {
            preferences.getStringSet(KEY_TODAY_ANSWERED, emptySet()).orEmpty()
        } else {
            emptySet()
        }
        val lockCardsToday = if (preferences.getString(KEY_LOCK_SCREEN_COUNT_DATE, null) == date.toString()) {
            preferences.getInt(KEY_LOCK_SCREEN_CARDS_TODAY, 0)
        } else {
            0
        }
        val storedLearnedIds = preferences.getStringSet(KEY_LEARNED, emptySet()).orEmpty()
        val storedReviewingIds = preferences.getStringSet(KEY_REVIEWING, emptySet()).orEmpty()
        val schedules = ReviewScheduleCodec.decode(
            preferences.getStringSet(KEY_REVIEW_SCHEDULES, emptySet()).orEmpty(),
        ).toMutableMap().apply {
            (storedLearnedIds + storedReviewingIds).forEach { id ->
                putIfAbsent(id, SpacedRepetition.migrated(id in storedLearnedIds, moment))
            }
        }
        val (learnedIds, reviewingIds) = statusSets(schedules)
        return StudyProgress(
            learnedIds = learnedIds,
            reviewingIds = reviewingIds,
            todayAnsweredIds = answered,
            dailyGoal = preferences.getInt(KEY_DAILY_GOAL, 5),
            maximumWords = preferences.getInt(
                KEY_MAXIMUM_WORDS,
                LearningWordLimiter.UNLIMITED,
            ).let { stored ->
                if (stored == LearningWordLimiter.UNLIMITED) {
                    LearningWordLimiter.UNLIMITED
                } else {
                    stored.coerceIn(LearningWordLimiter.MINIMUM_LIMIT, WordRepository.words.size)
                }
            },
            streakDays = preferences.getInt(KEY_STREAK, 0),
            reminderEnabled = preferences.getBoolean(KEY_REMINDER, false),
            lockScreenEnabled = preferences.getBoolean(KEY_LOCK_SCREEN_ENABLED, false),
            studyScope = preferences.getString(KEY_STUDY_SCOPE, null)
                ?.let { stored -> StudyScope.entries.firstOrNull { it.name == stored } }
                ?: StudyScope.All,
            selectedSurahs = preferences.getStringSet(KEY_SELECTED_SURAHS, emptySet())
                .orEmpty()
                .mapNotNull(String::toIntOrNull)
                .filterTo(mutableSetOf()) { it in AVAILABLE_SURAHS },
            quizCorrectDays = decodeQuizCorrectDays(
                preferences.getStringSet(KEY_QUIZ_CORRECT_DAYS, emptySet()).orEmpty(),
            ),
            quizCorrectAnswers = preferences.getInt(KEY_QUIZ_CORRECT_ANSWERS, 0),
            quizTotalAnswers = preferences.getInt(KEY_QUIZ_TOTAL_ANSWERS, 0),
            lockScreenQuizEnabled = preferences.getBoolean(KEY_LOCK_SCREEN_QUIZ_ENABLED, false),
            lockScreenQuizInterval = preferences.getInt(KEY_LOCK_SCREEN_QUIZ_INTERVAL, 3)
                .coerceIn(1, 10),
            themeMode = preferences.getString(KEY_THEME_MODE, null)
                ?.let { stored -> AppThemeMode.entries.firstOrNull { it.name == stored } }
                ?: AppThemeMode.Auto,
            advancedSettingsVisible = preferences.getBoolean(
                KEY_ADVANCED_SETTINGS_VISIBLE,
                false,
            ),
            currentStudyWordId = preferences.getString(KEY_CURRENT_STUDY_WORD_ID, null)
                ?.takeIf { storedId -> WordRepository.words.any { it.id == storedId } },
            reviewSchedules = schedules,
            favoriteIds = preferences.getStringSet(KEY_FAVORITE_IDS, emptySet()).orEmpty(),
            customStudyIds = preferences.getStringSet(KEY_CUSTOM_STUDY_IDS, emptySet()).orEmpty(),
            onboardingComplete = preferences.getBoolean(
                KEY_ONBOARDING_COMPLETE,
                preferences.contains(KEY_STUDY_SCOPE) ||
                    preferences.contains(KEY_LEARNED) ||
                    preferences.contains(KEY_REVIEW_SCHEDULES),
            ),
            reviewEvents = ReviewEventCodec.decode(
                preferences.getStringSet(KEY_REVIEW_EVENTS, emptySet()).orEmpty(),
            ),
            quietHoursEnabled = preferences.getBoolean(KEY_QUIET_HOURS_ENABLED, true),
            quietStartHour = preferences.getInt(KEY_QUIET_START_HOUR, 22).coerceIn(0, 23),
            quietEndHour = preferences.getInt(KEY_QUIET_END_HOUR, 7).coerceIn(0, 23),
            lockScreenDailyLimit = preferences.getInt(KEY_LOCK_SCREEN_DAILY_LIMIT, 20)
                .coerceIn(0, 100),
            lockScreenCardsToday = lockCardsToday,
            lockScreenPausedUntil = preferences.getLong(KEY_LOCK_SCREEN_PAUSED_UNTIL, 0L)
                .takeIf { it > 0L }
                ?.let(Instant::ofEpochMilli),
        )
    }

    private fun refreshDayIfNeeded(progress: StudyProgress, date: LocalDate): StudyProgress {
        val storedDay = preferences.getString(KEY_TODAY, null)
        return if (storedDay == date.toString()) {
            progress
        } else {
            progress.copy(todayAnsweredIds = emptySet(), lockScreenCardsToday = 0)
        }
    }

    private fun persist(progress: StudyProgress, date: LocalDate) {
        preferences.edit {
            putStringSet(KEY_LEARNED, progress.learnedIds)
            putStringSet(KEY_REVIEWING, progress.reviewingIds)
            putStringSet(KEY_TODAY_ANSWERED, progress.todayAnsweredIds)
            putString(KEY_TODAY, date.toString())
            putInt(KEY_DAILY_GOAL, progress.dailyGoal)
            putInt(KEY_MAXIMUM_WORDS, progress.maximumWords)
            putInt(KEY_STREAK, progress.streakDays)
            putBoolean(KEY_REMINDER, progress.reminderEnabled)
            putBoolean(KEY_LOCK_SCREEN_ENABLED, progress.lockScreenEnabled)
            putString(KEY_STUDY_SCOPE, progress.studyScope.name)
            putStringSet(KEY_SELECTED_SURAHS, progress.selectedSurahs.map(Int::toString).toSet())
            putStringSet(KEY_QUIZ_CORRECT_DAYS, encodeQuizCorrectDays(progress.quizCorrectDays))
            putInt(KEY_QUIZ_CORRECT_ANSWERS, progress.quizCorrectAnswers)
            putInt(KEY_QUIZ_TOTAL_ANSWERS, progress.quizTotalAnswers)
            putBoolean(KEY_LOCK_SCREEN_QUIZ_ENABLED, progress.lockScreenQuizEnabled)
            putInt(KEY_LOCK_SCREEN_QUIZ_INTERVAL, progress.lockScreenQuizInterval)
            putString(KEY_THEME_MODE, progress.themeMode.name)
            putBoolean(KEY_ADVANCED_SETTINGS_VISIBLE, progress.advancedSettingsVisible)
            putStringSet(KEY_REVIEW_SCHEDULES, ReviewScheduleCodec.encode(progress.reviewSchedules))
            putStringSet(KEY_FAVORITE_IDS, progress.favoriteIds)
            putStringSet(KEY_CUSTOM_STUDY_IDS, progress.customStudyIds)
            putBoolean(KEY_ONBOARDING_COMPLETE, progress.onboardingComplete)
            putStringSet(KEY_REVIEW_EVENTS, ReviewEventCodec.encode(progress.reviewEvents))
            putBoolean(KEY_QUIET_HOURS_ENABLED, progress.quietHoursEnabled)
            putInt(KEY_QUIET_START_HOUR, progress.quietStartHour)
            putInt(KEY_QUIET_END_HOUR, progress.quietEndHour)
            putInt(KEY_LOCK_SCREEN_DAILY_LIMIT, progress.lockScreenDailyLimit)
            putInt(KEY_LOCK_SCREEN_CARDS_TODAY, progress.lockScreenCardsToday)
            putString(KEY_LOCK_SCREEN_COUNT_DATE, date.toString())
            progress.lockScreenPausedUntil?.let {
                putLong(KEY_LOCK_SCREEN_PAUSED_UNTIL, it.toEpochMilli())
            } ?: remove(KEY_LOCK_SCREEN_PAUSED_UNTIL)
            progress.currentStudyWordId?.let {
                putString(KEY_CURRENT_STUDY_WORD_ID, it)
            } ?: remove(KEY_CURRENT_STUDY_WORD_ID)
        }
        _progress.value = progress
    }

    private fun decodeQuizCorrectDays(entries: Set<String>): Map<String, Set<String>> =
        entries.mapNotNull { entry ->
            val separator = entry.indexOf('|')
            if (separator <= 0) return@mapNotNull null
            val id = entry.substring(0, separator)
            val days = entry.substring(separator + 1)
                .split(',')
                .filter(String::isNotBlank)
                .toSet()
            id to days
        }.toMap()

    private fun encodeQuizCorrectDays(daysByWord: Map<String, Set<String>>): Set<String> =
        daysByWord.mapTo(mutableSetOf()) { (id, days) ->
            "$id|${days.sorted().joinToString(",")}"
        }

    private fun statusSets(
        schedules: Map<String, ReviewSchedule>,
    ): Pair<Set<String>, Set<String>> {
        val learned = schedules.filterValues { it.isLearned }.keys
        return learned to (schedules.keys - learned)
    }

    companion object {
        private const val PREFERENCES = "kalima_progress"
        private const val KEY_LEARNED = "learned"
        private const val KEY_REVIEWING = "reviewing"
        private const val KEY_TODAY_ANSWERED = "today_answered"
        private const val KEY_TODAY = "today"
        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val KEY_MAXIMUM_WORDS = "maximum_words"
        private const val KEY_STREAK = "streak"
        private const val KEY_REMINDER = "reminder"
        private const val KEY_LOCK_SCREEN_ENABLED = "lock_screen_enabled"
        private const val KEY_LOCK_SCREEN_SEQUENCE = "lock_screen_sequence"
        private const val KEY_LAST_STUDY_DATE = "last_study_date"
        private const val KEY_STUDY_SCOPE = "study_scope"
        private const val KEY_SELECTED_SURAHS = "selected_surahs"
        private const val KEY_QUIZ_CORRECT_DAYS = "quiz_correct_days"
        private const val KEY_QUIZ_CORRECT_ANSWERS = "quiz_correct_answers"
        private const val KEY_QUIZ_TOTAL_ANSWERS = "quiz_total_answers"
        private const val KEY_LOCK_SCREEN_QUIZ_ENABLED = "lock_screen_quiz_enabled"
        private const val KEY_LOCK_SCREEN_QUIZ_INTERVAL = "lock_screen_quiz_interval"
        private const val KEY_LOCK_SCREEN_WORDS_SINCE_QUIZ = "lock_screen_words_since_quiz"
        private const val KEY_LOCK_SCREEN_QUIZ_SEQUENCE = "lock_screen_quiz_sequence"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ADVANCED_SETTINGS_VISIBLE = "advanced_settings_visible"
        private const val KEY_CURRENT_STUDY_WORD_ID = "current_study_word_id"
        private const val KEY_REVIEW_SCHEDULES = "review_schedules_v1"
        private const val KEY_FAVORITE_IDS = "favorite_ids"
        private const val KEY_CUSTOM_STUDY_IDS = "custom_study_ids"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_REVIEW_EVENTS = "review_events_v1"
        private const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
        private const val KEY_QUIET_START_HOUR = "quiet_start_hour"
        private const val KEY_QUIET_END_HOUR = "quiet_end_hour"
        private const val KEY_LOCK_SCREEN_DAILY_LIMIT = "lock_screen_daily_limit"
        private const val KEY_LOCK_SCREEN_CARDS_TODAY = "lock_screen_cards_today"
        private const val KEY_LOCK_SCREEN_COUNT_DATE = "lock_screen_count_date"
        private const val KEY_LOCK_SCREEN_PAUSED_UNTIL = "lock_screen_paused_until"
        private const val DEFAULT_SURAH = 114
        private val AVAILABLE_SURAHS = 1..114
    }
}
