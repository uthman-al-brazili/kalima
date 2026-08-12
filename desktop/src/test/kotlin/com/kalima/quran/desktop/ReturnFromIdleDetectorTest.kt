package com.kalima.quran.desktop

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReturnFromIdleDetectorTest {
    @Test
    fun emitsOneEventWhenInputResumesAfterThreshold() {
        val detector = ReturnFromIdleDetector(cooldownMillis = 1_000)

        assertFalse(detector.sample(idleMillis = 30_000, thresholdMillis = 60_000, nowMillis = 0))
        assertFalse(detector.sample(idleMillis = 60_000, thresholdMillis = 60_000, nowMillis = 60_000))
        assertTrue(detector.sample(idleMillis = 250, thresholdMillis = 60_000, nowMillis = 60_250))
        assertFalse(detector.sample(idleMillis = 500, thresholdMillis = 60_000, nowMillis = 60_500))
    }

    @Test
    fun cooldownPreventsRepeatedReturns() {
        val detector = ReturnFromIdleDetector(cooldownMillis = 10_000)

        detector.sample(idleMillis = 60_000, thresholdMillis = 60_000, nowMillis = 60_000)
        assertTrue(detector.sample(idleMillis = 0, thresholdMillis = 60_000, nowMillis = 61_000))
        detector.sample(idleMillis = 60_000, thresholdMillis = 60_000, nowMillis = 62_000)
        assertFalse(detector.sample(idleMillis = 0, thresholdMillis = 60_000, nowMillis = 63_000))
    }

    @Test
    fun startupCommandUsesOnlyPackagedKalimaExecutable() {
        assertEquals(
            "\"C:\\Apps\\Kalima\\Kalima.exe\" --background",
            DesktopStartupManager.startupCommandFor(Path.of("C:\\Apps\\Kalima\\Kalima.exe")),
        )
        assertEquals(null, DesktopStartupManager.startupCommandFor(Path.of("C:\\Java\\java.exe")))
    }
}
