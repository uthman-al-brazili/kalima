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
    val newWordReady: Boolean,
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
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): DailyMissionState {
    val today = now.atZone(zoneId).toLocalDate()
    val answeredWordIds = progress.reviewEvents
        .asSequence()
        .filter { event -> event.timestamp.atZone(zoneId).toLocalDate() == today }
        .mapTo(mutableSetOf()) { event -> event.wordId }
    val completedWords = answeredWordIds.size
    val goalWords = progress.dailyGoal
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
        remainingWords = (goalWords - completedWords).coerceAtLeast(0),
        dueReviews = progress.dueReviewCount(
            availableWords.mapTo(mutableSetOf(), QuranWord::id),
            now,
        ),
        newWordReady = availableWords.any { progress.statusFor(it.id) == WordStatus.New },
        goalComplete = completedWords >= goalWords,
        activity = activity,
    )
}

internal fun shouldShowDailyMissionCompletion(
    sessionStartedAtCount: Int,
    dailyGoal: Int,
    answeredBeforeAction: Set<String>,
    completedWordId: String,
): Boolean =
    sessionStartedAtCount < dailyGoal &&
        answeredBeforeAction.size < dailyGoal &&
        completedWordId !in answeredBeforeAction &&
        (answeredBeforeAction + completedWordId).size >= dailyGoal

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
