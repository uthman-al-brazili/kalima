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
    fun `study shows a dismissible activation popup only while lock screen learning is off`() {
        val study = source("java/com/kalima/quran/ui/StudyScreen.kt")
        val mission = source("java/com/kalima/quran/ui/StudyMissionScreen.kt")
        val settings = source("java/com/kalima/quran/ui/SettingsScreen.kt")

        assertTrue(study.contains("if (!progress.lockScreenEnabled && showLockScreenPrompt)"))
        assertTrue(study.contains("LockScreenLearningPopup("))
        assertTrue(study.contains("AlertDialog("))
        assertTrue(study.contains("onDismiss = { showLockScreenPrompt = false }"))
        assertTrue(study.contains("onEnableLockScreen"))
        assertTrue(!mission.contains("LockScreenLearningCard("))
        assertTrue(settings.contains("onLockScreenChange"))
        assertTrue(settings.contains("onLockScreenQuizChange"))
        assertTrue(settings.contains("onPauseLockScreenOneHour"))
    }

    @Test
    fun `enabling lock screen study requests notification permission without making it mandatory`() {
        val activity = source("java/com/kalima/quran/MainActivity.kt")
        val lockScreenChange = activity.substringAfter("private fun changeLockScreen")
            .substringBefore("private fun continueLockScreenEnable")
        val permissionCallback = activity.substringAfter(
            "private val lockScreenNotificationPermission",
        ).substringBefore("private val overlayPermission")

        assertTrue(lockScreenChange.contains("Build.VERSION_CODES.TIRAMISU"))
        assertTrue(
            lockScreenChange.contains(
                "lockScreenNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)",
            ),
        )
        assertTrue(permissionCallback.contains("continueLockScreenEnable()"))
    }

    private fun assertRequiredLockScreenCopy(strings: String) {
        assertTrue(strings.contains("onboarding_lock_screen_title"))
        assertTrue(strings.contains("onboarding_lock_screen_description"))
        assertTrue(strings.contains("lock_screen_security_note"))
        assertTrue(strings.contains("permission") || strings.contains("permissão"))
        assertTrue(strings.contains("bypasses") || strings.contains("contorna"))
    }

    private fun source(relative: String): String = sequenceOf(
        File("app/src/main/$relative"),
        File("src/main/$relative"),
    ).firstOrNull(File::isFile)?.readText()
        ?: error("Android source not found: $relative")
}
