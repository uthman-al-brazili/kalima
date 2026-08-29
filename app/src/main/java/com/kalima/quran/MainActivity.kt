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
import com.kalima.quran.data.initialize
import com.kalima.quran.data.needsAlphabetFoundation
import com.kalima.quran.data.preloadQuranFirstPage
import com.kalima.quran.audio.OfflineWordAudioManager
import com.kalima.quran.lockscreen.LockScreenStudyService
import com.kalima.quran.lockscreen.LockScreenStudyActivity
import com.kalima.quran.localization.AppLanguage
import com.kalima.quran.localization.LanguageManager
import com.kalima.quran.notifications.NotificationHelper
import com.kalima.quran.notifications.ReminderScheduler
import com.kalima.quran.ui.KalimaApp
import com.kalima.quran.ui.ProgressStatisticsCache
import com.kalima.quran.ui.StartupLoadingScreen
import com.kalima.quran.ui.StudyLaunchTarget
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var progressStore: ProgressStore
    private var loadedProgressStore by mutableStateOf<ProgressStore?>(null)
    private var studyLaunchTarget by mutableStateOf<StudyLaunchTarget?>(null)
    private var lastStudyLaunchRequestId = 0L
    private var pendingBackupImport by mutableStateOf<DecodedProgressBackup?>(null)
    private val offlineWordAudioManager by lazy { OfflineWordAudioManager(applicationContext) }
    private var offlineWordAudioJob: Job? = null

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
            val store = loadedProgressStore ?: return@registerForActivityResult
            store.setReminderEnabled(true)
            ReminderScheduler.schedule(this)
        }
    }

    private val overlayPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        val store = loadedProgressStore ?: return@registerForActivityResult
        if (Settings.canDrawOverlays(this)) {
            store.setLockScreenEnabled(true)
            LockScreenStudyService.start(this)
        } else {
            store.setLockScreenEnabled(false)
            LockScreenStudyService.stop(this)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.localizedContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val store = loadedProgressStore
            if (store == null) {
                StartupLoadingScreen()
                return@setContent
            }

            val progress by store.progress.collectAsStateWithLifecycle()
            val offlineWordAudioState by offlineWordAudioManager.state.collectAsStateWithLifecycle()
            KalimaApp(
                progress = progress,
                onIntroduce = store::introduce,
                onAnswer = store::answer,
                onCurrentStudyWordChange = store::setCurrentStudyWord,
                onQuizAnswer = store::answerQuiz,
                onLockScreenChange = ::changeLockScreen,
                onLockScreenQuizChange = store::setLockScreenQuizEnabled,
                onLockScreenQuizIntervalChange = store::setLockScreenQuizInterval,
                onReminderChange = ::changeReminder,
                onDailyGoalChange = store::setDailyGoal,
                onMaximumWordsChange = store::setMaximumWords,
                onThemeModeChange = store::setThemeMode,
                onQuranFontSizeChange = store::setQuranFontSize,
                onQuranLearningOverlayChange = store::setQuranLearningOverlayEnabled,
                onAdvancedSettingsVisibleChange = store::setAdvancedSettingsVisible,
                onShowCompleteAyahChange = store::setShowCompleteAyah,
                onSpacedRepetitionEnabledChange = store::setSpacedRepetitionEnabled,
                onStudyScopeChange = store::toggleStudyScope,
                onToggleSurah = store::toggleSurah,
                onToggleCustomList = store::toggleCustomStudy,
                onToggleAlreadyKnown = store::toggleAlreadyKnown,
                onCompleteOnboarding = store::completeOnboarding,
                onCompleteAlphabetLesson = store::completeNextAlphabetLesson,
                onStartAlphabetFoundation = store::startAlphabetFoundation,
                onSkipAlphabetFoundation = store::skipAlphabetFoundation,
                onCompleteNumberLesson = store::completeNextNumberLesson,
                onStartNumberFoundation = store::startNumberFoundation,
                onOpenAppSettings = ::openAppSettings,
                onPreviewLockScreen = ::previewLockScreen,
                onOpenWebsite = ::openWebsite,
                onContactDeveloper = ::openSupportEmail,
                currentLanguage = LanguageManager.selectedLanguage(this),
                onLanguageChange = ::changeLanguage,
                onQuietHoursEnabledChange = store::setQuietHoursEnabled,
                onQuietHoursChange = store::setQuietHours,
                onLockScreenDailyLimitChange = store::setLockScreenDailyLimit,
                onPauseLockScreenOneHour = store::pauseLockScreenForHour,
                onPauseLockScreenToday = store::pauseLockScreenUntilTomorrow,
                onResumeLockScreen = store::resumeLockScreen,
                onLockScreenCooldownChange = store::setLockScreenCooldownMinutes,
                onExportBackup = ::chooseBackupDestination,
                onImportBackup = ::chooseBackupFile,
                backupImportPreview = pendingBackupImport,
                onConfirmBackupImport = ::confirmBackupImport,
                onCancelBackupImport = { pendingBackupImport = null },
                offlineWordAudioState = offlineWordAudioState,
                onDownloadOfflineWordAudio = { wordLocations, verseLocations ->
                    if (offlineWordAudioJob?.isActive != true) {
                        offlineWordAudioJob = lifecycleScope.launch {
                            offlineWordAudioManager.download(wordLocations, verseLocations)
                        }
                    }
                },
                onCancelOfflineWordAudio = { offlineWordAudioJob?.cancel() },
                studyLaunchTarget = studyLaunchTarget,
                onStudyLaunchTargetHandled = ::consumeStudyLaunchTarget,
            )
        }

        lifecycleScope.launch {
            val store = withContext(Dispatchers.Default) {
                ProgressStore.get(applicationContext)
            }
            progressStore = store
            updateStudyLaunchTarget(intent)
            loadedProgressStore = store

            launch {
                delay(POST_RENDER_WORK_DELAY_MS)
                synchronizeBackgroundFeatures()
            }
            launch(Dispatchers.Default) {
                delay(POST_RENDER_WORK_DELAY_MS)
                WordRepository.prepareDeferredIndexes()
            }
            launch(Dispatchers.Default) {
                store.progress.collectLatest { progress ->
                    ProgressStatisticsCache.prepare(progress, WordRepository.words)
                }
            }
            launch(Dispatchers.IO) {
                delay(POST_RENDER_WORK_DELAY_MS)
                preloadQuranFirstPage(applicationContext)
            }
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
        loadedProgressStore = null
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                WordRepository.initialize(applicationContext)
            }
            recreate()
        }
    }

    private fun changeLockScreen(enabled: Boolean) {
        if (enabled && progressStore.progress.value.needsAlphabetFoundation) {
            Toast.makeText(this, R.string.finish_alphabet_first, Toast.LENGTH_LONG).show()
            return
        }
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
        startActivity(
            Intent(this, LockScreenStudyActivity::class.java)
                .putExtra(LockScreenStudyActivity.EXTRA_PREVIEW, true),
        )
    }

    private fun openWebsite() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, WEBSITE_URL.toUri()))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.website_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    private fun openSupportEmail() {
        val intent = Intent(Intent.ACTION_SENDTO, "mailto:$SUPPORT_EMAIL".toUri())
            .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.support_email_subject))
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.support_email_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    private fun changeReminder(enabled: Boolean) {
        if (enabled && progressStore.progress.value.needsAlphabetFoundation) {
            Toast.makeText(this, R.string.finish_alphabet_first, Toast.LENGTH_LONG).show()
            return
        }
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
                        knownWordIds = WordRepository.wordIds,
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
        if (!WordRepository.containsWord(wordId)) return
        lastStudyLaunchRequestId = nextStudyLaunchRequestId(
            previousId = lastStudyLaunchRequestId,
            nowNanos = SystemClock.elapsedRealtimeNanos(),
        )
        studyLaunchTarget = StudyLaunchTarget(wordId, lastStudyLaunchRequestId)
    }

    private fun consumeStudyLaunchTarget(requestId: Long) {
        if (studyLaunchTarget?.requestId == requestId) {
            studyLaunchTarget = null
        }
    }

    companion object {
        private const val TAG = "KalimaMain"
        private const val POST_RENDER_WORK_DELAY_MS = 500L
        private const val EXTRA_STUDY_WORD_ID = "com.kalima.quran.extra.STUDY_WORD_ID"
        private const val WEBSITE_URL = "https://kalima-h1f.pages.dev/"
        private const val SUPPORT_EMAIL = "uthman-al-brazili@proton.me"

        fun createStudyIntent(context: Context, wordId: String): Intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_STUDY_WORD_ID, wordId)
            }
    }
}

internal fun nextStudyLaunchRequestId(previousId: Long, nowNanos: Long): Long =
    if (nowNanos > previousId) nowNanos else previousId + 1L
