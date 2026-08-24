package com.kalima.quran.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class LockScreenLearningCopyRegressionTest {
    @Test
    fun `onboarding explains opt-in permissions and device security in both languages`() {
        assertRequiredLockScreenCopy(source("res/values-en/strings.xml"))
        assertRequiredLockScreenCopy(source("res/values/strings.xml"))
    }

    @Test
    fun `study keeps a visible status or activation card without changing settings controls`() {
        val study = source("java/com/kalima/quran/ui/StudyScreen.kt")
        val settings = source("java/com/kalima/quran/ui/SettingsScreen.kt")

        assertTrue(study.contains("LockScreenLearningCard("))
        assertTrue(study.contains("progress.lockScreenEnabled"))
        assertTrue(study.contains("onEnableLockScreen"))
        assertTrue(settings.contains("onLockScreenChange"))
        assertTrue(settings.contains("onLockScreenQuizChange"))
        assertTrue(settings.contains("onPauseLockScreenOneHour"))
    }

    private fun assertRequiredLockScreenCopy(strings: String) {
        assertTrue(strings.contains("onboarding_lock_screen_title"))
        assertTrue(strings.contains("onboarding_lock_screen_description"))
        assertTrue(strings.contains("study_lock_screen_active_description"))
        assertTrue(strings.contains("permission") || strings.contains("permissão"))
        assertTrue(strings.contains("bypasses") || strings.contains("contorna"))
    }

    private fun source(relative: String): String = sequenceOf(
        File("app/src/main/$relative"),
        File("src/main/$relative"),
    ).firstOrNull(File::isFile)?.readText()
        ?: error("Android source not found: $relative")
}
