package com.kalima.quran.ui

import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.ReviewHistory
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.VerseToken
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal data class DailyMissionActivity(
    val date: LocalDate,
    val completedReviews: Int,
    val isToday: Boolean,
)

internal data class StudyCompletionPayoff(
    val reviewedWords: List<QuranWord>,
    val featuredWord: QuranWord,
    val recognizedWordCount: Int,
)

internal fun buildMissionActivity(
    progress: StudyProgress,
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<DailyMissionActivity> {
    val today = now.atZone(zoneId).toLocalDate()
    val countsByDay = ReviewHistory.countByDay(progress.reviewEvents, zoneId)
    return (6L downTo 0L).map { daysAgo ->
        val date = today.minusDays(daysAgo)
        DailyMissionActivity(
            date = date,
            completedReviews = countsByDay[date] ?: 0,
            isToday = date == today,
        )
    }
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
