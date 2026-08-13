package com.kalima.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class SpacedRepetitionTest {
    private val now = Instant.parse("2026-08-12T12:00:00Z")

    @Test
    fun successfulReviewsExpandFromOneToThreeToEightDays() {
        val first = SpacedRepetition.review(null, ReviewGrade.Good, now)
        val second = SpacedRepetition.review(first, ReviewGrade.Good, first.dueAt)
        val third = SpacedRepetition.review(second, ReviewGrade.Good, second.dueAt)

        assertEquals(1, first.intervalDays)
        assertEquals(3, second.intervalDays)
        assertEquals(8, third.intervalDays)
        assertEquals(3, third.repetitions)
        assertTrue(second.isLearned)
    }

    @Test
    fun forgottenCardUsesTenMinuteRelearningStepAndLosesEase() {
        val learned = SpacedRepetition.review(null, ReviewGrade.Good, now)
        val forgotten = SpacedRepetition.review(learned, ReviewGrade.Again, learned.dueAt)

        assertEquals(0, forgotten.repetitions)
        assertEquals(0, forgotten.intervalDays)
        assertEquals(Duration.ofMinutes(10), Duration.between(forgotten.lastReviewedAt, forgotten.dueAt))
        assertEquals(2.3, forgotten.easeFactor, 0.001)
        assertEquals(1, forgotten.lapses)
        assertFalse(forgotten.isLearned)
    }

    @Test
    fun earlySuccessfulReviewDoesNotInflateInterval() {
        val first = SpacedRepetition.review(null, ReviewGrade.Good, now)
        val early = SpacedRepetition.review(first, ReviewGrade.Good, now.plusSeconds(60))

        assertEquals(first, early)
    }

    @Test
    fun queuePrioritizesOldestDueCardsThenRotatedNewCardsAndHidesFutureCards() {
        val words = WordRepository.words.take(5)
        val schedules = mapOf(
            words[0].id to scheduleDueAt(now.minusSeconds(60)),
            words[1].id to scheduleDueAt(now.minusSeconds(120)),
            words[2].id to scheduleDueAt(now.plusSeconds(60)),
        )

        val queue = ReviewQueue.ordered(words, schedules, now, newStartIndex = 1)

        assertEquals(listOf(words[1], words[0], words[4], words[3]), queue)
    }

    @Test
    fun disabledSchedulingCanRotateEveryWordWithoutDueFiltering() {
        val words = WordRepository.words.take(4)

        assertEquals(
            listOf(words[2], words[3], words[0], words[1]),
            ReviewQueue.rotated(words, startIndex = 2),
        )
    }

    @Test
    fun disabledSchedulingReportsNoDueReviewsWithoutDeletingSchedules() {
        val word = WordRepository.words.first()
        val progress = StudyProgress(
            spacedRepetitionEnabled = false,
            reviewSchedules = mapOf(word.id to scheduleDueAt(now.minusSeconds(60))),
        )

        assertEquals(0, progress.dueReviewCount(setOf(word.id), now))
        assertTrue(progress.reviewSchedules.containsKey(word.id))
    }

    @Test
    fun scheduleCodecRoundTripsAndIgnoresMalformedRecords() {
        val schedules = mapOf("allah" to scheduleDueAt(now))
        val encoded = ReviewScheduleCodec.encode(schedules) + "broken"

        assertEquals(schedules, ReviewScheduleCodec.decode(encoded))
    }

    private fun scheduleDueAt(dueAt: Instant) = ReviewSchedule(
        repetitions = 2,
        intervalDays = 3,
        easeFactor = 2.5,
        dueAt = dueAt,
        lastReviewedAt = now.minus(Duration.ofDays(3)),
    )
}
