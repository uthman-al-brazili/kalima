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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.LearningWordLimiter
import com.kalima.quran.data.WordRepository
import com.kalima.quran.data.limitNewWords
import com.kalima.quran.localization.AppLanguage
import com.kalima.quran.ui.theme.Forest
import com.kalima.quran.ui.theme.Gold
import com.kalima.quran.ui.theme.Muted
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(
    progress: StudyProgress,
    onLockScreenChange: (Boolean) -> Unit,
    onLockScreenQuizChange: (Boolean) -> Unit,
    onLockScreenQuizIntervalChange: (Int) -> Unit,
    onReminderChange: (Boolean) -> Unit,
    onDailyGoalChange: (Int) -> Unit,
    onMaximumWordsChange: (Int) -> Unit,
    onStudyScopeChange: (StudyScope) -> Unit,
    onToggleSurah: (Int) -> Unit,
    onOpenAppSettings: () -> Unit,
    onPreviewLockScreen: () -> Unit,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    val selectionKey = progress.selectedSurahs.sorted().joinToString(",")
    val activeWords = remember(progress.studyScope, selectionKey) {
        WordRepository.wordsFor(progress.studyScope, progress.selectedSurahs)
    }
    val learnedInScope = remember(activeWords, progress.learnedIds) {
        activeWords.count { it.id in progress.learnedIds }
    }
    val learningWords = remember(
        activeWords,
        progress.maximumWords,
        progress.learnedIds,
        progress.reviewingIds,
    ) {
        progress.limitNewWords(activeWords)
    }
    val learnedFraction = learnedInScope.toFloat() / activeWords.size
    var showSurahDialog by rememberSaveable { mutableStateOf(false) }
    var maximumWordsText by rememberSaveable(progress.maximumWords) {
        mutableStateOf(
            (progress.maximumWords.takeIf { it != LearningWordLimiter.UNLIMITED }
                ?: LearningWordLimiter.DEFAULT_LIMIT).toString(),
        )
    }
    val enteredMaximum = maximumWordsText.toIntOrNull()
    val validMaximum = enteredMaximum != null &&
        enteredMaximum in LearningWordLimiter.MINIMUM_LIMIT..WordRepository.words.size

    if (showSurahDialog) {
        SurahSelectionDialog(
            selectedSurahs = progress.selectedSurahs,
            onToggleSurah = onToggleSurah,
            onDismiss = { showSurahDialog = false },
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(stringResource(R.string.progress_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.progress_subtitle), color = Muted)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBlock(
                value = progress.learnedIds.size.toString(),
                label = stringResource(R.string.stat_learned),
                modifier = Modifier.weight(1f),
            )
            StatBlock(
                value = progress.reviewingIds.size.toString(),
                label = stringResource(R.string.stat_reviewing),
                modifier = Modifier.weight(1f),
            )
            StatBlock(
                value = "🔥 ${progress.streakDays}",
                label = stringResource(R.string.stat_days),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(14.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Forest),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.selected_content), color = Gold, fontWeight = FontWeight.Bold)
                    Text(
                        "$learnedInScope/${activeWords.size}",
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { learnedFraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(9.dp),
                    color = Gold,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.selected_content_note),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.choose_words), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.choose_words_note),
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(10.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 1.dp,
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = progress.studyScope == StudyScope.All,
                        onClick = { onStudyScopeChange(StudyScope.All) },
                        label = { Text(stringResource(R.string.scope_all)) },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = progress.studyScope == StudyScope.Frequent,
                        onClick = { onStudyScopeChange(StudyScope.Frequent) },
                        label = { Text(stringResource(R.string.scope_frequent)) },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = progress.studyScope == StudyScope.Surahs,
                        onClick = { onStudyScopeChange(StudyScope.Surahs) },
                        label = { Text(stringResource(R.string.scope_surah)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (progress.studyScope == StudyScope.Surahs) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (progress.selectedSurahs.size == 1) {
                            stringResource(R.string.one_selected_surah, progress.selectedSurahs.first())
                        } else {
                            pluralStringResource(
                                R.plurals.selected_surahs_count,
                                progress.selectedSurahs.size,
                                progress.selectedSurahs.size,
                            )
                        },
                        color = Forest,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (progress.selectedSurahs.isNotEmpty()) {
                        Text(
                            progress.selectedSurahs.sorted().take(8).joinToString(", ") +
                                if (progress.selectedSurahs.size > 8) "…" else "",
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showSurahDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.search_choose_surahs))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    pluralStringResource(
                        R.plurals.cards_in_current_study,
                        learningWords.size,
                        learningWords.size,
                    ),
                    color = Forest,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.learning_limit), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.learning_limit_description),
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
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
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.language_description),
            color = Muted,
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
                    selected = currentLanguage == AppLanguage.Portuguese,
                    onClick = { onLanguageChange(AppLanguage.Portuguese) },
                    label = { Text(stringResource(R.string.language_portuguese)) },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = currentLanguage == AppLanguage.English,
                    onClick = { onLanguageChange(AppLanguage.English) },
                    label = { Text(stringResource(R.string.language_english)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.routine), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.lock_screen_study), color = Forest, fontWeight = FontWeight.Bold)
                        Text(
                            if (progress.lockScreenEnabled) {
                                stringResource(R.string.lock_screen_study_enabled)
                            } else {
                                stringResource(R.string.lock_screen_study_disabled)
                            },
                            color = Muted,
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
                        Text(stringResource(R.string.settings))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.lock_screen_quiz), color = Forest, fontWeight = FontWeight.Bold)
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
                            color = Muted,
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
                            color = Forest,
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
        Spacer(Modifier.height(12.dp))
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
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(checked = progress.reminderEnabled, onCheckedChange = onReminderChange)
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.daily_goal), fontWeight = FontWeight.Bold)
                    Text(
                        pluralStringResource(R.plurals.words_count, progress.dailyGoal, progress.dailyGoal),
                        color = Forest,
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
        Spacer(Modifier.height(18.dp))
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(
                stringResource(R.string.editorial_note),
                modifier = Modifier.padding(16.dp),
                color = Forest,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
