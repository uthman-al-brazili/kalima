package com.kalima.quran.ui

import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.ReviewHistory
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.VerseToken
import com.kalima.quran.data.WordStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal data class DailyMissionActivity(
    val date: LocalDate,
    val completedReviews: Int,
    val isToday: Boolean,
)

internal data class DailyMissionState(
    val answeredWordIds: Set<String>,
    val completedWords: Int,
    val goalWords: Int,
    val remainingWords: Int,
    val dueReviews: Int,
    val newWordsReady: Int,
    val goalComplete: Boolean,
    val activity: List<DailyMissionActivity>,
)

internal data class StudyCompletionPayoff(
    val reviewedWords: List<QuranWord>,
    val featuredWord: QuranWord,
    val recognizedWordCount: Int,
)

internal fun buildDailyMissionState(
    progress: StudyProgress,
    availableWords: List<QuranWord>,
    missionWords: List<QuranWord> = availableWords,
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
    restrictAnswersToAvailableWords: Boolean = false,
): DailyMissionState {
    val today = now.atZone(zoneId).toLocalDate()
    val availableWordIds = if (restrictAnswersToAvailableWords) {
        availableWords.mapTo(mutableSetOf(), QuranWord::id)
    } else {
        null
    }
    val answeredWordIds = progress.reviewEvents
        .asSequence()
        .filter { event -> event.timestamp.atZone(zoneId).toLocalDate() == today }
        .filter { event -> availableWordIds == null || event.wordId in availableWordIds }
        .mapTo(mutableSetOf()) { event -> event.wordId }
    val completedWords = answeredWordIds.size
    val queuedWordCount = missionWords
        .asSequence()
        .map(QuranWord::id)
        .distinct()
        .count { wordId -> wordId !in answeredWordIds }
    val achievableGoalWords = completedWords + queuedWordCount
    val goalWords = if (achievableGoalWords == 0) {
        progress.dailyGoal
    } else {
        minOf(progress.dailyGoal, achievableGoalWords)
    }
    val remainingWords = (goalWords - completedWords).coerceAtLeast(0)
    val newWordsReady = availableWords
        .asSequence()
        .filter { word -> progress.statusFor(word.id) == WordStatus.New }
        .map(QuranWord::id)
        .distinct()
        .count()
        .coerceAtMost(remainingWords)
    val countsByDay = ReviewHistory.countByDay(progress.reviewEvents, zoneId)
    val activity = (6L downTo 0L).map { daysAgo ->
        val date = today.minusDays(daysAgo)
        DailyMissionActivity(
            date = date,
            completedReviews = countsByDay[date] ?: 0,
            isToday = date == today,
        )
    }
    return DailyMissionState(
        answeredWordIds = answeredWordIds,
        completedWords = completedWords,
        goalWords = goalWords,
        remainingWords = remainingWords,
        dueReviews = progress.dueReviewCount(
            availableWords.mapTo(mutableSetOf(), QuranWord::id),
            now,
        ),
        newWordsReady = newWordsReady,
        goalComplete = completedWords >= goalWords,
        activity = activity,
    )
}

internal fun missionActionWordCount(
    mission: DailyMissionState,
    queuedWordCount: Int,
): Int = minOf(mission.remainingWords, queuedWordCount.coerceAtLeast(0))

internal fun shouldOpenContextCheckpoint(
    mission: DailyMissionState,
    wordId: String,
    dailyGoalWasIncompleteAtMissionStart: Boolean,
    wordCompletionAlreadyRecorded: Boolean,
): Boolean {
    if (!dailyGoalWasIncompleteAtMissionStart) return false
    if (wordCompletionAlreadyRecorded) return mission.goalComplete
    if (mission.goalComplete || wordId in mission.answeredWordIds) return false
    return mission.completedWords + 1 >= mission.goalWords
}

internal fun buildStudyCompletionPayoff(
    reviewedWords: List<QuranWord>,
    recognizedWordIds: Set<String>,
    tokensFor: (QuranWord) -> List<VerseToken>,
): StudyCompletionPayoff {
    val uniqueReviewedWords = reviewedWords.distinctBy(QuranWord::id)
    require(uniqueReviewedWords.isNotEmpty()) { "A completion payoff needs at least one reviewed word." }
    val scoredWords = uniqueReviewedWords.map { word ->
        word to tokensFor(word).count { token -> token.word?.id in recognizedWordIds }
    }
    val (featuredWord, recognizedWordCount) = scoredWords.maxBy { it.second }
    return StudyCompletionPayoff(
        reviewedWords = uniqueReviewedWords,
        featuredWord = featuredWord,
        recognizedWordCount = recognizedWordCount,
    )
}
