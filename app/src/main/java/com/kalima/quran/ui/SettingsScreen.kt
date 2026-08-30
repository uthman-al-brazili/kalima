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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.audio.OfflineWordAudioDownloadState
import com.kalima.quran.audio.OfflineWordAudioManager
import com.kalima.quran.data.AppThemeMode
import com.kalima.quran.data.DecodedProgressBackup
import com.kalima.quran.data.LearningWordLimiter
import com.kalima.quran.data.LockScreenPerformanceBudget
import com.kalima.quran.data.StudyPlan
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.QuranVerseAudioLocation
import com.kalima.quran.data.QuranWordAudioLocation
import com.kalima.quran.data.WordRepository
import com.kalima.quran.data.limitNewWords
import com.kalima.quran.localization.AppLanguage
import kotlin.math.roundToInt
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
    onDailyGoalChange: (Int) -> Unit,
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
    var dailyGoalSlider by rememberSaveable(progress.dailyGoal) {
        mutableFloatStateOf(progress.dailyGoal.toFloat())
    }

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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.daily_goal), fontWeight = FontWeight.Bold)
                    Text(
                        pluralStringResource(
                            R.plurals.words_count,
                            dailyGoalSlider.roundToInt(),
                            dailyGoalSlider.roundToInt(),
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Slider(
                    value = dailyGoalSlider,
                    onValueChange = { dailyGoalSlider = it },
                    onValueChangeFinished = {
                        onDailyGoalChange(dailyGoalSlider.roundToInt())
                    },
                    valueRange = 3f..20f,
                    steps = 16,
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

@Composable
private fun AdvancedSettings(
    progress: StudyProgress,
    onLockScreenChange: (Boolean) -> Unit,
    onSpacedRepetitionEnabledChange: (Boolean) -> Unit,
    onLockScreenQuizChange: (Boolean) -> Unit,
    onLockScreenQuizIntervalChange: (Int) -> Unit,
    onMaximumWordsChange: (Int) -> Unit,
    onOpenAppSettings: () -> Unit,
    onPreviewLockScreen: () -> Unit,
    onQuietHoursEnabledChange: (Boolean) -> Unit,
    onQuietHoursChange: (Int, Int) -> Unit,
    onLockScreenDailyLimitChange: (Int) -> Unit,
    onPauseLockScreenOneHour: () -> Unit,
    onPauseLockScreenToday: () -> Unit,
    onResumeLockScreen: () -> Unit,
    onLockScreenCooldownChange: (Int) -> Unit,
) {
    var maximumWordsText by rememberSaveable(progress.maximumWords) {
        mutableStateOf(
            (progress.maximumWords.takeIf { it != LearningWordLimiter.UNLIMITED }
                ?: LearningWordLimiter.DEFAULT_LIMIT).toString(),
        )
    }
    val enteredMaximum = maximumWordsText.toIntOrNull()
    val validMaximum = enteredMaximum != null &&
        enteredMaximum in LearningWordLimiter.MINIMUM_LIMIT..WordRepository.words.size
    var quietStartSlider by rememberSaveable(progress.quietStartHour) {
        mutableFloatStateOf(progress.quietStartHour.toFloat())
    }
    var quietEndSlider by rememberSaveable(progress.quietEndHour) {
        mutableFloatStateOf(progress.quietEndHour.toFloat())
    }
    var dailyLimitSlider by rememberSaveable(progress.lockScreenDailyLimit) {
        mutableFloatStateOf(progress.lockScreenDailyLimit.toFloat())
    }
    var cooldownSlider by rememberSaveable(progress.lockScreenCooldownMinutes) {
        mutableFloatStateOf(progress.lockScreenCooldownMinutes.toFloat())
    }
    var quizIntervalSlider by rememberSaveable(progress.lockScreenQuizInterval) {
        mutableFloatStateOf(progress.lockScreenQuizInterval.toFloat())
    }

    SettingsSectionTitle(R.string.study_scheduling)
    Spacer(Modifier.height(10.dp))
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.spaced_repetition), fontWeight = FontWeight.Bold)
                Text(
                    stringResource(
                        if (progress.spacedRepetitionEnabled) {
                            R.string.spaced_repetition_enabled_description
                        } else {
                            R.string.spaced_repetition_disabled_observation
                        },
                    ),
                    color = if (progress.spacedRepetitionEnabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = progress.spacedRepetitionEnabled,
                onCheckedChange = onSpacedRepetitionEnabledChange,
            )
        }
    }
    Spacer(Modifier.height(22.dp))

    SettingsSectionTitle(R.string.lock_screen_features)
    Text(
        stringResource(R.string.lock_screen_features_description),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(Modifier.height(10.dp))
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.lock_screen_study), fontWeight = FontWeight.Bold)
                    Text(
                        if (progress.lockScreenEnabled) {
                            stringResource(R.string.lock_screen_study_enabled)
                        } else {
                            stringResource(R.string.lock_screen_study_disabled)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = progress.lockScreenEnabled,
                    onCheckedChange = onLockScreenChange,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onPreviewLockScreen) {
                    Text(stringResource(R.string.preview_card))
                }
                TextButton(onClick = onOpenAppSettings) {
                    Text(stringResource(R.string.android_app_settings))
                }
            }
            Text(
                if (progress.lockScreenEnabled) {
                    stringResource(
                        R.string.lock_health_ok,
                        progress.lockScreenCardsToday,
                        progress.lockScreenDailyLimit,
                    )
                } else {
                    stringResource(R.string.lock_health_off)
                },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(
                    R.string.lock_safety_status,
                    progress.lastLockScreenLatencyMs?.toString() ?: "—",
                    LockScreenPerformanceBudget.MAX_LAUNCH_LATENCY_MS,
                    progress.lockScreenSafetySkips,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.quiet_hours), fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(
                            R.string.quiet_hours_summary,
                            progress.quietStartHour,
                            progress.quietEndHour,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = progress.quietHoursEnabled,
                    onCheckedChange = onQuietHoursEnabledChange,
                )
            }
            if (progress.quietHoursEnabled) {
                Text("${quietStartSlider.roundToInt()}:00", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = quietStartSlider,
                    onValueChange = { quietStartSlider = it },
                    onValueChangeFinished = {
                        onQuietHoursChange(
                            quietStartSlider.roundToInt(),
                            quietEndSlider.roundToInt(),
                        )
                    },
                    valueRange = 0f..23f,
                    steps = 22,
                )
                Text("${quietEndSlider.roundToInt()}:00", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = quietEndSlider,
                    onValueChange = { quietEndSlider = it },
                    onValueChangeFinished = {
                        onQuietHoursChange(
                            quietStartSlider.roundToInt(),
                            quietEndSlider.roundToInt(),
                        )
                    },
                    valueRange = 0f..23f,
                    steps = 22,
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.daily_card_limit),
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.daily_card_limit_value, dailyLimitSlider.roundToInt()),
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            Slider(
                value = dailyLimitSlider,
                onValueChange = { dailyLimitSlider = it },
                onValueChangeFinished = {
                    onLockScreenDailyLimitChange(dailyLimitSlider.roundToInt())
                },
                valueRange = 5f..50f,
                steps = 44,
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.card_cooldown),
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    pluralStringResource(
                        R.plurals.minutes_count,
                        cooldownSlider.roundToInt(),
                        cooldownSlider.roundToInt(),
                    ),
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            Slider(
                value = cooldownSlider,
                onValueChange = { cooldownSlider = it },
                onValueChangeFinished = {
                    onLockScreenCooldownChange(cooldownSlider.roundToInt())
                },
                valueRange = 0f..60f,
                steps = 59,
            )
            if (progress.lockScreenPausedUntil != null && progress.lockScreenPausedUntil > java.time.Instant.now()) {
                Text(
                    stringResource(R.string.cards_paused),
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onResumeLockScreen) {
                    Text(stringResource(R.string.resume_cards))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onPauseLockScreenOneHour) {
                        Text(stringResource(R.string.pause_one_hour))
                    }
                    TextButton(onClick = onPauseLockScreenToday) {
                        Text(stringResource(R.string.pause_today))
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.lock_screen_quiz), fontWeight = FontWeight.Bold)
                    Text(
                        if (progress.lockScreenQuizEnabled) {
                            pluralStringResource(
                                R.plurals.lock_screen_quiz_enabled,
                                progress.lockScreenQuizInterval,
                                progress.lockScreenQuizInterval,
                            )
                        } else {
                            stringResource(R.string.lock_screen_quiz_disabled)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = progress.lockScreenQuizEnabled,
                    onCheckedChange = onLockScreenQuizChange,
                )
            }
            if (progress.lockScreenQuizEnabled) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.interval), fontWeight = FontWeight.SemiBold)
                    Text(
                        pluralStringResource(
                            R.plurals.words_count,
                            quizIntervalSlider.roundToInt(),
                            quizIntervalSlider.roundToInt(),
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Slider(
                    value = quizIntervalSlider,
                    onValueChange = { quizIntervalSlider = it },
                    onValueChangeFinished = {
                        onLockScreenQuizIntervalChange(quizIntervalSlider.roundToInt())
                    },
                    valueRange = 1f..10f,
                    steps = 8,
                )
            }
        }
    }
    Spacer(Modifier.height(22.dp))

    SettingsSectionTitle(R.string.learning_limit)
    Text(
        stringResource(R.string.learning_limit_description),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(Modifier.height(10.dp))
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.limit_new_words),
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                )
                Switch(
                    checked = progress.maximumWords != LearningWordLimiter.UNLIMITED,
                    onCheckedChange = { enabled ->
                        onMaximumWordsChange(
                            if (enabled) {
                                enteredMaximum?.takeIf { validMaximum }
                                    ?: LearningWordLimiter.DEFAULT_LIMIT
                            } else {
                                LearningWordLimiter.UNLIMITED
                            },
                        )
                    },
                )
            }
            if (progress.maximumWords != LearningWordLimiter.UNLIMITED) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = maximumWordsText,
                    onValueChange = { value ->
                        if (
                            value.all(Char::isDigit) &&
                            value.length <= WordRepository.words.size.toString().length
                        ) {
                            maximumWordsText = value
                            value.toIntOrNull()
                                ?.takeIf {
                                    it in LearningWordLimiter.MINIMUM_LIMIT..WordRepository.words.size
                                }
                                ?.let(onMaximumWordsChange)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.maximum_words)) },
                    supportingText = {
                        Text(
                            stringResource(
                                R.string.learning_limit_range,
                                LearningWordLimiter.MINIMUM_LIMIT,
                                WordRepository.words.size,
                            ),
                        )
                    },
                    isError = !validMaximum,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(titleRes: Int) {
    Text(
        stringResource(titleRes),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
}

private data class OfflineAudioSelectionKey(
    val studyScope: String,
    val selectedSurahs: Set<Int>,
    val customStudyIds: Set<String>,
    val activeUnderstandPath: String?,
    val activeUnderstandPathStage: Int,
    val maximumWords: Int,
    val learnedIds: Set<String>,
    val reviewingIds: Set<String>,
    val alreadyKnownIds: Set<String>,
)

private data class OfflineAudioSelection(
    val wordLocations: List<QuranWordAudioLocation> = emptyList(),
    val verseLocations: List<QuranVerseAudioLocation> = emptyList(),
    val ready: Boolean = false,
)

@Composable
private fun ThemeModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
    )
}
