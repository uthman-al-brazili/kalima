package com.kalima.quran.desktop

/**
 * Converts Windows' continuously sampled idle duration into one return event.
 * A return is emitted only after the computer has crossed the configured idle
 * threshold and input makes the idle duration drop below that threshold again.
 */
class ReturnFromIdleDetector(
    private val cooldownMillis: Long = DEFAULT_COOLDOWN_MILLIS,
) {
    private var wasAway = false
    private var lastReturnAtMillis: Long? = null

    fun sample(
        idleMillis: Long,
        thresholdMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        val normalizedIdle = idleMillis.coerceAtLeast(0)
        val normalizedThreshold = thresholdMillis.coerceAtLeast(1)
        if (normalizedIdle >= normalizedThreshold) {
            wasAway = true
            return false
        }
        if (!wasAway) return false
        wasAway = false

        val previousReturn = lastReturnAtMillis
        if (previousReturn != null && nowMillis - previousReturn < cooldownMillis) return false
        lastReturnAtMillis = nowMillis
        return true
    }

    companion object {
        const val DEFAULT_COOLDOWN_MILLIS = 60_000L
    }
}
