package com.kalima.quran.ui

import androidx.compose.runtime.Immutable
import com.kalima.quran.audio.OfflineWordAudioDownloadState
import com.kalima.quran.data.AppThemeMode
import com.kalima.quran.data.DecodedProgressBackup
import com.kalima.quran.data.QuranVerseAudioLocation
import com.kalima.quran.data.QuranWordAudioLocation
import com.kalima.quran.data.SessionLevel
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.UnderstandPathId
import com.kalima.quran.localization.AppLanguage

@Immutable
data class KalimaUiState(
    val progress: StudyProgress,
    val currentLanguage: AppLanguage,
    val backupImportPreview: DecodedProgressBackup?,
    val offlineWordAudioState: OfflineWordAudioDownloadState,
    val studyLaunchTarget: StudyLaunchTarget? = null,
)

@Immutable
data class KalimaAppActions(
    val study: StudyActions,
    val learning: LearningActions,
    val preferences: PreferenceActions,
    val lockScreen: LockScreenActions,
    val backup: BackupActions,
    val offlineAudio: OfflineAudioActions,
    val external: ExternalActions,
)

@Immutable
data class StudyActions(
    val onIntroduce: (String) -> Unit,
    val onAnswer: (String, Boolean) -> Unit,
    val onCurrentWordChange: (String) -> Unit,
    val onQuizAnswer: (String, Boolean) -> Unit,
    val onSessionLevelChange: (SessionLevel) -> Unit,
    val onAdvanceUnderstandPath: () -> Unit,
    val onLaunchTargetHandled: (Long) -> Unit = {},
)

@Immutable
data class LearningActions(
    val onStudyScopeChange: (StudyScope) -> Unit,
    val onSelectUnderstandPath: (UnderstandPathId?) -> Unit,
    val onToggleSurah: (Int) -> Unit,
    val onToggleCustomList: (String) -> Unit,
    val onToggleAlreadyKnown: (String) -> Unit,
    val onShowCompleteAyahChange: (Boolean) -> Unit,
    val onCompleteOnboarding: (StudyScope, UnderstandPathId?, SessionLevel) -> Unit,
)

@Immutable
data class PreferenceActions(
    val onReminderChange: (Boolean) -> Unit,
    val onMaximumWordsChange: (Int) -> Unit,
    val onThemeModeChange: (AppThemeMode) -> Unit,
    val onQuranFontSizeChange: (Int) -> Unit,
    val onQuranLearningOverlayChange: (Boolean) -> Unit,
    val onAdvancedSettingsVisibleChange: (Boolean) -> Unit,
    val onSpacedRepetitionEnabledChange: (Boolean) -> Unit,
    val onLanguageChange: (AppLanguage) -> Unit,
)

@Immutable
data class LockScreenActions(
    val onEnabledChange: (Boolean) -> Unit,
    val onQuizEnabledChange: (Boolean) -> Unit,
    val onQuizIntervalChange: (Int) -> Unit,
    val onOpenAppSettings: () -> Unit,
    val onPreview: () -> Unit,
    val onQuietHoursEnabledChange: (Boolean) -> Unit,
    val onQuietHoursChange: (Int, Int) -> Unit,
    val onDailyLimitChange: (Int) -> Unit,
    val onPauseOneHour: () -> Unit,
    val onPauseToday: () -> Unit,
    val onResume: () -> Unit,
    val onCooldownChange: (Int) -> Unit,
)

@Immutable
data class BackupActions(
    val onExport: () -> Unit,
    val onImport: () -> Unit,
    val onConfirmImport: () -> Unit,
    val onCancelImport: () -> Unit,
)

@Immutable
data class OfflineAudioActions(
    val onDownload: (List<QuranWordAudioLocation>, List<QuranVerseAudioLocation>) -> Unit,
    val onCancel: () -> Unit,
)

@Immutable
data class ExternalActions(
    val onOpenWebsite: () -> Unit,
    val onContactDeveloper: () -> Unit,
)
