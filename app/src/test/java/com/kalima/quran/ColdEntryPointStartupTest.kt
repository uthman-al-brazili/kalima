package com.kalima.quran

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColdEntryPointStartupTest {
    @Test
    fun `boot reads feature flags without constructing progress store`() {
        val receiver = source("notifications/ReminderReceiver.kt")
        val bootReceiver = receiver.substringAfter("class BootReceiver")
            .substringBefore("object NotificationHelper")

        assertTrue(bootReceiver.contains("ProgressFeatureFlags.isReminderEnabled"))
        assertTrue(bootReceiver.contains("ProgressFeatureFlags.isLockScreenEnabled"))
        assertFalse(bootReceiver.contains("ProgressStore.get"))
    }

    @Test
    fun `corpus dependent receivers use bounded async work`() {
        val reminder = source("notifications/ReminderReceiver.kt")
        val widget = source("widget/DailyQuranWordWidgetProvider.kt")
        val asyncWork = source("background/AsyncBroadcastWork.kt")

        assertTrue(reminder.contains("AsyncBroadcastWork.run(this, \"daily reminder\")"))
        assertTrue(widget.contains("AsyncBroadcastWork.run(this, \"widget update\")"))
        assertTrue(widget.contains("AsyncBroadcastWork.run(this, \"widget next\")"))
        assertTrue(asyncWork.contains("MAX_BROADCAST_WORK_SECONDS"))
        assertTrue(asyncWork.contains("pendingResult.finish()"))
    }

    @Test
    fun `foreground service posts notification before loading progress`() {
        val service = source("lockscreen/LockScreenStudyService.kt")
        val onCreate = service.substringAfter("override fun onCreate()")
            .substringBefore("override fun onStartCommand")

        assertTrue(onCreate.contains("enterForeground()"))
        assertFalse(onCreate.contains("ProgressStore.get"))
        assertTrue(service.contains("precomputeExecutor.execute"))
        assertTrue(service.contains("runCatching { ProgressStore.get(applicationContext) }"))
    }

    private fun source(relative: String): String = sequenceOf(
        File("app/src/main/java/com/kalima/quran/$relative"),
        File("src/main/java/com/kalima/quran/$relative"),
    ).firstOrNull(File::isFile)?.readText()
        ?: error("Android source not found: $relative")
}
