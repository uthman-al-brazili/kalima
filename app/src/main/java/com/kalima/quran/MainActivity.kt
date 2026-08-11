package com.kalima.quran

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kalima.quran.data.ProgressStore
import com.kalima.quran.lockscreen.LockScreenStudyService
import com.kalima.quran.lockscreen.LockScreenStudyActivity
import com.kalima.quran.notifications.NotificationHelper
import com.kalima.quran.notifications.ReminderScheduler
import com.kalima.quran.ui.KalimaApp

class MainActivity : ComponentActivity() {
    private lateinit var progressStore: ProgressStore

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            progressStore.setReminderEnabled(true)
            ReminderScheduler.schedule(this)
        }
    }

    private val overlayPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (Settings.canDrawOverlays(this)) {
            progressStore.setLockScreenEnabled(true)
            LockScreenStudyService.start(this)
        } else {
            progressStore.setLockScreenEnabled(false)
            LockScreenStudyService.stop(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressStore = ProgressStore(applicationContext)
        NotificationHelper.createChannel(this)
        if (progressStore.progress.value.reminderEnabled) {
            ReminderScheduler.schedule(this)
        }
        if (progressStore.progress.value.lockScreenEnabled) {
            if (Settings.canDrawOverlays(this)) {
                LockScreenStudyService.start(this)
            } else {
                progressStore.setLockScreenEnabled(false)
            }
        }

        setContent {
            val progress by progressStore.progress.collectAsStateWithLifecycle()
            KalimaApp(
                progress = progress,
                onAnswer = progressStore::answer,
                onQuizAnswer = progressStore::answerQuiz,
                onLockScreenChange = ::changeLockScreen,
                onLockScreenQuizChange = progressStore::setLockScreenQuizEnabled,
                onLockScreenQuizIntervalChange = progressStore::setLockScreenQuizInterval,
                onReminderChange = ::changeReminder,
                onDailyGoalChange = progressStore::setDailyGoal,
                onStudyScopeChange = progressStore::setStudyScope,
                onToggleSurah = progressStore::toggleSurah,
                onOpenAppSettings = ::openAppSettings,
                onPreviewLockScreen = ::previewLockScreen,
            )
        }
    }

    private fun changeLockScreen(enabled: Boolean) {
        if (!enabled) {
            progressStore.setLockScreenEnabled(false)
            LockScreenStudyService.stop(this)
            return
        }

        if (Settings.canDrawOverlays(this)) {
            progressStore.setLockScreenEnabled(true)
            LockScreenStudyService.start(this)
            return
        }

        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:$packageName".toUri(),
        )
        try {
            overlayPermission.launch(intent)
        } catch (_: ActivityNotFoundException) {
            overlayPermission.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        }
    }

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:$packageName".toUri(),
            ),
        )
    }

    private fun previewLockScreen() {
        startActivity(Intent(this, LockScreenStudyActivity::class.java))
    }

    private fun changeReminder(enabled: Boolean) {
        if (!enabled) {
            progressStore.setReminderEnabled(false)
            ReminderScheduler.cancel(this)
            return
        }

        val needsPermission =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            progressStore.setReminderEnabled(true)
            ReminderScheduler.schedule(this)
        }
    }
}
