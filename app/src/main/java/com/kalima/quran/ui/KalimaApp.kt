package com.kalima.quran.ui

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import androidx.annotation.StringRes
import androidx.annotation.DrawableRes
import com.kalima.quran.R
import com.kalima.quran.audio.OfflineWordAudioDownloadState
import com.kalima.quran.data.AppThemeMode
import com.kalima.quran.data.DecodedProgressBackup
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.QuranWordAudioLocation
import com.kalima.quran.data.QuranVerseAudioLocation
import com.kalima.quran.data.StudyScope
import com.kalima.quran.localization.AppLanguage
import com.kalima.quran.ui.theme.KalimaTheme

private enum class AppTab(@param:StringRes val labelRes: Int, @param:DrawableRes val iconRes: Int) {
    Study(R.string.tab_study, R.drawable.ic_study),
    Quran(R.string.tab_quran, R.drawable.ic_quran),
    Library(R.string.tab_words, R.drawable.ic_library),
    Quiz(R.string.tab_quiz, R.drawable.ic_quiz),
    Progress(R.string.tab_progress, R.drawable.ic_progress),
    Foundations(R.string.tab_foundations, R.drawable.ic_foundations),
    Settings(R.string.tab_settings, R.drawable.ic_settings),
}

data class StudyLaunchTarget(val wordId: String, val requestId: Long)

