package com.kalima.quran.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.audio.OfflineWordAudioDownloadState
import com.kalima.quran.audio.OfflineWordAudioManager
import com.kalima.quran.data.AppThemeMode
import com.kalima.quran.data.DecodedProgressBackup
import com.kalima.quran.data.SessionLevel
import com.kalima.quran.data.StudyPlan
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.QuranVerseAudioLocation
import com.kalima.quran.data.QuranWordAudioLocation
import com.kalima.quran.data.WordRepository
import com.kalima.quran.data.limitNewWords
import com.kalima.quran.localization.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    progress: StudyProgress,
    showTitle: Boolean = true,
    currentLanguage: AppLanguage,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onReminderChange: (Boolean) -> Unit,
    onSessionLevelChange: (SessionLevel) -> Unit,
    onAdvancedSettingsVisibleChange: (Boolean) -> Unit,
    onSpacedRepetitionEnabledChange: (Boolean) -> Unit,
    onLockScreenChange: (Boolean) -> Unit,
    onLockScreenQuizChange: (Boolean) -> Unit,
    onLockScreenQuizIntervalChange: (Int) -> Unit,
    onMaximumWordsChange: (Int) -> Unit,
    onOpenAppSettings: () -> Unit,
    onPreviewLockScreen: () -> Unit,
    onOpenWebsite: () -> Unit,
    onContactDeveloper: () -> Unit,
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
) {
    val audioSelectionKey = remember(
        progress.studyScopes,
        progress.selectedSurahs,
        progress.customStudyIds,
        progress.activeUnderstandPath,
        progress.activeUnderstandPathStage,
        progress.maximumWords,
        progress.learnedIds,
        progress.reviewingIds,
        progress.alreadyKnownIds,
    ) {
        OfflineAudioSelectionKey(
            progress.studyScopes.map(StudyScope::name).sorted().joinToString(","),
            progress.selectedSurahs,
            progress.customStudyIds,
            progress.activeUnderstandPath?.name,
            progress.activeUnderstandPathStage,
            progress.maximumWords,
            progress.learnedIds,
            progress.reviewingIds,
            progress.alreadyKnownIds,
        )
    }
    val audioSelection by produceState(
        initialValue = OfflineAudioSelection(),
        key1 = audioSelectionKey,
    ) {
        value = withContext(Dispatchers.Default) {
            val wordLocations = progress.limitNewWords(
                StudyPlan.calculate(progress, WordRepository.words).combinedWords,
            ).mapNotNull { it.audioLocation }
                .distinctBy(QuranWordAudioLocation::fileName)
            val verseLocations = wordLocations
                .map(QuranVerseAudioLocation::fromWord)
                .distinctBy(QuranVerseAudioLocation::fileName)
            OfflineAudioSelection(wordLocations, verseLocations, ready = true)
        }
    }
    val offlineWordAudioLocations = audioSelection.wordLocations
    val offlineVerseAudioLocations = audioSelection.verseLocations
    val estimatedAudioMegabytes = OfflineWordAudioManager.estimatedMegabytes(
        wordCount = offlineWordAudioLocations.size,
        verseCount = offlineVerseAudioLocations.size,
    )
    var showAudioDownloadConfirmation by rememberSaveable { mutableStateOf(false) }

    if (showAudioDownloadConfirmation) {
        AlertDialog(
            onDismissRequest = { showAudioDownloadConfirmation = false },
            title = { Text(stringResource(R.string.offline_audio_download_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.offline_audio_download_confirmation,
                        offlineWordAudioLocations.size,
                        offlineVerseAudioLocations.size,
                        estimatedAudioMegabytes,
                    ),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAudioDownloadConfirmation = false
                        onDownloadOfflineWordAudio(
                            offlineWordAudioLocations,
                            offlineVerseAudioLocations,
                        )
                    },
                ) {
                    Text(stringResource(R.string.download))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAudioDownloadConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (backupImportPreview != null) {
        val backupProgress = backupImportPreview.progress
        AlertDialog(
            onDismissRequest = onCancelBackupImport,
            title = { Text(stringResource(R.string.backup_preview_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.backup_preview_summary,
                        backupImportPreview.metadata.createdAt.toString(),
                        backupImportPreview.metadata.appVersion,
                        backupProgress.learnedIds.size,
                        backupProgress.reviewingIds.size,
                        backupProgress.alreadyKnownIds.size,
                        backupProgress.customStudyIds.size,
                    ),
                )
            },
            confirmButton = {
                Button(onClick = onConfirmBackupImport) {
                    Text(stringResource(R.string.restore_backup))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelBackupImport) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        if (showTitle) {
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            stringResource(R.string.settings_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(22.dp))

        SettingsSectionTitle(R.string.appearance)
        Text(
            stringResource(R.string.appearance_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(10.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeModeChip(
                    label = stringResource(R.string.theme_auto),
                    selected = progress.themeMode == AppThemeMode.Auto,
                    onClick = { onThemeModeChange(AppThemeMode.Auto) },
                    modifier = Modifier.weight(1f),
                )
                ThemeModeChip(
                    label = stringResource(R.string.theme_light),
                    selected = progress.themeMode == AppThemeMode.Light,
                    onClick = { onThemeModeChange(AppThemeMode.Light) },
                    modifier = Modifier.weight(1f),
                )
                ThemeModeChip(
                    label = stringResource(R.string.theme_dark),
                    selected = progress.themeMode == AppThemeMode.Dark,
                    onClick = { onThemeModeChange(AppThemeMode.Dark) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(22.dp))

        SettingsSectionTitle(R.string.language)
        Text(
            stringResource(R.string.language_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(10.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = currentLanguage == AppLanguage.English,
                    onClick = { onLanguageChange(AppLanguage.English) },
                    label = { Text(stringResource(R.string.language_english)) },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = currentLanguage == AppLanguage.Portuguese,
                    onClick = { onLanguageChange(AppLanguage.Portuguese) },
                    label = { Text(stringResource(R.string.language_portuguese)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(22.dp))

        SettingsSectionTitle(R.string.routine)
        Spacer(Modifier.height(10.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 1.dp,
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.daily_reminder), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.daily_reminder_description),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(checked = progress.reminderEnabled, onCheckedChange = onReminderChange)
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(R.string.default_session_level),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.default_session_level_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                SessionLevelSelector(
                    selected = progress.sessionLevel,
                    onSelected = onSessionLevelChange,
                )
            }
        }
        Spacer(Modifier.height(22.dp))

        SettingsSectionTitle(R.string.audio_title)
        Spacer(Modifier.height(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 1.dp,
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    stringResource(R.string.audio_disclosure),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                if (offlineWordAudioState.running) {
                    val progressFraction = if (offlineWordAudioState.total == 0) {
                        0f
                    } else {
                        offlineWordAudioState.completed.toFloat() / offlineWordAudioState.total
                    }
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier.fillMaxWidth(),
                        drawStopIndicator = {},
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(
                            R.string.offline_audio_download_progress,
                            offlineWordAudioState.completed,
                            offlineWordAudioState.total,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = onCancelOfflineWordAudio) {
                        Text(stringResource(R.string.cancel_download))
                    }
                } else {
                    Button(
                        onClick = { showAudioDownloadConfirmation = true },
                        enabled = audioSelection.ready && offlineWordAudioLocations.isNotEmpty(),
                    ) {
                        Text(stringResource(R.string.download_selected_audio))
                    }
                    Text(
                        if (audioSelection.ready) {
                            stringResource(
                                R.string.offline_audio_download_estimate,
                                offlineWordAudioLocations.size,
                                offlineVerseAudioLocations.size,
                                estimatedAudioMegabytes,
                            )
                        } else {
                            stringResource(R.string.preparing_audio_selection)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (offlineWordAudioState.total > 0) {
                        Text(
                            stringResource(
                                R.string.offline_audio_download_result,
                                offlineWordAudioState.downloaded,
                                offlineWordAudioState.alreadyAvailable,
                                offlineWordAudioState.failed,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))

        SettingsSectionTitle(R.string.backup_title)
        Text(
            stringResource(R.string.backup_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 1.dp,
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text(
                    stringResource(R.string.backup_local_note),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onExportBackup) {
                        Text(stringResource(R.string.export_backup))
                    }
                    TextButton(onClick = onImportBackup) {
                        Text(stringResource(R.string.import_backup))
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))

        SettingsSectionTitle(R.string.support_title)
        Spacer(Modifier.height(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    stringResource(R.string.support_description),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onOpenWebsite) {
                    Text(stringResource(R.string.open_website))
                }
                TextButton(onClick = onContactDeveloper) {
                    Text(stringResource(R.string.contact_developer))
                }
            }
        }
        Spacer(Modifier.height(22.dp))

        SettingsSectionTitle(R.string.privacy_title)
        Spacer(Modifier.height(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(
                stringResource(R.string.privacy_summary),
                modifier = Modifier.padding(18.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(22.dp))

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.show_advanced_settings),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.advanced_settings_description),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = progress.advancedSettingsVisible,
                    onCheckedChange = onAdvancedSettingsVisibleChange,
                )
            }
        }

        if (progress.advancedSettingsVisible) {
            Spacer(Modifier.height(22.dp))
            AdvancedSettings(
                progress = progress,
                onLockScreenChange = onLockScreenChange,
                onSpacedRepetitionEnabledChange = onSpacedRepetitionEnabledChange,
                onLockScreenQuizChange = onLockScreenQuizChange,
                onLockScreenQuizIntervalChange = onLockScreenQuizIntervalChange,
                onMaximumWordsChange = onMaximumWordsChange,
                onOpenAppSettings = onOpenAppSettings,
                onPreviewLockScreen = onPreviewLockScreen,
                onQuietHoursEnabledChange = onQuietHoursEnabledChange,
                onQuietHoursChange = onQuietHoursChange,
                onLockScreenDailyLimitChange = onLockScreenDailyLimitChange,
                onPauseLockScreenOneHour = onPauseLockScreenOneHour,
                onPauseLockScreenToday = onPauseLockScreenToday,
                onResumeLockScreen = onResumeLockScreen,
                onLockScreenCooldownChange = onLockScreenCooldownChange,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
