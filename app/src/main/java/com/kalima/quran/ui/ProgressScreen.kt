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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.audio.ArabicPronouncer
import com.kalima.quran.data.StudyPlan
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.UnderstandPathId
import com.kalima.quran.data.WordRepository
import com.kalima.quran.data.limitNewWords
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProgressScreen(
    progress: StudyProgress,
    onStudyScopeChange: (StudyScope) -> Unit,
    onSelectUnderstandPath: (UnderstandPathId?) -> Unit,
    onAdvanceUnderstandPath: () -> Unit,
    onToggleSurah: (Int) -> Unit,
    pronouncer: ArabicPronouncer,
    onOpenSettings: () -> Unit,
) {
    val studySetScopeKey = progress.studyScopes.map(StudyScope::name).sorted().joinToString(",")
    val selectionKey = progress.selectedSurahs.sorted().joinToString(",")
    val corpusWords = WordRepository.words
    val studyPlan = remember(progress) { StudyPlan.calculate(progress, corpusWords) }
    val statisticsScopeKey = buildString {
        append(studySetScopeKey)
        studyPlan.focus?.let { focus ->
            append(":focus:")
            append(focus.definition.id.name)
            append(':')
            append(focus.currentStageIndex)
        }
    }
    val studyPlanLearningWordCount = remember(
        studyPlan.combinedWords,
        progress.maximumWords,
        progress.learnedIds,
        progress.reviewingIds,
        progress.alreadyKnownIds,
    ) {
        progress.limitNewWords(studyPlan.combinedWords).size
    }
    val statistics by produceState<ProgressStatistics?>(
        initialValue = if (studyPlan.focus == null) {
            ProgressStatisticsCache.get(progress, corpusWords)
                ?: ProgressStatisticsCache.latest(corpusWords)
        } else {
            null
        },
        corpusWords,
        statisticsScopeKey,
        selectionKey,
        studyPlan.combinedWords,
        progress.customStudyIds,
        progress.maximumWords,
        progress.learnedIds,
        progress.reviewingIds,
        progress.alreadyKnownIds,
        progress.reviewSchedules,
        progress.spacedRepetitionEnabled,
    ) {
        if (studyPlan.focus != null) {
            value = withContext(Dispatchers.Default) {
                calculateProgressStatistics(progress, corpusWords, studyPlan.combinedWords)
            }
            return@produceState
        }
        ProgressStatisticsCache.get(progress, corpusWords)?.let {
            value = it
            return@produceState
        }
        ProgressStatisticsCache.latest(corpusWords)?.let { value = it }
        value = withContext(Dispatchers.Default) {
            ProgressStatisticsCache.prepare(progress, corpusWords)
        }
    }
    val selectedStudySetSummary = progress.studyScopes
        .sortedBy(StudyScope::ordinal)
        .map { scope -> studyScopeDescription(scope) }
        .joinToString("  •  ")
    val selectedContentSummary = progress.activeUnderstandPath?.let { pathId ->
        stringResource(
            R.string.study_plan_summary_with_focus,
            understandPathTitle(pathId),
            selectedStudySetSummary,
        )
    } ?: selectedStudySetSummary
    val today = LocalDate.now()
    val eventsToday = progress.reviewEvents.filter {
        it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() == today
    }
    val newToday = eventsToday.count { it.wasNew }
    val reviewedToday = eventsToday.size - newToday
    var showSurahDialog by rememberSaveable { mutableStateOf(false) }
    var showStudySetDetails by rememberSaveable { mutableStateOf(false) }
    var showMoreDetails by rememberSaveable { mutableStateOf(false) }

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
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.progress_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            TabSettingsButton(
                onClick = onOpenSettings,
                modifier = Modifier.alignTabSettingsButton(
                    contentHorizontalPadding = 16.dp,
                    contentTopPadding = 12.25.dp,
                ),
            )
        }
        Text(
            stringResource(R.string.progress_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(12.dp))
        ProgressOverviewCard(
            statistics = statistics,
            dueNow = statistics?.dueInScope,
            streakDays = progress.streakDays,
            newToday = newToday,
            reviewedToday = reviewedToday,
            accuracy7 = progress.accuracy(7),
            accuracy30 = progress.accuracy(30),
        )
        Spacer(Modifier.height(12.dp))
        if (statistics == null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            VocabularyCoverageSummary(requireNotNull(statistics).vocabularyCoverage)
        }
        Spacer(Modifier.height(12.dp))
        ExpandableSectionHeader(
            title = stringResource(R.string.study_plan_title),
            summary = selectedContentSummary,
            expanded = showStudySetDetails,
            showLabel = stringResource(R.string.study_plan_manage),
            hideLabel = stringResource(R.string.study_plan_hide),
            onToggle = { showStudySetDetails = !showStudySetDetails },
        )
        if (showStudySetDetails) {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 1.dp,
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        stringResource(R.string.study_plan_focus_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.study_plan_focus_note),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    UnderstandPathLauncher(
                        progress = progress,
                        onSelectPath = onSelectUnderstandPath,
                        onAdvancePath = onAdvanceUnderstandPath,
                    )
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.study_plan_supporting_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(
                            if (progress.activeUnderstandPath == null) {
                                R.string.study_plan_supporting_note
                            } else {
                                R.string.study_plan_supporting_note_with_focus
                            },
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(10.dp))
                PathGroupLabel(R.string.path_group_frequency)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProgressPathChip(progress.studyScopes, StudyScope.Frequent50, R.string.scope_first_50, onStudyScopeChange, Modifier.weight(1f))
                    ProgressPathChip(progress.studyScopes, StudyScope.Frequent, R.string.scope_top_100, onStudyScopeChange, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProgressPathChip(progress.studyScopes, StudyScope.Frequent300, R.string.scope_top_300, onStudyScopeChange, Modifier.weight(1f))
                    ProgressPathChip(progress.studyScopes, StudyScope.Frequent500, R.string.scope_top_500, onStudyScopeChange, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                PathGroupLabel(R.string.path_group_goal)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProgressPathChip(progress.studyScopes, StudyScope.Prayer, R.string.scope_prayer, onStudyScopeChange, Modifier.weight(1f))
                    ProgressPathChip(progress.studyScopes, StudyScope.ShortSurahs, R.string.scope_short_surahs, onStudyScopeChange, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                PathGroupLabel(R.string.path_group_collection)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProgressPathChip(progress.studyScopes, StudyScope.All, R.string.scope_all, onStudyScopeChange, Modifier.weight(1f))
                    ProgressPathChip(progress.studyScopes, StudyScope.Surahs, R.string.scope_surah, onStudyScopeChange, Modifier.weight(1f))
                }
                ProgressPathChip(
                    progress.studyScopes,
                    StudyScope.Custom,
                    R.string.scope_custom,
                    onStudyScopeChange,
                    Modifier.fillMaxWidth(),
                )
                if (StudyScope.Surahs in progress.studyScopes) {
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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        stringResource(
                            R.string.study_plan_supporting_summary,
                            selectedStudySetSummary,
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    pluralStringResource(
                        R.plurals.cards_in_current_plan,
                        studyPlanLearningWordCount,
                        studyPlanLearningWordCount,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ExpandableSectionHeader(
            title = stringResource(R.string.progress_more_details),
            summary = stringResource(R.string.progress_more_details_note),
            expanded = showMoreDetails,
            showLabel = stringResource(R.string.progress_show_details),
            hideLabel = stringResource(R.string.progress_hide_details),
            onToggle = { showMoreDetails = !showMoreDetails },
        )
        if (showMoreDetails) {
            Spacer(Modifier.height(18.dp))
            ActivityCalendar(progress, pronouncer)
            if (progress.spacedRepetitionEnabled) {
                Spacer(Modifier.height(20.dp))
                DifficultWords(statistics?.difficultWords)
            }
            Spacer(Modifier.height(20.dp))
            RootMastery(statistics?.rootMastery)
            statistics?.let {
                Spacer(Modifier.height(20.dp))
                VocabularyCoverageBySurah(it.vocabularyCoverage)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