@Composable
fun KalimaApp(
    progress: StudyProgress,
    onIntroduce: (String) -> Unit,
    onAnswer: (String, Boolean) -> Unit,
    onCurrentStudyWordChange: (String) -> Unit,
    onQuizAnswer: (String, Boolean) -> Unit,
    onLockScreenChange: (Boolean) -> Unit,
    onLockScreenQuizChange: (Boolean) -> Unit,
    onLockScreenQuizIntervalChange: (Int) -> Unit,
    onReminderChange: (Boolean) -> Unit,
    onDailyGoalChange: (Int) -> Unit,
    onMaximumWordsChange: (Int) -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onQuranFontSizeChange: (Int) -> Unit,
    onQuranLearningOverlayChange: (Boolean) -> Unit,
    onAdvancedSettingsVisibleChange: (Boolean) -> Unit,
    onShowCompleteAyahChange: (Boolean) -> Unit,
    onSpacedRepetitionEnabledChange: (Boolean) -> Unit,
    onStudyScopeChange: (StudyScope) -> Unit,
    onToggleSurah: (Int) -> Unit,
    onToggleCustomList: (String) -> Unit,
    onToggleAlreadyKnown: (String) -> Unit,
    onCompleteOnboarding: (StudyScope, Int, Boolean, Boolean) -> Unit,
    onCompleteAlphabetLesson: () -> Unit,
    onStartAlphabetFoundation: () -> Unit,
    onSkipAlphabetFoundation: () -> Unit,
    onCompleteNumberLesson: () -> Unit,
    onStartNumberFoundation: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onPreviewLockScreen: () -> Unit,
    onOpenWebsite: () -> Unit,
    onContactDeveloper: () -> Unit,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onQuietHoursEnabledChange: (Boolean) -> Unit,
    onQuietHoursChange: (Int, Int) -> Unit,
    onLockScreenDailyLimitChange: (Int) -> Unit,
    onPauseLockScreenOneHour: () -> Unit,
    onPauseLockScreenToday: () -> Unit,
    onResumeLockScreen: () -> Unit,
    onLockScreenCooldownChange: (Int) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    backupImportPreview: DecodedProgressBackup?,
    onConfirmBackupImport: () -> Unit,
    onCancelBackupImport: () -> Unit,
    offlineWordAudioState: OfflineWordAudioDownloadState,
    onDownloadOfflineWordAudio: (List<QuranWordAudioLocation>, List<QuranVerseAudioLocation>) -> Unit,
    onCancelOfflineWordAudio: () -> Unit,
    studyLaunchTarget: StudyLaunchTarget? = null,
    onStudyLaunchTargetHandled: (Long) -> Unit = {},
) {
    var selectedName by rememberSaveable { mutableStateOf(AppTab.Study.name) }
    var handledStudyRequestId by rememberSaveable { mutableLongStateOf(NO_STUDY_REQUEST) }
    var excludedWordsRequestId by rememberSaveable { mutableLongStateOf(0L) }
    var readerStudyWordId by rememberSaveable { mutableStateOf<String?>(null) }
    var readerStudyRequestId by rememberSaveable { mutableLongStateOf(-1L) }
    val hasPendingStudyRequest = studyLaunchTarget != null &&
        studyLaunchTarget.requestId != handledStudyRequestId
    val selected = if (hasPendingStudyRequest) AppTab.Study else AppTab.valueOf(selectedName)
    val activeStudyLaunchTarget = studyLaunchTarget ?: readerStudyWordId?.let { wordId ->
        StudyLaunchTarget(wordId, readerStudyRequestId)
    }
    val pronouncer = rememberArabicPronouncer()
    val screenStateHolder = rememberSaveableStateHolder()

    LaunchedEffect(studyLaunchTarget?.requestId) {
        val target = studyLaunchTarget ?: return@LaunchedEffect
        if (target.requestId != handledStudyRequestId) {
            selectedName = AppTab.Study.name
            handledStudyRequestId = target.requestId
        }
    }

    KalimaTheme(themeMode = progress.themeMode) {
        val view = LocalView.current
        val backgroundColor = MaterialTheme.colorScheme.background
        val surfaceColor = MaterialTheme.colorScheme.surface
        SideEffect {
            if (!view.isInEditMode) {
                val window = (view.context as Activity).window
                window.statusBarColor = backgroundColor.toArgb()
                window.navigationBarColor = surfaceColor.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = backgroundColor.luminance() > 0.5f
                    isAppearanceLightNavigationBars = surfaceColor.luminance() > 0.5f
                }
            }
        }
        if (!progress.onboardingComplete) {
            OnboardingScreen(onComplete = onCompleteOnboarding)
            return@KalimaTheme
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    AppTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selected == tab,
                            onClick = { selectedName = tab.name },
                            icon = {
                                Icon(
                                    painter = painterResource(tab.iconRes),
                                    contentDescription = stringResource(tab.labelRes),
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    maxLines = 1,
                                    softWrap = false,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            alwaysShowLabel = false,
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                TabStateProvider(selected, screenStateHolder) {
                    when (selected) {
                        AppTab.Study -> StudyScreen(
                            progress = progress,
                            onIntroduce = onIntroduce,
                            onAnswer = onAnswer,
                            onCurrentWordChange = onCurrentStudyWordChange,
                            onEnableLockScreen = { onLockScreenChange(true) },
                            onOpenExcludedWords = {
                                excludedWordsRequestId += 1L
                                selectedName = AppTab.Library.name
                            },
                            pronouncer = pronouncer,
                            onToggleCustomList = onToggleCustomList,
                            onToggleAlreadyKnown = onToggleAlreadyKnown,
                            onShowCompleteAyahChange = onShowCompleteAyahChange,
                            onOpenFoundations = { selectedName = AppTab.Foundations.name },
                            launchTarget = activeStudyLaunchTarget,
                            onLaunchTargetHandled = { requestId ->
                                selectedName = AppTab.Study.name
                                if (requestId == readerStudyRequestId) {
                                    readerStudyWordId = null
                                } else {
                                    handledStudyRequestId = requestId
                                    onStudyLaunchTargetHandled(requestId)
                                }
                            },
                        )
                        AppTab.Quran -> QuranReaderScreen(
                            progress = progress,
                            fontSizeSp = progress.quranFontSizeSp,
                            customStudyIds = progress.customStudyIds,
                            learningOverlayEnabled = progress.quranLearningOverlayEnabled,
                            onFontSizeChange = onQuranFontSizeChange,
                            onLearningOverlayChange = onQuranLearningOverlayChange,
                            onToggleCustomList = onToggleCustomList,
                            onStudyWord = { wordId ->
                                readerStudyRequestId -= 1L
                                readerStudyWordId = wordId
                                selectedName = AppTab.Study.name
                            },
                        )
                        AppTab.Library -> LibraryScreen(
                            progress = progress,
                            pronouncer = pronouncer,
                            onToggleCustomList = onToggleCustomList,
                            onToggleAlreadyKnown = onToggleAlreadyKnown,
                            onShowCompleteAyahChange = onShowCompleteAyahChange,
                            openExcludedWordsRequestId = excludedWordsRequestId,
                        )
                        AppTab.Quiz -> QuizScreen(
                            progress = progress,
                            onAnswer = onQuizAnswer,
                            pronouncer = pronouncer,
                        )
                        AppTab.Progress -> ProgressScreen(
                            progress = progress,
                            onStudyScopeChange = onStudyScopeChange,
                            onToggleSurah = onToggleSurah,
                            pronouncer = pronouncer,
                        )
                        AppTab.Foundations -> FoundationsScreen(
                            progress = progress,
                            onCompleteAlphabetLesson = onCompleteAlphabetLesson,
                            onStartAlphabetFoundation = onStartAlphabetFoundation,
                            onSkipAlphabetFoundation = onSkipAlphabetFoundation,
                            onCompleteNumberLesson = onCompleteNumberLesson,
                            onStartNumberFoundation = onStartNumberFoundation,
                            pronouncer = pronouncer,
                        )
                        AppTab.Settings -> SettingsScreen(
                            progress = progress,
                            currentLanguage = currentLanguage,
                            onThemeModeChange = onThemeModeChange,
                            onLanguageChange = onLanguageChange,
                            onReminderChange = onReminderChange,
                            onDailyGoalChange = onDailyGoalChange,
                            onAdvancedSettingsVisibleChange = onAdvancedSettingsVisibleChange,
                            onSpacedRepetitionEnabledChange = onSpacedRepetitionEnabledChange,
                            onLockScreenChange = onLockScreenChange,
                            onLockScreenQuizChange = onLockScreenQuizChange,
                            onLockScreenQuizIntervalChange = onLockScreenQuizIntervalChange,
                            onMaximumWordsChange = onMaximumWordsChange,
                            onOpenAppSettings = onOpenAppSettings,
                            onPreviewLockScreen = onPreviewLockScreen,
                            onOpenWebsite = onOpenWebsite,
                            onContactDeveloper = onContactDeveloper,
                            onQuietHoursEnabledChange = onQuietHoursEnabledChange,
                            onQuietHoursChange = onQuietHoursChange,
                            onLockScreenDailyLimitChange = onLockScreenDailyLimitChange,
                            onPauseLockScreenOneHour = onPauseLockScreenOneHour,
                            onPauseLockScreenToday = onPauseLockScreenToday,
                            onResumeLockScreen = onResumeLockScreen,
                            onLockScreenCooldownChange = onLockScreenCooldownChange,
                            onExportBackup = onExportBackup,
                            onImportBackup = onImportBackup,
                            backupImportPreview = backupImportPreview,
                            onConfirmBackupImport = onConfirmBackupImport,
                            onCancelBackupImport = onCancelBackupImport,
                            offlineWordAudioState = offlineWordAudioState,
                            onDownloadOfflineWordAudio = onDownloadOfflineWordAudio,
                            onCancelOfflineWordAudio = onCancelOfflineWordAudio,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabStateProvider(
    selected: AppTab,
    stateHolder: SaveableStateHolder,
    content: @Composable () -> Unit,
) {
    stateHolder.SaveableStateProvider(selected.name, content)
}

private const val NO_STUDY_REQUEST = Long.MIN_VALUE
