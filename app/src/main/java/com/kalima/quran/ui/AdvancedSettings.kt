package com.kalima.quran.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableFloatStateOf
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
import com.kalima.quran.data.LearningWordLimiter
import com.kalima.quran.data.LockScreenPerformanceBudget
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.QuranVerseAudioLocation
import com.kalima.quran.data.QuranWordAudioLocation
import com.kalima.quran.data.WordRepository
import kotlin.math.roundToInt

@Composable
internal fun AdvancedSettings(
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
internal fun SettingsSectionTitle(titleRes: Int) {
    Text(
        stringResource(titleRes),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
}

internal data class OfflineAudioSelectionKey(
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

internal data class OfflineAudioSelection(
    val wordLocations: List<QuranWordAudioLocation> = emptyList(),
    val verseLocations: List<QuranVerseAudioLocation> = emptyList(),
    val ready: Boolean = false,
)

@Composable
internal fun ThemeModeChip(
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
