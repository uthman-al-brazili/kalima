package com.kalima.quran.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class LockScreenBlockReason {
    Disabled,
    Paused,
    QuietHours,
    DailyLimit,
}

object LockScreenPolicy {
    fun blockReason(
        enabled: Boolean,
        pausedUntil: Instant?,
        quietHoursEnabled: Boolean,
        quietStartHour: Int,
        quietEndHour: Int,
        dailyLimit: Int,
        shownToday: Int,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): LockScreenBlockReason? {
        if (!enabled) return LockScreenBlockReason.Disabled
        if (pausedUntil != null && pausedUntil > now) return LockScreenBlockReason.Paused
        val hour = now.atZone(zoneId).hour
        if (quietHoursEnabled && isQuietHour(hour, quietStartHour, quietEndHour)) {
            return LockScreenBlockReason.QuietHours
        }
        if (dailyLimit > 0 && shownToday >= dailyLimit) return LockScreenBlockReason.DailyLimit
        return null
    }

    fun isQuietHour(hour: Int, startHour: Int, endHour: Int): Boolean {
        val start = startHour.coerceIn(0, 23)
        val end = endHour.coerceIn(0, 23)
        if (start == end) return true
        return if (start < end) hour in start until end else hour >= start || hour < end
    }

    fun pauseUntilTomorrow(
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Instant = LocalDate.now(zoneId).plusDays(1).atStartOfDay(zoneId).toInstant()
}
