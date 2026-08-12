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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.WordRepository
import com.kalima.quran.data.limitNewWords

@Composable
fun ProgressScreen(
    progress: StudyProgress,
    onStudyScopeChange: (StudyScope) -> Unit,
    onToggleSurah: (Int) -> Unit,
) {
    val selectionKey = progress.selectedSurahs.sorted().joinToString(",")
    val activeWords = remember(progress.studyScope, selectionKey) {
        WordRepository.wordsFor(progress.studyScope, progress.selectedSurahs)
    }
    val learnedInScope = remember(activeWords, progress.learnedIds) {
        activeWords.count { it.id in progress.learnedIds }
    }
    val dueInScope = progress.dueReviewCount(activeWords.mapTo(mutableSetOf()) { it.id })
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
        Text(
            stringResource(R.string.progress_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.progress_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBlock(
                value = progress.learnedIds.size.toString(),
                label = stringResource(R.string.stat_learned),
                modifier = Modifier.weight(1f),
            )
            StatBlock(
                value = dueInScope.toString(),
                label = stringResource(R.string.stat_due),
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
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        stringResource(R.string.selected_content),
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("$learnedInScope/${activeWords.size}")
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { learnedFraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(9.dp),
                    color = MaterialTheme.colorScheme.secondary,
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
        Text(
            stringResource(R.string.choose_words),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.choose_words_note),
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
                            stringResource(
                                R.string.one_selected_surah,
                                progress.selectedSurahs.first(),
                            )
                        } else {
                            pluralStringResource(
                                R.plurals.selected_surahs_count,
                                progress.selectedSurahs.size,
                                progress.selectedSurahs.size,
                            )
                        },
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (progress.selectedSurahs.isNotEmpty()) {
                        Text(
                            progress.selectedSurahs.sorted().take(8).joinToString(", ") +
                                if (progress.selectedSurahs.size > 8) "…" else "",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                Spacer(Modifier.height(12.dp))
                Text(
                    pluralStringResource(
                        R.plurals.cards_in_current_study,
                        learningWords.size,
                        learningWords.size,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
