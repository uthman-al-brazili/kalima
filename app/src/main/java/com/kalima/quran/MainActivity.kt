package com.kalima.quran

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.kalima.quran.data.DecodedProgressBackup
import com.kalima.quran.data.ProgressBackupCodec
import com.kalima.quran.data.ProgressStore
import com.kalima.quran.data.WordRepository
import com.kalima.quran.audio.ArabicVoiceInstaller
import com.kalima.quran.lockscreen.LockScreenStudyService
import com.kalima.quran.lockscreen.LockScreenStudyActivity
import com.kalima.quran.localization.AppLanguage
import com.kalima.quran.localization.LanguageManager
import com.kalima.quran.notifications.NotificationHelper
import com.kalima.quran.notifications.ReminderScheduler
import com.kalima.quran.ui.KalimaApp
import com.kalima.quran.ui.StudyLaunchTarget
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var progressStore: ProgressStore
    private var studyLaunchTarget by mutableStateOf<StudyLaunchTarget?>(null)
    private var pendingBackupImport by mutableStateOf<DecodedProgressBackup?>(null)

    private val createBackupDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) exportBackup(uri)
    }

    private val openBackupDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) inspectBackup(uri)
    }

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

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.localizedContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressStore = ProgressStore.get(applicationContext)
        updateStudyLaunchTarget(intent)
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
                onCurrentStudyWordChange = progressStore::setCurrentStudyWord,
                onQuizAnswer = progressStore::answerQuiz,
                onLockScreenChange = ::changeLockScreen,
                onLockScreenQuizChange = progressStore::setLockScreenQuizEnabled,
                onLockScreenQuizIntervalChange = progressStore::setLockScreenQuizInterval,
                onReminderChange = ::changeReminder,
                onDailyGoalChange = progressStore::setDailyGoal,
                onMaximumWordsChange = progressStore::setMaximumWords,
                onThemeModeChange = progressStore::setThemeMode,
                onAdvancedSettingsVisibleChange = progressStore::setAdvancedSettingsVisible,
                onSpacedRepetitionEnabledChange = progressStore::setSpacedRepetitionEnabled,
                onStudyScopeChange = progressStore::setStudyScope,
                onToggleSurah = progressStore::toggleSurah,
                onToggleCustomList = progressStore::toggleCustomStudy,
                onCompleteOnboarding = progressStore::completeOnboarding,
                onOpenAppSettings = ::openAppSettings,
                onOpenTextToSpeechSettings = ::openTextToSpeechSettings,
                onPreviewLockScreen = ::previewLockScreen,
                currentLanguage = LanguageManager.selectedLanguage(this),
                onLanguageChange = ::changeLanguage,
                onQuietHoursEnabledChange = progressStore::setQuietHoursEnabled,
                onQuietHoursChange = progressStore::setQuietHours,
                onLockScreenDailyLimitChange = progressStore::setLockScreenDailyLimit,
                onPauseLockScreenOneHour = progressStore::pauseLockScreenForHour,
                onPauseLockScreenToday = progressStore::pauseLockScreenUntilTomorrow,
                onResumeLockScreen = progressStore::resumeLockScreen,
                onLockScreenCooldownChange = progressStore::setLockScreenCooldownMinutes,
                onExportBackup = ::chooseBackupDestination,
                onImportBackup = ::chooseBackupFile,
                backupImportPreview = pendingBackupImport,
                onConfirmBackupImport = ::confirmBackupImport,
                onCancelBackupImport = { pendingBackupImport = null },
                studyLaunchTarget = studyLaunchTarget,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateStudyLaunchTarget(intent)
    }

    private fun changeLanguage(language: AppLanguage) {
        if (LanguageManager.selectedLanguage(this) == language) return
        LanguageManager.setLanguage(applicationContext, language)
        recreate()
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

    private fun openTextToSpeechSettings() {
        if (!ArabicVoiceInstaller.open(this)) {
            Toast.makeText(
                this,
                R.string.pronunciation_installation_failed,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun previewLockScreen() {
        startActivity(
            Intent(this, LockScreenStudyActivity::class.java)
                .putExtra(LockScreenStudyActivity.EXTRA_PREVIEW, true),
        )
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

    private fun chooseBackupDestination() {
        createBackupDocument.launch(
            "kalima-${LocalDate.now()}.${ProgressBackupCodec.FILE_EXTENSION}",
        )
    }

    private fun chooseBackupFile() {
        openBackupDocument.launch(arrayOf("application/octet-stream", "text/plain", "*/*"))
    }

    private fun exportBackup(uri: android.net.Uri) {
        lifecycleScope.launch {
            val succeeded = withContext(Dispatchers.IO) {
                runCatching {
                    val text = encodeCurrentBackup()
                    requireNotNull(contentResolver.openOutputStream(uri)).bufferedWriter().use {
                        it.write(text)
                    }
                }.onFailure { error -> Log.e(TAG, "Unable to export progress backup", error) }
                    .isSuccess
            }
            Toast.makeText(
                this@MainActivity,
                if (succeeded) R.string.backup_exported else R.string.backup_failed,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun inspectBackup(uri: android.net.Uri) {
        lifecycleScope.launch {
            val decoded = withContext(Dispatchers.IO) {
                runCatching {
                    val text = requireNotNull(contentResolver.openInputStream(uri)).bufferedReader().use {
                        it.readText()
                    }
                    ProgressBackupCodec.decode(
                        backup = text,
                        expectedCorpusIdentity = WordRepository.corpusIdentity(),
                        knownWordIds = WordRepository.words.mapTo(mutableSetOf()) { it.id },
                    )
                }.onFailure { error -> Log.w(TAG, "Invalid progress backup", error) }
                    .getOrNull()
            }
            if (decoded == null) {
                Toast.makeText(this@MainActivity, R.string.backup_invalid, Toast.LENGTH_LONG).show()
            } else {
                pendingBackupImport = decoded
            }
        }
    }

    private fun confirmBackupImport() {
        val decoded = pendingBackupImport ?: return
        lifecycleScope.launch {
            val recoveryCreated = withContext(Dispatchers.IO) {
                runCatching {
                    val directory = java.io.File(filesDir, "progress-recovery").apply { mkdirs() }
                    val file = java.io.File(directory, "before-import-${System.currentTimeMillis()}.kalima-backup")
                    file.writeText(encodeCurrentBackup())
                }.onFailure { error -> Log.e(TAG, "Unable to create recovery backup", error) }
                    .isSuccess
            }
            if (!recoveryCreated) {
                Toast.makeText(this@MainActivity, R.string.backup_recovery_failed, Toast.LENGTH_LONG).show()
                return@launch
            }
            progressStore.restoreFromBackup(decoded.progress)
            pendingBackupImport = null
            synchronizeBackgroundFeatures()
            Toast.makeText(this@MainActivity, R.string.backup_restored, Toast.LENGTH_LONG).show()
        }
    }

    private fun encodeCurrentBackup(): String = ProgressBackupCodec.encode(
        progress = progressStore.snapshotForBackup(),
        appVersion = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty(),
        corpusIdentity = WordRepository.corpusIdentity(),
        createdAt = Instant.now(),
    )

    private fun synchronizeBackgroundFeatures() {
        val progress = progressStore.progress.value
        if (progress.reminderEnabled) ReminderScheduler.schedule(this) else ReminderScheduler.cancel(this)
        if (progress.lockScreenEnabled && Settings.canDrawOverlays(this)) {
            LockScreenStudyService.start(this)
        } else {
            if (progress.lockScreenEnabled) progressStore.setLockScreenEnabled(false)
            LockScreenStudyService.stop(this)
        }
    }

    private fun updateStudyLaunchTarget(intent: Intent) {
        val wordId = intent.getStringExtra(EXTRA_STUDY_WORD_ID) ?: return
        if (WordRepository.words.none { it.id == wordId }) return
        val requestId = intent.getLongExtra(EXTRA_STUDY_REQUEST_ID, 0L)
            .takeIf { it != 0L }
            ?: SystemClock.elapsedRealtimeNanos()
        studyLaunchTarget = StudyLaunchTarget(wordId, requestId)
    }

    companion object {
        private const val TAG = "KalimaMain"
        private const val EXTRA_STUDY_WORD_ID = "com.kalima.quran.extra.STUDY_WORD_ID"
        private const val EXTRA_STUDY_REQUEST_ID = "com.kalima.quran.extra.STUDY_REQUEST_ID"

        fun createStudyIntent(context: Context, wordId: String): Intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_STUDY_WORD_ID, wordId)
                putExtra(EXTRA_STUDY_REQUEST_ID, SystemClock.elapsedRealtimeNanos())
            }
    }
}
