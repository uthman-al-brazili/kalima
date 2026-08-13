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
import com.kalima.quran.data.ReviewHistory
import com.kalima.quran.data.limitNewWords
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ProgressScreen(
    progress: StudyProgress,
    onStudyScopeChange: (StudyScope) -> Unit,
    onToggleSurah: (Int) -> Unit,
) {
    val selectionKey = progress.selectedSurahs.sorted().joinToString(",")
    val activeWords = remember(
        progress.studyScope,
        selectionKey,
        progress.favoriteIds,
        progress.customStudyIds,
    ) {
        WordRepository.wordsFor(
            progress.studyScope,
            progress.selectedSurahs,
            progress.favoriteIds,
            progress.customStudyIds,
        )
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
    val learnedFraction = if (activeWords.isEmpty()) 0f else learnedInScope.toFloat() / activeWords.size
    val today = LocalDate.now()
    val eventsToday = progress.reviewEvents.filter {
        it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() == today
    }
    val newToday = eventsToday.count { it.wasNew }
    val reviewedToday = eventsToday.size - newToday
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
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBlock(
                value = progress.accuracy(7)?.let { "$it%" } ?: "—",
                label = stringResource(R.string.accuracy_7_days),
                modifier = Modifier.weight(1f),
            )
            StatBlock(
                value = progress.accuracy(30)?.let { "$it%" } ?: "—",
                label = stringResource(R.string.accuracy_30_days),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBlock(newToday.toString(), stringResource(R.string.new_today), Modifier.weight(1f))
            StatBlock(reviewedToday.toString(), stringResource(R.string.reviewed_today), Modifier.weight(1f))
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
            stringResource(R.string.guided_paths),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.guided_paths_note),
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
                    ProgressPathChip(progress.studyScope, StudyScope.Frequent50, R.string.scope_first_50, onStudyScopeChange, Modifier.weight(1f))
                    ProgressPathChip(progress.studyScope, StudyScope.Frequent, R.string.scope_top_100, onStudyScopeChange, Modifier.weight(1f))
                    ProgressPathChip(progress.studyScope, StudyScope.Frequent300, R.string.scope_top_300, onStudyScopeChange, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProgressPathChip(progress.studyScope, StudyScope.Frequent500, R.string.scope_top_500, onStudyScopeChange, Modifier.weight(1f))
                    ProgressPathChip(progress.studyScope, StudyScope.Prayer, R.string.scope_prayer, onStudyScopeChange, Modifier.weight(1f))
                    ProgressPathChip(progress.studyScope, StudyScope.ShortSurahs, R.string.scope_short_surahs, onStudyScopeChange, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProgressPathChip(progress.studyScope, StudyScope.All, R.string.scope_all, onStudyScopeChange, Modifier.weight(1f))
                    ProgressPathChip(progress.studyScope, StudyScope.Favorites, R.string.scope_favorites, onStudyScopeChange, Modifier.weight(1f))
                    ProgressPathChip(progress.studyScope, StudyScope.Custom, R.string.scope_custom, onStudyScopeChange, Modifier.weight(1f))
                }
                ProgressPathChip(progress.studyScope, StudyScope.Surahs, R.string.scope_surah, onStudyScopeChange, Modifier.fillMaxWidth())
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
        ActivityCalendar(progress)
        Spacer(Modifier.height(24.dp))
        if (progress.spacedRepetitionEnabled) DifficultWords(progress)
        Spacer(Modifier.height(24.dp))
        RootMastery(activeWords, progress)
        Spacer(Modifier.height(24.dp))
        SurahMastery(activeWords, progress)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SurahMastery(words: List<com.kalima.quran.data.QuranWord>, progress: StudyProgress) {
    val surahs = remember(words, progress.learnedIds, progress.reviewingIds) {
        words.filter { it.surahNumber != null }
            .groupBy { requireNotNull(it.surahNumber) }
            .map { (surah, surahWords) ->
                Triple(surah, surahWords.count { progress.statusFor(it.id) != com.kalima.quran.data.WordStatus.New }, surahWords.size)
            }
            .sortedWith(compareByDescending<Triple<Int, Int, Int>> { it.second }.thenBy { it.first })
            .take(5)
    }
    Text(stringResource(R.string.surah_mastery), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
    if (surahs.isEmpty()) {
        Text(stringResource(R.string.no_history_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else surahs.forEach { (surah, familiar, total) ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.surah_number, surah),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.mastery_value, familiar, total), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProgressPathChip(
    selectedScope: StudyScope,
    scope: StudyScope,
    labelRes: Int,
    onSelect: (StudyScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selectedScope == scope,
        onClick = { onSelect(scope) },
        label = { Text(stringResource(labelRes)) },
        modifier = modifier,
    )
}

@Composable
private fun ActivityCalendar(progress: StudyProgress) {
    val today = LocalDate.now()
    val counts = remember(progress.reviewEvents) { ReviewHistory.countByDay(progress.reviewEvents) }
    val days = (13L downTo 0L).map(today::minusDays)
    Text(
        stringResource(R.string.activity_14_days),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(10.dp))
    days.chunked(7).forEach { week ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            week.forEach { day ->
                val count = counts[day] ?: 0
                Surface(
                    modifier = Modifier.weight(1f),
                    color = if (count == 0) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = (0.3f + count * 0.1f).coerceAtMost(1f))
                    },
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall)
                        Text(count.toString(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(7.dp))
    }
}

@Composable
private fun DifficultWords(progress: StudyProgress) {
    val difficult = remember(progress.reviewSchedules) {
        progress.reviewSchedules.entries
            .sortedByDescending { it.value.lapses }
            .filter { it.value.lapses > 0 }
            .take(5)
            .mapNotNull { entry -> WordRepository.words.firstOrNull { it.id == entry.key }?.let { it to entry.value.lapses } }
    }
    Text(stringResource(R.string.difficult_words), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text(stringResource(R.string.difficult_words_note), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(10.dp))
    if (difficult.isEmpty()) {
        Text(stringResource(R.string.no_history_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else difficult.forEach { (word, lapses) ->
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp),
        ) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    ArabicText(word.arabic, size = 25, align = androidx.compose.ui.text.style.TextAlign.Start)
                    Text(word.meaning, style = MaterialTheme.typography.bodySmall)
                }
                Text("×$lapses", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RootMastery(words: List<com.kalima.quran.data.QuranWord>, progress: StudyProgress) {
    val roots = remember(words, progress.learnedIds, progress.reviewingIds) {
        words.filter { it.root.isNotBlank() && it.root != "—" }
            .groupBy { it.root }
            .map { (root, rootWords) ->
                Triple(root, rootWords.count { progress.statusFor(it.id) != com.kalima.quran.data.WordStatus.New }, rootWords.size)
            }
            .sortedWith(compareByDescending<Triple<String, Int, Int>> { it.second }.thenByDescending { it.third })
            .take(5)
    }
    Text(stringResource(R.string.root_mastery), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
    if (roots.isEmpty()) {
        Text(stringResource(R.string.no_history_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else roots.forEach { (root, familiar, total) ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(root, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.mastery_value, familiar, total), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
