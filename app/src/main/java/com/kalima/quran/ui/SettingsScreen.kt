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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.kalima.quran.data.AppThemeMode
import com.kalima.quran.data.DecodedProgressBackup
import com.kalima.quran.data.LearningWordLimiter
import com.kalima.quran.data.LockScreenPerformanceBudget
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.WordRepository
import com.kalima.quran.localization.AppLanguage
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    progress: StudyProgress,
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
    onOpenTextToSpeechSettings: () -> Unit,
    onPreviewLockScreen: () -> Unit,
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
) {
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
                        backupProgress.favoriteIds.size,
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
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
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
                            progress.dailyGoal,
                            progress.dailyGoal,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Slider(
                    value = progress.dailyGoal.toFloat(),
                    onValueChange = { onDailyGoalChange(it.roundToInt()) },
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
                Text(
                    stringResource(R.string.arabic_voice_settings_observation),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = onOpenTextToSpeechSettings) {
                    Text(stringResource(R.string.open_tts_settings))
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
                Text("${progress.quietStartHour}:00", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = progress.quietStartHour.toFloat(),
                    onValueChange = { onQuietHoursChange(it.roundToInt(), progress.quietEndHour) },
                    valueRange = 0f..23f,
                    steps = 22,
                )
                Text("${progress.quietEndHour}:00", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = progress.quietEndHour.toFloat(),
                    onValueChange = { onQuietHoursChange(progress.quietStartHour, it.roundToInt()) },
                    valueRange = 0f..23f,
                    steps = 22,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.daily_card_limit), fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.daily_card_limit_value, progress.lockScreenDailyLimit),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Slider(
                value = progress.lockScreenDailyLimit.toFloat(),
                onValueChange = { onLockScreenDailyLimitChange(it.roundToInt()) },
                valueRange = 5f..50f,
                steps = 44,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.card_cooldown), fontWeight = FontWeight.Bold)
                Text(
                    pluralStringResource(
                        R.plurals.minutes_count,
                        progress.lockScreenCooldownMinutes,
                        progress.lockScreenCooldownMinutes,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Slider(
                value = progress.lockScreenCooldownMinutes.toFloat(),
                onValueChange = { onLockScreenCooldownChange(it.roundToInt()) },
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
                            progress.lockScreenQuizInterval,
                            progress.lockScreenQuizInterval,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Slider(
                    value = progress.lockScreenQuizInterval.toFloat(),
                    onValueChange = { onLockScreenQuizIntervalChange(it.roundToInt()) },
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
