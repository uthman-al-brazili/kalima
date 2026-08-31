package com.kalima.quran.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import androidx.annotation.StringRes
import androidx.annotation.DrawableRes
import com.kalima.quran.R
import com.kalima.quran.data.UnderstandPathId
import com.kalima.quran.ui.theme.KalimaTheme

private enum class AppTab(@param:StringRes val labelRes: Int, @param:DrawableRes val iconRes: Int) {
    Study(R.string.tab_study, R.drawable.ic_study),
    Quran(R.string.tab_quran, R.drawable.ic_quran),
    Learn(R.string.tab_learn, R.drawable.ic_library),
    Progress(R.string.tab_progress, R.drawable.ic_progress),
}

data class StudyLaunchTarget(val wordId: String, val requestId: Long)

@Composable
fun KalimaApp(
    state: KalimaUiState,
    actions: KalimaAppActions,
) {
    val progress = state.progress
    val studyActions = actions.study
    val learningActions = actions.learning
    val preferences = actions.preferences
    val lockScreen = actions.lockScreen
    val backup = actions.backup
    val offlineAudio = actions.offlineAudio
    val external = actions.external
    val studyLaunchTarget = state.studyLaunchTarget
    var selectedName by rememberSaveable { mutableStateOf(AppTab.Study.name) }
    var selectedLearnSectionName by rememberSaveable { mutableStateOf(LearnSection.Dictionary.name) }
    var quizUnderstandPathName by rememberSaveable { mutableStateOf<String?>(null) }
    var settingsVisible by rememberSaveable { mutableStateOf(false) }
    var handledStudyRequestId by rememberSaveable { mutableLongStateOf(NO_STUDY_REQUEST) }
    var excludedWordsRequestId by rememberSaveable { mutableLongStateOf(0L) }
    val hasPendingStudyRequest = studyLaunchTarget != null &&
        studyLaunchTarget.requestId != handledStudyRequestId
    val selected = if (hasPendingStudyRequest) {
        AppTab.Study
    } else {
        AppTab.entries.firstOrNull { it.name == selectedName } ?: AppTab.Study
    }
    val selectedLearnSection = LearnSection.entries
        .firstOrNull { it.name == selectedLearnSectionName } ?: LearnSection.Dictionary
    val pronouncer = rememberArabicPronouncer()
    val screenStateHolder = rememberSaveableStateHolder()

    LaunchedEffect(studyLaunchTarget?.requestId) {
        val target = studyLaunchTarget ?: return@LaunchedEffect
        if (target.requestId != handledStudyRequestId) {
            selectedName = AppTab.Study.name
            settingsVisible = false
            handledStudyRequestId = target.requestId
        }
    }

    BackHandler(enabled = settingsVisible) {
        settingsVisible = false
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
            OnboardingScreen(onComplete = learningActions.onCompleteOnboarding)
            return@KalimaTheme
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (settingsVisible) {
                    SettingsTopBar(onCloseSettings = { settingsVisible = false })
                }
            },
            bottomBar = {
                if (!settingsVisible) {
                    NavigationBar(
                        modifier = Modifier.height(64.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        AppTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selected == tab,
                                onClick = {
                                    selectedName = tab.name
                                    settingsVisible = false
                                },
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
                                        autoSize = TextAutoSize.StepBased(
                                            minFontSize = 8.sp,
                                            maxFontSize = 11.sp,
                                            stepSize = 0.5.sp,
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                                alwaysShowLabel = true,
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                if (settingsVisible) {
                    screenStateHolder.SaveableStateProvider(SETTINGS_STATE_KEY) {
                        SettingsScreen(
                            progress = progress,
                            showTitle = false,
                            currentLanguage = state.currentLanguage,
                            onThemeModeChange = preferences.onThemeModeChange,
                            onLanguageChange = preferences.onLanguageChange,
                            onReminderChange = preferences.onReminderChange,
                            onSessionLevelChange = studyActions.onSessionLevelChange,
                            onAdvancedSettingsVisibleChange = preferences.onAdvancedSettingsVisibleChange,
                            onSpacedRepetitionEnabledChange = preferences.onSpacedRepetitionEnabledChange,
                            onLockScreenChange = lockScreen.onEnabledChange,
                            onLockScreenQuizChange = lockScreen.onQuizEnabledChange,
                            onLockScreenQuizIntervalChange = lockScreen.onQuizIntervalChange,
                            onMaximumWordsChange = preferences.onMaximumWordsChange,
                            onOpenAppSettings = lockScreen.onOpenAppSettings,
                            onPreviewLockScreen = lockScreen.onPreview,
                            onOpenWebsite = external.onOpenWebsite,
                            onContactDeveloper = external.onContactDeveloper,
                            onQuietHoursEnabledChange = lockScreen.onQuietHoursEnabledChange,
                            onQuietHoursChange = lockScreen.onQuietHoursChange,
                            onLockScreenDailyLimitChange = lockScreen.onDailyLimitChange,
                            onPauseLockScreenOneHour = lockScreen.onPauseOneHour,
                            onPauseLockScreenToday = lockScreen.onPauseToday,
                            onResumeLockScreen = lockScreen.onResume,
                            onLockScreenCooldownChange = lockScreen.onCooldownChange,
                            onExportBackup = backup.onExport,
                            onImportBackup = backup.onImport,
                            backupImportPreview = state.backupImportPreview,
                            onConfirmBackupImport = backup.onConfirmImport,
                            onCancelBackupImport = backup.onCancelImport,
                            offlineWordAudioState = state.offlineWordAudioState,
                            onDownloadOfflineWordAudio = offlineAudio.onDownload,
                            onCancelOfflineWordAudio = offlineAudio.onCancel,
                        )
                    }
                } else {
                    TabStateProvider(selected, screenStateHolder) {
                        when (selected) {
                        AppTab.Study -> StudyScreen(
                            progress = progress,
                            onIntroduce = studyActions.onIntroduce,
                            onAnswer = studyActions.onAnswer,
                            onCheckpointAnswer = studyActions.onQuizAnswer,
                            onCurrentWordChange = studyActions.onCurrentWordChange,
                            onEnableLockScreen = { lockScreen.onEnabledChange(true) },
                            onOpenExcludedWords = {
                                excludedWordsRequestId += 1L
                                selectedLearnSectionName = LearnSection.Dictionary.name
                                selectedName = AppTab.Learn.name
                            },
                            pronouncer = pronouncer,
                            onToggleCustomList = learningActions.onToggleCustomList,
                            onToggleAlreadyKnown = learningActions.onToggleAlreadyKnown,
                            onOpenQuiz = {
                                quizUnderstandPathName = progress.activeUnderstandPath?.name
                                selectedLearnSectionName = LearnSection.Quiz.name
                                selectedName = AppTab.Learn.name
                            },
                            onSessionLevelChange = studyActions.onSessionLevelChange,
                            onAdvanceUnderstandPath = studyActions.onAdvanceUnderstandPath,
                            onOpenSettings = { settingsVisible = true },
                            launchTarget = studyLaunchTarget,
                            onLaunchTargetHandled = { requestId ->
                                selectedName = AppTab.Study.name
                                handledStudyRequestId = requestId
                                studyActions.onLaunchTargetHandled(requestId)
                            },
                        )
                        AppTab.Quran -> QuranReaderScreen(
                            progress = progress,
                            fontSizeSp = progress.quranFontSizeSp,
                            customStudyIds = progress.customStudyIds,
                            learningOverlayEnabled = progress.quranLearningOverlayEnabled,
                            onFontSizeChange = preferences.onQuranFontSizeChange,
                            onLearningOverlayChange = preferences.onQuranLearningOverlayChange,
                            onToggleCustomList = learningActions.onToggleCustomList,
                            onOpenSettings = { settingsVisible = true },
                        )
                        AppTab.Learn -> LearnScreen(
                            progress = progress,
                            selectedSection = selectedLearnSection,
                            onSectionSelected = {
                                selectedLearnSectionName = it.name
                                if (it == LearnSection.Quiz) quizUnderstandPathName = null
                            },
                            pronouncer = pronouncer,
                            onToggleCustomList = learningActions.onToggleCustomList,
                            onToggleAlreadyKnown = learningActions.onToggleAlreadyKnown,
                            onShowCompleteAyahChange = learningActions.onShowCompleteAyahChange,
                            openExcludedWordsRequestId = excludedWordsRequestId,
                            onQuizAnswer = studyActions.onQuizAnswer,
                            quizUnderstandPath = quizUnderstandPathName?.let { stored ->
                                UnderstandPathId.entries.firstOrNull { it.name == stored }
                            },
                            onChooseQuizMode = { quizUnderstandPathName = null },
                            onOpenSettings = { settingsVisible = true },
                        )
                        AppTab.Progress -> ProgressScreen(
                            progress = progress,
                            onStudyScopeChange = learningActions.onStudyScopeChange,
                            onSelectUnderstandPath = learningActions.onSelectUnderstandPath,
                            onAdvanceUnderstandPath = studyActions.onAdvanceUnderstandPath,
                            onToggleSurah = learningActions.onToggleSurah,
                            pronouncer = pronouncer,
                            onOpenSettings = { settingsVisible = true },
                        )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTopBar(
    onCloseSettings: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCloseSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.navigate_back),
                )
            }
            Text(
                text = stringResource(R.string.tab_settings),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
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
private const val SETTINGS_STATE_KEY = "Settings"
