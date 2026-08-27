package com.kalima.quran.ui

import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.ReviewEvent
import com.kalima.quran.data.ReviewSchedule
import com.kalima.quran.data.ReviewSource
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.VerseToken
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyMissionTest {
    private val now = Instant.parse("2026-08-27T12:00:00Z")
    private val zone = ZoneId.of("UTC")

    @Test
    fun `daily mission combines goal due new and seven day activity`() {
        val completed = word("completed")
        val due = word("due")
        val fresh = word("fresh")
        val yesterday = now.minusSeconds(86_400)
        val progress = StudyProgress(
            todayAnsweredIds = setOf(completed.id),
            dailyGoal = 3,
            reviewingIds = setOf(completed.id, due.id),
            reviewSchedules = mapOf(
                due.id to ReviewSchedule(dueAt = now, lastReviewedAt = yesterday),
            ),
            reviewEvents = listOf(
                ReviewEvent(yesterday, completed.id, true, false, ReviewSource.Study),
                ReviewEvent(now, completed.id, true, false, ReviewSource.Study),
            ),
        )

        val mission = buildDailyMissionState(
            progress = progress,
            availableWords = listOf(completed, due, fresh),
            now = now,
            zoneId = zone,
        )

        assertEquals(1, mission.completedWords)
        assertEquals(3, mission.goalWords)
        assertEquals(2, mission.remainingWords)
        assertEquals(1, mission.dueReviews)
        assertTrue(mission.newWordReady)
        assertFalse(mission.goalComplete)
        assertEquals(7, mission.activity.size)
        assertEquals(LocalDate.of(2026, 8, 27), mission.activity.last().date)
        assertTrue(mission.activity.last().isToday)
        assertEquals(1, mission.activity[5].completedReviews)
    }

    @Test
    fun `daily mission excludes answered ids from the prior local day`() {
        val stale = word("stale")
        val progress = StudyProgress(
            todayAnsweredIds = setOf(stale.id),
            dailyGoal = 3,
            reviewEvents = listOf(
                ReviewEvent(now.minusSeconds(86_400), stale.id, true, false, ReviewSource.Study),
            ),
        )

        val mission = buildDailyMissionState(
            progress = progress,
            availableWords = listOf(stale),
            now = now,
            zoneId = zone,
        )

        assertEquals(emptySet<String>(), mission.answeredWordIds)
        assertEquals(0, mission.completedWords)
        assertEquals(3, mission.remainingWords)
        assertFalse(mission.goalComplete)
    }

    @Test
    fun `completion appears only when this session crosses the daily goal`() {
        assertTrue(
            shouldShowDailyMissionCompletion(
                sessionStartedAtCount = 2,
                dailyGoal = 3,
                answeredBeforeAction = setOf("one", "two"),
                completedWordId = "three",
            ),
        )
        assertFalse(
            shouldShowDailyMissionCompletion(
                sessionStartedAtCount = 3,
                dailyGoal = 3,
                answeredBeforeAction = setOf("one", "two", "three"),
                completedWordId = "four",
            ),
        )
        assertFalse(
            shouldShowDailyMissionCompletion(
                sessionStartedAtCount = 2,
                dailyGoal = 3,
                answeredBeforeAction = setOf("one", "two"),
                completedWordId = "two",
            ),
        )
        assertFalse(
            shouldShowDailyMissionCompletion(
                sessionStartedAtCount = 2,
                dailyGoal = 3,
                answeredBeforeAction = setOf("one", "two", "external"),
                completedWordId = "four",
            ),
        )
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
                when (word.id) {
                    first.id -> listOf(
                        VerseToken(0, first.arabic, first),
                        VerseToken(1, "unknown", null),
                    )
                    else -> listOf(
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
    fun `completion is rendered before the empty queue fallback`() {
        val source = File("src/main/java/com/kalima/quran/ui/StudyScreen.kt").readText()

        assertTrue(source.indexOf("if (showCompletion && sessionWordIds.isNotEmpty())") <
            source.indexOf("if (words.isEmpty())"))
    }

    @Test
    fun `mission time refreshes while visible and when the app resumes`() {
        val source = File("src/main/java/com/kalima/quran/ui/StudyScreen.kt").readText()

        assertTrue(source.contains("delay(MISSION_REFRESH_MILLIS)"))
        assertTrue(source.contains("Lifecycle.Event.ON_RESUME"))
        assertTrue(source.contains("now = missionNow"))
    }

    @Test
    fun `mission session resets when the local day changes`() {
        val source = File("src/main/java/com/kalima/quran/ui/StudyScreen.kt").readText()
        val dateKeyedState = "rememberSaveable(scopeKey, selectionKey, missionDate)"

        assertTrue(source.contains("val missionDate = missionNow.atZone(ZoneId.systemDefault()).toLocalDate()"))
        assertEquals(4, Regex(Regex.escape(dateKeyedState)).findAll(source).count())
        assertTrue(source.contains("DisposableEffect(lifecycleOwner)"))
        assertTrue(source.contains("if (event == Lifecycle.Event.ON_RESUME)"))
        assertTrue(source.contains("LaunchedEffect(Unit)"))
        assertTrue(source.contains("if (missionSessionDate != missionDate.toString())"))
    }

    private fun word(id: String) = QuranWord(
        id = id,
        arabic = "كَلِمَة",
        lemma = "كَلِمَة",
        transliteration = id,
        meaning = id,
        root = "ك ل م",
        grammar = "noun",
        category = "test",
        reference = "Test 1:1",
        verseArabic = "كَلِمَة كَلِمَة",
        verseMeaning = "test verse",
        insight = "",
    )
}
