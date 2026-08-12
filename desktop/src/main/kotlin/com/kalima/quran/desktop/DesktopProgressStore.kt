package com.kalima.quran.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kalima.quran.data.AppThemeMode
import com.kalima.quran.data.LearningWordLimiter
import com.kalima.quran.data.ReviewEvent
import com.kalima.quran.data.ReviewEventCodec
import com.kalima.quran.data.ReviewGrade
import com.kalima.quran.data.ReviewHistory
import com.kalima.quran.data.ReviewSchedule
import com.kalima.quran.data.ReviewScheduleCodec
import com.kalima.quran.data.ReviewSource
import com.kalima.quran.data.SpacedRepetition
import com.kalima.quran.data.StreakCalculator
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.WordRepository
import com.kalima.quran.localization.AppLanguage
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.LocalDate
import java.util.Properties

class DesktopProgressStore(
    val dataDirectory: Path = defaultDataDirectory(),
) {
    private val dataFile = dataDirectory.resolve("progress.properties")
    private val properties = Properties()

    var language by mutableStateOf(AppLanguage.Portuguese)
        private set
    var reminderHour by mutableIntStateOf(8)
        private set
    var reminderMinute by mutableIntStateOf(0)
        private set
    var progress by mutableStateOf(StudyProgress(onboardingComplete = false))
        private set

    init {
        Files.createDirectories(dataDirectory)
        if (Files.isRegularFile(dataFile)) {
            Files.newInputStream(dataFile).use(properties::load)
        }
        language = properties.getProperty(KEY_LANGUAGE)
            ?.let { stored -> AppLanguage.entries.firstOrNull { it.languageTag == stored } }
            ?: defaultLanguage()
        reminderHour = properties.int(KEY_REMINDER_HOUR, 8).coerceIn(0, 23)
        reminderMinute = properties.int(KEY_REMINDER_MINUTE, 0).coerceIn(0, 59)
        reloadCorpus()
        progress = loadProgress()
    }

    fun answer(wordId: String, remembered: Boolean) =
        recordAnswer(wordId, remembered, ReviewSource.Study, quiz = false)

    fun answerQuiz(wordId: String, correct: Boolean) =
        recordAnswer(wordId, correct, ReviewSource.Quiz, quiz = true)

    private fun recordAnswer(
        wordId: String,
        correct: Boolean,
        source: ReviewSource,
        quiz: Boolean,
    ) {
        if (WordRepository.words.none { it.id == wordId }) return
        val moment = Instant.now()
        val date = LocalDate.now()
        val previous = refreshDay(progress, date)
        val wasNew = wordId !in previous.reviewSchedules
        val schedules = previous.reviewSchedules + (
            wordId to SpacedRepetition.review(
                previous.reviewSchedules[wordId],
                if (correct) ReviewGrade.Good else ReviewGrade.Again,
                moment,
            )
        )
        val (learnedIds, reviewingIds) = statusSets(schedules)
        val lastStudyDate = properties.getProperty(KEY_LAST_STUDY_DATE)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val correctDays = if (quiz && correct) {
            previous.quizCorrectDays + (
                wordId to (previous.quizCorrectDays[wordId].orEmpty() + date.toString())
            )
        } else {
            previous.quizCorrectDays
        }
        progress = previous.copy(
            learnedIds = learnedIds,
            reviewingIds = reviewingIds,
            todayAnsweredIds = previous.todayAnsweredIds + wordId,
            streakDays = StreakCalculator.next(previous.streakDays, lastStudyDate, date),
            quizCorrectAnswers = previous.quizCorrectAnswers + if (quiz && correct) 1 else 0,
            quizTotalAnswers = previous.quizTotalAnswers + if (quiz) 1 else 0,
            quizCorrectDays = correctDays,
            reviewSchedules = schedules,
            reviewEvents = ReviewHistory.append(
                previous.reviewEvents,
                ReviewEvent(moment, wordId, correct, wasNew, source),
            ),
        )
        properties.setProperty(KEY_LAST_STUDY_DATE, date.toString())
        persist()
    }

    fun completeOnboarding(scope: StudyScope, dailyGoal: Int) {
        update {
            it.copy(
                onboardingComplete = true,
                studyScope = scope,
                dailyGoal = dailyGoal.coerceIn(3, 20),
            )
        }
    }

    fun setStudyScope(scope: StudyScope) {
        update {
            it.copy(
                studyScope = scope,
                selectedSurahs = if (scope == StudyScope.Surahs && it.selectedSurahs.isEmpty()) {
                    setOf(114)
                } else {
                    it.selectedSurahs
                },
            )
        }
    }

    fun toggleSurah(number: Int) {
        if (number !in 1..114) return
        update { current ->
            val selected = current.selectedSurahs.toMutableSet().apply {
                if (!add(number)) remove(number)
            }
            if (selected.isEmpty()) {
                current.copy(studyScope = StudyScope.All, selectedSurahs = emptySet())
            } else {
                current.copy(studyScope = StudyScope.Surahs, selectedSurahs = selected)
            }
        }
    }

    fun toggleFavorite(wordId: String) = update { current ->
        current.copy(favoriteIds = current.favoriteIds.toggled(wordId))
    }

    fun toggleCustomStudy(wordId: String) = update { current ->
        current.copy(customStudyIds = current.customStudyIds.toggled(wordId))
    }

    fun setDailyGoal(value: Int) = update { it.copy(dailyGoal = value.coerceIn(3, 20)) }

    fun setMaximumWords(value: Int) = update {
        val normalized = if (value == LearningWordLimiter.UNLIMITED) {
            LearningWordLimiter.UNLIMITED
        } else {
            value.coerceIn(LearningWordLimiter.MINIMUM_LIMIT, WordRepository.words.size)
        }
        it.copy(maximumWords = normalized)
    }

    fun setThemeMode(value: AppThemeMode) = update { it.copy(themeMode = value) }

    fun setReminderEnabled(enabled: Boolean) = update { it.copy(reminderEnabled = enabled) }

    fun setReminderTime(hour: Int, minute: Int) {
        reminderHour = hour.coerceIn(0, 23)
        reminderMinute = minute.coerceIn(0, 59)
        persist()
    }

    fun changeLanguage(value: AppLanguage) {
        if (language == value) return
        language = value
        reloadCorpus()
        persist()
    }

    fun setCurrentStudyWord(wordId: String?) = update { current ->
        current.copy(currentStudyWordId = wordId?.takeIf { id -> WordRepository.words.any { it.id == id } })
    }

    fun resetProgress() {
        progress = StudyProgress(
            dailyGoal = progress.dailyGoal,
            studyScope = progress.studyScope,
            selectedSurahs = progress.selectedSurahs,
            themeMode = progress.themeMode,
            maximumWords = progress.maximumWords,
            reminderEnabled = progress.reminderEnabled,
            onboardingComplete = true,
        )
        properties.remove(KEY_LAST_STUDY_DATE)
        persist()
    }

    private fun update(transform: (StudyProgress) -> StudyProgress) {
        progress = transform(refreshDay(progress, LocalDate.now()))
        persist()
    }

    private fun reloadCorpus() {
        val stream = requireNotNull(javaClass.getResourceAsStream("/quran_vocabulary.tsv.gz")) {
            "Corpus offline nao encontrado"
        }
        WordRepository.initialize(stream, language)
    }

    private fun loadProgress(): StudyProgress {
        val today = LocalDate.now()
        val schedules = ReviewScheduleCodec.decode(properties.set(KEY_REVIEW_SCHEDULES))
        val (learnedIds, reviewingIds) = statusSets(schedules)
        val storedDay = properties.getProperty(KEY_TODAY)
        return StudyProgress(
            learnedIds = learnedIds,
            reviewingIds = reviewingIds,
            todayAnsweredIds = if (storedDay == today.toString()) properties.set(KEY_TODAY_ANSWERED) else emptySet(),
            dailyGoal = properties.int(KEY_DAILY_GOAL, 5).coerceIn(3, 20),
            maximumWords = properties.int(KEY_MAXIMUM_WORDS, LearningWordLimiter.UNLIMITED).let {
                if (it == LearningWordLimiter.UNLIMITED) it else it.coerceIn(1, WordRepository.words.size)
            },
            streakDays = properties.int(KEY_STREAK, 0).coerceAtLeast(0),
            reminderEnabled = properties.boolean(KEY_REMINDER, false),
            studyScope = properties.getProperty(KEY_STUDY_SCOPE)
                ?.let { stored -> StudyScope.entries.firstOrNull { it.name == stored } }
                ?: StudyScope.Frequent,
            selectedSurahs = properties.set(KEY_SELECTED_SURAHS)
                .mapNotNull(String::toIntOrNull).filterTo(mutableSetOf()) { it in 1..114 },
            quizCorrectDays = decodeCorrectDays(properties.set(KEY_QUIZ_CORRECT_DAYS)),
            quizCorrectAnswers = properties.int(KEY_QUIZ_CORRECT_ANSWERS, 0).coerceAtLeast(0),
            quizTotalAnswers = properties.int(KEY_QUIZ_TOTAL_ANSWERS, 0).coerceAtLeast(0),
            themeMode = properties.getProperty(KEY_THEME_MODE)
                ?.let { stored -> AppThemeMode.entries.firstOrNull { it.name == stored } }
                ?: AppThemeMode.Auto,
            currentStudyWordId = properties.getProperty(KEY_CURRENT_STUDY_WORD_ID)
                ?.takeIf { id -> WordRepository.words.any { it.id == id } },
            reviewSchedules = schedules,
            favoriteIds = properties.set(KEY_FAVORITE_IDS),
            customStudyIds = properties.set(KEY_CUSTOM_STUDY_IDS),
            onboardingComplete = properties.boolean(KEY_ONBOARDING_COMPLETE, false),
            reviewEvents = ReviewEventCodec.decode(properties.set(KEY_REVIEW_EVENTS)),
        )
    }

    private fun persist() {
        val date = LocalDate.now()
        val current = refreshDay(progress, date)
        progress = current
        properties.apply {
            setProperty(KEY_LANGUAGE, language.languageTag)
            setProperty(KEY_REMINDER_HOUR, reminderHour.toString())
            setProperty(KEY_REMINDER_MINUTE, reminderMinute.toString())
            setProperty(KEY_TODAY, date.toString())
            setProperty(KEY_TODAY_ANSWERED, current.todayAnsweredIds.encoded())
            setProperty(KEY_DAILY_GOAL, current.dailyGoal.toString())
            setProperty(KEY_MAXIMUM_WORDS, current.maximumWords.toString())
            setProperty(KEY_STREAK, current.streakDays.toString())
            setProperty(KEY_REMINDER, current.reminderEnabled.toString())
            setProperty(KEY_STUDY_SCOPE, current.studyScope.name)
            setProperty(KEY_SELECTED_SURAHS, current.selectedSurahs.map(Int::toString).toSet().encoded())
            setProperty(KEY_QUIZ_CORRECT_DAYS, encodeCorrectDays(current.quizCorrectDays).encoded())
            setProperty(KEY_QUIZ_CORRECT_ANSWERS, current.quizCorrectAnswers.toString())
            setProperty(KEY_QUIZ_TOTAL_ANSWERS, current.quizTotalAnswers.toString())
            setProperty(KEY_THEME_MODE, current.themeMode.name)
            setProperty(KEY_REVIEW_SCHEDULES, ReviewScheduleCodec.encode(current.reviewSchedules).encoded())
            setProperty(KEY_FAVORITE_IDS, current.favoriteIds.encoded())
            setProperty(KEY_CUSTOM_STUDY_IDS, current.customStudyIds.encoded())
            setProperty(KEY_ONBOARDING_COMPLETE, current.onboardingComplete.toString())
            setProperty(KEY_REVIEW_EVENTS, ReviewEventCodec.encode(current.reviewEvents).encoded())
            current.currentStudyWordId?.let { setProperty(KEY_CURRENT_STUDY_WORD_ID, it) }
                ?: remove(KEY_CURRENT_STUDY_WORD_ID)
        }
        Files.createDirectories(dataDirectory)
        val temporary = dataDirectory.resolve("progress.properties.tmp")
        Files.newOutputStream(temporary).use { properties.store(it, "Kalima desktop progress") }
        try {
            Files.move(
                temporary,
                dataFile,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary, dataFile, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun refreshDay(current: StudyProgress, date: LocalDate): StudyProgress =
        if (properties.getProperty(KEY_TODAY) == date.toString()) current
        else current.copy(todayAnsweredIds = emptySet())

    private fun statusSets(
        schedules: Map<String, ReviewSchedule>,
    ): Pair<Set<String>, Set<String>> {
        val learned = schedules.filterValues(ReviewSchedule::isLearned).keys
        return learned to (schedules.keys - learned)
    }

    private fun decodeCorrectDays(entries: Set<String>): Map<String, Set<String>> =
        entries.mapNotNull { entry ->
            val separator = entry.indexOf('|')
            if (separator <= 0) return@mapNotNull null
            entry.substring(0, separator) to entry.substring(separator + 1)
                .split(',').filter(String::isNotBlank).toSet()
        }.toMap()

    private fun encodeCorrectDays(value: Map<String, Set<String>>): Set<String> =
        value.mapTo(mutableSetOf()) { (id, days) -> "$id|${days.sorted().joinToString(",")}" }

    private fun Set<String>.toggled(value: String): Set<String> = toMutableSet().apply {
        if (!add(value)) remove(value)
    }

    private fun Set<String>.encoded(): String = sorted().joinToString(ENTRY_SEPARATOR)

    private fun Properties.set(key: String): Set<String> = getProperty(key)
        .orEmpty().split(ENTRY_SEPARATOR).filter(String::isNotBlank).toSet()

    private fun Properties.int(key: String, default: Int): Int =
        getProperty(key)?.toIntOrNull() ?: default

    private fun Properties.boolean(key: String, default: Boolean): Boolean =
        getProperty(key)?.toBooleanStrictOrNull() ?: default

    companion object {
        private const val ENTRY_SEPARATOR = "\u001E"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_TODAY = "today"
        private const val KEY_TODAY_ANSWERED = "today_answered"
        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val KEY_MAXIMUM_WORDS = "maximum_words"
        private const val KEY_STREAK = "streak"
        private const val KEY_REMINDER = "reminder"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
        private const val KEY_LAST_STUDY_DATE = "last_study_date"
        private const val KEY_STUDY_SCOPE = "study_scope"
        private const val KEY_SELECTED_SURAHS = "selected_surahs"
        private const val KEY_QUIZ_CORRECT_DAYS = "quiz_correct_days"
        private const val KEY_QUIZ_CORRECT_ANSWERS = "quiz_correct_answers"
        private const val KEY_QUIZ_TOTAL_ANSWERS = "quiz_total_answers"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_CURRENT_STUDY_WORD_ID = "current_study_word_id"
        private const val KEY_REVIEW_SCHEDULES = "review_schedules_v1"
        private const val KEY_FAVORITE_IDS = "favorite_ids"
        private const val KEY_CUSTOM_STUDY_IDS = "custom_study_ids"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_REVIEW_EVENTS = "review_events_v1"

        fun defaultDataDirectory(): Path {
            System.getenv("KALIMA_DATA_DIR")?.takeIf(String::isNotBlank)?.let(Path::of)?.let { return it }
            val base = System.getenv("APPDATA")?.takeIf(String::isNotBlank)
                ?: System.getProperty("user.home")
            return Path.of(base, "Kalima")
        }

        private fun defaultLanguage(): AppLanguage =
            if (System.getProperty("user.language").equals("pt", ignoreCase = true)) {
                AppLanguage.Portuguese
            } else {
                AppLanguage.English
            }
    }
}
