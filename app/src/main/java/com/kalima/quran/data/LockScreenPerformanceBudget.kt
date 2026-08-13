package com.kalima.quran.data

object LockScreenPerformanceBudget {
    const val MAX_LAUNCH_LATENCY_MS = 700L

    fun isWithinBudget(latencyMs: Long?): Boolean =
        latencyMs != null && latencyMs in 0..MAX_LAUNCH_LATENCY_MS
}
