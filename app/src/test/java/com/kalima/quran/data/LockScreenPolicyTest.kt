package com.kalima.quran.data

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LockScreenPolicyTest {
    private val utc = ZoneId.of("UTC")

    @Test
    fun overnightQuietHoursCrossMidnight() {
        assertTrue(LockScreenPolicy.isQuietHour(23, 22, 7))
        assertTrue(LockScreenPolicy.isQuietHour(6, 22, 7))
        assertEquals(false, LockScreenPolicy.isQuietHour(12, 22, 7))
    }

    @Test
    fun policyReportsPauseQuietHoursAndDailyLimit() {
        val noon = Instant.parse("2026-08-12T12:00:00Z")
        assertEquals(
            LockScreenBlockReason.Paused,
            LockScreenPolicy.blockReason(true, noon.plusSeconds(60), true, 22, 7, 20, 0, noon, utc),
        )
        assertEquals(
            LockScreenBlockReason.DailyLimit,
            LockScreenPolicy.blockReason(true, null, true, 22, 7, 20, 20, noon, utc),
        )
        assertNull(LockScreenPolicy.blockReason(true, null, true, 22, 7, 20, 2, noon, utc))
    }
}
