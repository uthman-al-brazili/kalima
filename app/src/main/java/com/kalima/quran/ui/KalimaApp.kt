package com.kalima.quran.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.kalima.quran.audio.OfflineWordAudioDownloadState
import com.kalima.quran.data.AppThemeMode
import com.kalima.quran.data.DecodedProgressBackup
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.QuranWordAudioLocation
import com.kalima.quran.data.QuranVerseAudioLocation
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.UnderstandPathId
import com.kalima.quran.localization.AppLanguage
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
    onSelectUnderstandPath: (UnderstandPathId?) -> Unit,
    onAdvanceUnderstandPath: () -> Unit,
    onToggleSurah: (Int) -> Unit,
    onToggleCustomList: (String) -> Unit,
    onToggleAlreadyKnown: (String) -> Unit,
    onCompleteOnboarding: (StudyScope, Int, Boolean, Boolean) -> Unit,
    onCompleteAlphabetLesson: () -> Unit,
    onAlphabetPracticeAnswer: (String, Boolean) -> Unit,
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
            OnboardingScreen(onComplete = onCompleteOnboarding)
            return@KalimaTheme
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                KalimaTopBar(
                    settingsVisible = settingsVisible,
                    onOpenSettings = { settingsVisible = true },
                    onCloseSettings = { settingsVisible = false },
                )
            },
            bottomBar = {
                if (!settingsVisible) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
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
                } else {
                    TabStateProvider(selected, screenStateHolder) {
                        when (selected) {
                        AppTab.Study -> StudyScreen(
                            progress = progress,
                            onIntroduce = onIntroduce,
                            onAnswer = onAnswer,
                            onCheckpointAnswer = onQuizAnswer,
                            onCurrentWordChange = onCurrentStudyWordChange,
                            onEnableLockScreen = { onLockScreenChange(true) },
                            onOpenExcludedWords = {
                                excludedWordsRequestId += 1L
                                selectedLearnSectionName = LearnSection.Dictionary.name
                                selectedName = AppTab.Learn.name
                            },
                            pronouncer = pronouncer,
                            onToggleCustomList = onToggleCustomList,
                            onToggleAlreadyKnown = onToggleAlreadyKnown,
                            onOpenFoundations = {
                                selectedLearnSectionName = LearnSection.Alphabet.name
                                selectedName = AppTab.Learn.name
                            },
                            onOpenQuiz = {
                                quizUnderstandPathName = progress.activeUnderstandPath?.name
                                selectedLearnSectionName = LearnSection.Quiz.name
                                selectedName = AppTab.Learn.name
                            },
                            onSelectUnderstandPath = onSelectUnderstandPath,
                            onAdvanceUnderstandPath = onAdvanceUnderstandPath,
                            launchTarget = studyLaunchTarget,
                            onLaunchTargetHandled = { requestId ->
                                selectedName = AppTab.Study.name
                                handledStudyRequestId = requestId
                                onStudyLaunchTargetHandled(requestId)
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
                        )
                        AppTab.Learn -> LearnScreen(
                            progress = progress,
                            selectedSection = selectedLearnSection,
                            onSectionSelected = {
                                selectedLearnSectionName = it.name
                                if (it == LearnSection.Quiz) quizUnderstandPathName = null
                            },
                            pronouncer = pronouncer,
                            onToggleCustomList = onToggleCustomList,
                            onToggleAlreadyKnown = onToggleAlreadyKnown,
                            onShowCompleteAyahChange = onShowCompleteAyahChange,
                            openExcludedWordsRequestId = excludedWordsRequestId,
                            onQuizAnswer = onQuizAnswer,
                            quizUnderstandPath = quizUnderstandPathName?.let { stored ->
                                UnderstandPathId.entries.firstOrNull { it.name == stored }
                            },
                            onCompleteAlphabetLesson = onCompleteAlphabetLesson,
                            onAlphabetPracticeAnswer = onAlphabetPracticeAnswer,
                            onStartAlphabetFoundation = onStartAlphabetFoundation,
                            onSkipAlphabetFoundation = onSkipAlphabetFoundation,
                            onCompleteNumberLesson = onCompleteNumberLesson,
                            onStartNumberFoundation = onStartNumberFoundation,
                        )
                        AppTab.Progress -> ProgressScreen(
                            progress = progress,
                            onStudyScopeChange = onStudyScopeChange,
                            onToggleSurah = onToggleSurah,
                            onSelectUnderstandPath = onSelectUnderstandPath,
                            onAdvanceUnderstandPath = onAdvanceUnderstandPath,
                            pronouncer = pronouncer,
                        )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KalimaTopBar(
    settingsVisible: Boolean,
    onOpenSettings: () -> Unit,
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
            if (settingsVisible) {
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
                Spacer(Modifier.weight(1f))
                ArabicIndicClock(modifier = Modifier.padding(horizontal = 8.dp))
            } else {
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                ArabicIndicClock(modifier = Modifier.padding(horizontal = 8.dp))
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = stringResource(R.string.tab_settings),
                    )
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
private const val SETTINGS_STATE_KEY = "Settings"
