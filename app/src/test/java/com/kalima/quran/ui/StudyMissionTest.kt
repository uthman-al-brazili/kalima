package com.kalima.quran.ui

import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.ReviewEvent
import com.kalima.quran.data.ReviewSource
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.VerseToken
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyMissionTest {
    private val now = Instant.parse("2026-08-27T12:00:00Z")
    private val zone = ZoneId.of("UTC")

    @Test
    fun `mission activity shows the latest seven local days`() {
        val todayWord = word("today")
        val yesterdayWord = word("yesterday")
        val activity = buildMissionActivity(
            progress = StudyProgress(
                reviewEvents = listOf(
                    ReviewEvent(now, todayWord.id, true, false, ReviewSource.Study),
                    ReviewEvent(
                        now.minusSeconds(86_400),
                        yesterdayWord.id,
                        true,
                        false,
                        ReviewSource.Study,
                    ),
                ),
            ),
            now = now,
            zoneId = zone,
        )

        assertEquals(7, activity.size)
        assertEquals(LocalDate.of(2026, 8, 27), activity.last().date)
        assertTrue(activity.last().isToday)
        assertEquals(1, activity.last().completedReviews)
        assertEquals(1, activity[5].completedReviews)
    }

    @Test
    fun `completion payoff chooses the reviewed ayah with the most recognized words`() {
        val first = word("first")
        val second = word("second")
        val extra = word("extra")
        val payoff = buildStudyCompletionPayoff(
            reviewedWords = listOf(first, second, first),
            recognizedWordIds = setOf(first.id, second.id, extra.id),
            tokensFor = { word ->
                if (word.id == first.id) {
                    listOf(VerseToken(0, first.arabic, first), VerseToken(1, "unknown", null))
                } else {
                    listOf(
                        VerseToken(0, second.arabic, second),
                        VerseToken(1, extra.arabic, extra),
                        VerseToken(2, extra.arabic, extra),
                    )
                }
            },
        )

        assertEquals(listOf(first.id, second.id), payoff.reviewedWords.map(QuranWord::id))
        assertEquals(second.id, payoff.featuredWord.id)
        assertEquals(3, payoff.recognizedWordCount)
    }

    @Test
    fun `guided empty plan stays on mission while routed launches bypass it`() {
        assertTrue(shouldShowDailyMission(true, true, false, false, false))
        assertTrue(shouldShowDailyMission(false, true, false, false, false))
        assertFalse(shouldShowDailyMission(false, false, false, false, false))
        assertFalse(shouldShowDailyMission(false, true, false, true, false))
        assertFalse(shouldShowDailyMission(false, true, false, false, true))
    }

    private fun word(id: String) = QuranWord(
        id = id,
        arabic = id,
        lemma = id,
        transliteration = id,
        meaning = id,
        root = id,
        grammar = "noun",
        category = "test",
        reference = "Test 1:1",
        verseArabic = "$id $id",
        verseMeaning = "test verse",
        insight = "",
    )
}
