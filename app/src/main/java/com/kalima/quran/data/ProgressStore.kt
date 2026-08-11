package com.kalima.quran.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.kalima.quran.quiz.LockScreenContent
import com.kalima.quran.quiz.LockScreenQuizSchedule
import com.kalima.quran.quiz.QuizEngine
import com.kalima.quran.quiz.QuizMastery
import java.time.LocalDate

const val QUIZ_MASTERY_DAYS = 3

data class StudyProgress(
    val learnedIds: Set<String> = emptySet(),
    val reviewingIds: Set<String> = emptySet(),
    val todayAnsweredIds: Set<String> = emptySet(),
    val dailyGoal: Int = 5,
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
) {
    val todayCompleted: Int get() = todayAnsweredIds.size

    fun statusFor(id: String): WordStatus = when (id) {
        in learnedIds -> WordStatus.Learned
        in reviewingIds -> WordStatus.Reviewing
        else -> WordStatus.New
    }

    fun quizCorrectDayCount(id: String): Int = quizCorrectDays[id].orEmpty().size
}

class ProgressStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    init {
        WordRepository.initialize(context.applicationContext)
    }

    private val today = { LocalDate.now() }
    private val _progress = MutableStateFlow(load())
    val progress: StateFlow<StudyProgress> = _progress.asStateFlow()

    fun answer(wordId: String, learned: Boolean) {
        val date = today()
        val previous = refreshDayIfNeeded(_progress.value, date)
        val learnedIds = previous.learnedIds.toMutableSet()
        val reviewingIds = previous.reviewingIds.toMutableSet()

        if (learned) {
            learnedIds += wordId
            reviewingIds -= wordId
        } else {
            reviewingIds += wordId
            learnedIds -= wordId
        }

        val answeredToday = previous.todayAnsweredIds + wordId
        val lastStudyDate = preferences.getString(KEY_LAST_STUDY_DATE, null)
            ?.let(LocalDate::parse)
        val updated = previous.copy(
            learnedIds = learnedIds,
            reviewingIds = reviewingIds,
            todayAnsweredIds = answeredToday,
            streakDays = StreakCalculator.next(previous.streakDays, lastStudyDate, date),
        )
        preferences.edit { putString(KEY_LAST_STUDY_DATE, date.toString()) }
        persist(updated, date)
    }

    fun answerQuiz(wordId: String, correct: Boolean) {
        val date = today()
        val previous = refreshDayIfNeeded(_progress.value, date)
        val learnedIds = previous.learnedIds.toMutableSet()
        val reviewingIds = previous.reviewingIds.toMutableSet()
        val correctDays = previous.quizCorrectDays
            .mapValuesTo(mutableMapOf()) { (_, days) -> days.toMutableSet() }

        if (correct) {
            val wordDays = QuizMastery.recordCorrectDay(correctDays[wordId].orEmpty(), date)
            correctDays[wordId] = wordDays.toMutableSet()
            if (QuizMastery.isMastered(wordDays)) {
                learnedIds += wordId
                reviewingIds -= wordId
            } else {
                reviewingIds += wordId
                learnedIds -= wordId
            }
        } else {
            reviewingIds += wordId
            learnedIds -= wordId
        }

        val lastStudyDate = preferences.getString(KEY_LAST_STUDY_DATE, null)
            ?.let(LocalDate::parse)
        val updated = previous.copy(
            learnedIds = learnedIds,
            reviewingIds = reviewingIds,
            todayAnsweredIds = previous.todayAnsweredIds + wordId,
            streakDays = StreakCalculator.next(previous.streakDays, lastStudyDate, date),
            quizCorrectDays = correctDays,
            quizCorrectAnswers = previous.quizCorrectAnswers + if (correct) 1 else 0,
            quizTotalAnswers = previous.quizTotalAnswers + 1,
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

    fun nextLockScreenContent(): LockScreenContent {
        val current = _progress.value
        val source = WordRepository.wordsFor(current.studyScope, current.selectedSurahs)
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
                    statusFor = current::statusFor,
                    sequence = quizSequence,
                )
            }.getOrNull()
            if (question != null) {
                preferences.edit {
                    putInt(KEY_LOCK_SCREEN_QUIZ_SEQUENCE, quizSequence + 1)
                    putInt(KEY_LOCK_SCREEN_WORDS_SINCE_QUIZ, 0)
                }
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
        return LockScreenContent.WordCard(word)
    }

    fun setDailyGoal(goal: Int) {
        val updated = _progress.value.copy(dailyGoal = goal.coerceIn(3, 20))
        persist(updated, today())
    }

    private fun load(): StudyProgress {
        val date = today()
        val storedDay = preferences.getString(KEY_TODAY, null)
        val answered = if (storedDay == date.toString()) {
            preferences.getStringSet(KEY_TODAY_ANSWERED, emptySet()).orEmpty()
        } else {
            emptySet()
        }
        return StudyProgress(
            learnedIds = preferences.getStringSet(KEY_LEARNED, emptySet()).orEmpty(),
            reviewingIds = preferences.getStringSet(KEY_REVIEWING, emptySet()).orEmpty(),
            todayAnsweredIds = answered,
            dailyGoal = preferences.getInt(KEY_DAILY_GOAL, 5),
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
        )
    }

    private fun refreshDayIfNeeded(progress: StudyProgress, date: LocalDate): StudyProgress {
        val storedDay = preferences.getString(KEY_TODAY, null)
        return if (storedDay == date.toString()) progress else progress.copy(todayAnsweredIds = emptySet())
    }

    private fun persist(progress: StudyProgress, date: LocalDate) {
        preferences.edit {
            putStringSet(KEY_LEARNED, progress.learnedIds)
            putStringSet(KEY_REVIEWING, progress.reviewingIds)
            putStringSet(KEY_TODAY_ANSWERED, progress.todayAnsweredIds)
            putString(KEY_TODAY, date.toString())
            putInt(KEY_DAILY_GOAL, progress.dailyGoal)
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

    companion object {
        private const val PREFERENCES = "kalima_progress"
        private const val KEY_LEARNED = "learned"
        private const val KEY_REVIEWING = "reviewing"
        private const val KEY_TODAY_ANSWERED = "today_answered"
        private const val KEY_TODAY = "today"
        private const val KEY_DAILY_GOAL = "daily_goal"
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
        private const val DEFAULT_SURAH = 114
        private val AVAILABLE_SURAHS = 1..114
    }
}
