package com.kalima.quran.data

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewHistoryTest {
    private val now = Instant.parse("2026-08-12T12:00:00Z")

    @Test
    fun codecPreservesReviewEvents() {
        val events = listOf(
            ReviewEvent(now.minusSeconds(60), "word-1", true, true, ReviewSource.Study),
            ReviewEvent(now, "word-2", false, false, ReviewSource.LockScreen),
        )

        assertEquals(events, ReviewEventCodec.decode(ReviewEventCodec.encode(events)))
    }

    @Test
    fun recentAccuracyUsesOnlyRequestedWindow() {
        val events = listOf(
            ReviewEvent(now.minusSeconds(2 * 86_400), "a", true, true, ReviewSource.Study),
            ReviewEvent(now.minusSeconds(86_400), "b", false, false, ReviewSource.Quiz),
            ReviewEvent(now.minusSeconds(40 * 86_400), "c", true, false, ReviewSource.Study),
        )

        assertEquals(50, ReviewHistory.accuracy(events, 7, now))
        assertNull(ReviewHistory.accuracy(emptyList(), 1, now))
    }
}
