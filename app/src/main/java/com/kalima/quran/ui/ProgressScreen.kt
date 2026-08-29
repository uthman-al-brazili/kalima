package com.kalima.quran.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.audio.ArabicPronouncer
import com.kalima.quran.data.HijriCalendar
import com.kalima.quran.data.HijriCalendarDate
import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.QuranVocabularyCoverage
import com.kalima.quran.data.SurahVocabularyCoverage
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.VocabularyCoverage
import com.kalima.quran.data.WordRepository
import com.kalima.quran.data.ReviewHistory
import com.kalima.quran.data.limitNewWords
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProgressScreen(
    progress: StudyProgress,
    onStudyScopeChange: (StudyScope) -> Unit,
    onToggleSurah: (Int) -> Unit,
    pronouncer: ArabicPronouncer,
) {
    val scopeKey = progress.studyScopes.map(StudyScope::name).sorted().joinToString(",")
    val selectionKey = progress.selectedSurahs.sorted().joinToString(",")
    val corpusWords = WordRepository.words
    val statistics by produceState<ProgressStatistics?>(
        initialValue = ProgressStatisticsCache.get(progress, corpusWords)
            ?: ProgressStatisticsCache.latest(corpusWords),
        corpusWords,
        scopeKey,
        selectionKey,
        progress.customStudyIds,
        progress.maximumWords,
        progress.learnedIds,
        progress.reviewingIds,
        progress.alreadyKnownIds,
        progress.reviewSchedules,
        progress.spacedRepetitionEnabled,
    ) {
        ProgressStatisticsCache.get(progress, corpusWords)?.let {
            value = it
            return@produceState
        }
        ProgressStatisticsCache.latest(corpusWords)?.let { value = it }
        value = withContext(Dispatchers.Default) {
            ProgressStatisticsCache.prepare(progress, corpusWords)
        }
    }
    val selectedPathSummary = progress.studyScopes
        .sortedBy(StudyScope::ordinal)
        .map { scope -> studyScopeDescription(scope) }
        .joinToString("  •  ")
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
                value = statistics?.dueInScope?.toString() ?: "—",
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
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        statistics?.let { "${it.learnedInScope}/${it.activeWordCount}" } ?: "—",
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (statistics == null) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(9.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { requireNotNull(statistics).learnedFraction },
                        modifier = Modifier.fillMaxWidth().height(9.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.selected_content_note),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        if (statistics == null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            VocabularyCoverageSummary(requireNotNull(statistics).vocabularyCoverage)
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
                            R.string.path_selected_summary,
                            selectedPathSummary,
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    statistics?.learningWordCount?.let { learningWordCount ->
                        pluralStringResource(
                            R.plurals.cards_in_current_study,
                            learningWordCount,
                            learningWordCount,
                        )
                    } ?: "—",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        ActivityCalendar(progress, pronouncer)
        Spacer(Modifier.height(24.dp))
        if (progress.spacedRepetitionEnabled) DifficultWords(statistics?.difficultWords)
        Spacer(Modifier.height(24.dp))
        RootMastery(statistics?.rootMastery)
        Spacer(Modifier.height(24.dp))
        statistics?.let { VocabularyCoverageBySurah(it.vocabularyCoverage) }
        Spacer(Modifier.height(24.dp))
    }
}

internal data class ProgressStatistics(
    val activeWordCount: Int,
    val learnedInScope: Int,
    val learningWordCount: Int,
    val dueInScope: Int,
    val vocabularyCoverage: QuranVocabularyCoverage,
    val difficultWords: List<Pair<QuranWord, Int>>,
    val rootMastery: List<Triple<String, Int, Int>>,
) {
    val learnedFraction: Float
        get() = if (activeWordCount == 0) 0f else learnedInScope.toFloat() / activeWordCount
}

internal object ProgressStatisticsCache {
    @Volatile
    private var entry: Entry? = null

    fun get(progress: StudyProgress, corpusWords: List<QuranWord>): ProgressStatistics? =
        entry?.takeIf { it.matches(progress, corpusWords) }?.statistics

    fun latest(corpusWords: List<QuranWord>): ProgressStatistics? =
        entry?.takeIf { it.hasCorpus(corpusWords) }?.statistics

    @Synchronized
    fun prepare(progress: StudyProgress, corpusWords: List<QuranWord>): ProgressStatistics {
        get(progress, corpusWords)?.let { return it }
        val statistics = calculateProgressStatistics(progress, corpusWords)
        if (WordRepository.words === corpusWords) {
            entry = Entry(progress, corpusWords, statistics)
        }
        return statistics
    }

    private class Entry(
        progress: StudyProgress,
        private val corpusWords: List<QuranWord>,
        val statistics: ProgressStatistics,
    ) {
        private val studyScope = progress.studyScope
        private val selectedStudyScopes = progress.selectedStudyScopes
        private val selectedSurahs = progress.selectedSurahs
        private val customStudyIds = progress.customStudyIds
        private val maximumWords = progress.maximumWords
        private val learnedIds = progress.learnedIds
        private val reviewingIds = progress.reviewingIds
        private val alreadyKnownIds = progress.alreadyKnownIds
        private val reviewSchedules = progress.reviewSchedules
        private val spacedRepetitionEnabled = progress.spacedRepetitionEnabled

        fun hasCorpus(corpusWords: List<QuranWord>): Boolean = this.corpusWords === corpusWords

        fun matches(progress: StudyProgress, corpusWords: List<QuranWord>): Boolean =
            hasCorpus(corpusWords) &&
                studyScope == progress.studyScope &&
                selectedStudyScopes === progress.selectedStudyScopes &&
                selectedSurahs === progress.selectedSurahs &&
                customStudyIds === progress.customStudyIds &&
                maximumWords == progress.maximumWords &&
                learnedIds === progress.learnedIds &&
                reviewingIds === progress.reviewingIds &&
                alreadyKnownIds === progress.alreadyKnownIds &&
                reviewSchedules === progress.reviewSchedules &&
                spacedRepetitionEnabled == progress.spacedRepetitionEnabled
    }
}

private fun calculateProgressStatistics(
    progress: StudyProgress,
    corpusWords: List<QuranWord>,
): ProgressStatistics {
    val activeWords = WordRepository.wordsFor(
        progress.studyScopes,
        progress.selectedSurahs,
        progress.customStudyIds,
    )
    val learningWords = progress.limitNewWords(activeWords)
    val recognizedWordIds = progress.learnedIds + progress.alreadyKnownIds
    val rootMastery = activeWords
        .asSequence()
        .filter { it.root.isNotBlank() && it.root != "—" }
        .groupBy(QuranWord::root)
        .map { (root, rootWords) ->
            Triple(root, rootWords.count { progress.statusFor(it.id) != com.kalima.quran.data.WordStatus.New }, rootWords.size)
        }
        .sortedWith(compareByDescending<Triple<String, Int, Int>> { it.second }.thenByDescending { it.third })
        .take(5)
    return ProgressStatistics(
        activeWordCount = activeWords.size,
        learnedInScope = activeWords.count { it.id in progress.learnedIds },
        learningWordCount = learningWords.size,
        dueInScope = progress.dueReviewCount(learningWords.mapTo(mutableSetOf()) { it.id }),
        vocabularyCoverage = VocabularyCoverage.calculate(corpusWords, recognizedWordIds),
        difficultWords = progress.reviewSchedules.entries
            .sortedByDescending { it.value.lapses }
            .filter { it.value.lapses > 0 }
            .take(5)
            .mapNotNull { entry -> WordRepository.wordById(entry.key)?.let { it to entry.value.lapses } },
        rootMastery = rootMastery,
    )
}

@Composable
private fun VocabularyCoverageSummary(coverage: QuranVocabularyCoverage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                stringResource(R.string.vocabulary_coverage_title),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                stringResource(R.string.vocabulary_coverage_note),
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.vocabulary_coverage_percent, coverage.percent),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    stringResource(
                        R.string.vocabulary_coverage_occurrences,
                        coverage.recognizedOccurrences,
                        coverage.totalOccurrences,
                    ),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.height(9.dp))
            LinearProgressIndicator(
                progress = { coverage.percent / 100f },
                modifier = Modifier.fillMaxWidth().height(9.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f),
            )
            Spacer(Modifier.height(9.dp))
            Text(
                coverage.nextMilestonePercent?.let { milestone ->
                    stringResource(R.string.vocabulary_coverage_next_milestone, milestone)
                } ?: stringResource(R.string.vocabulary_coverage_top_milestone),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun VocabularyCoverageBySurah(coverage: QuranVocabularyCoverage) {
    val surahs = remember(coverage) {
        coverage.surahs
            .filter { it.recognizedOccurrences > 0 }
            .sortedWith(
                compareByDescending<SurahVocabularyCoverage> { it.percent }
                    .thenByDescending { it.recognizedOccurrences }
                    .thenBy { it.surahNumber },
            )
            .take(5)
    }
    Text(
        stringResource(R.string.surah_mastery),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Text(
        stringResource(R.string.surah_mastery_note),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(Modifier.height(10.dp))
    if (surahs.isEmpty()) {
        Text(stringResource(R.string.no_history_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else surahs.forEach { surah ->
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.surah_number, surah.surahNumber),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.vocabulary_coverage_percent, surah.percent),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(5.dp))
            LinearProgressIndicator(
                progress = { surah.percent / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(
                    R.string.vocabulary_coverage_occurrences,
                    surah.recognizedOccurrences,
                    surah.totalOccurrences,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun PathGroupLabel(labelRes: Int) {
    Text(
        stringResource(labelRes),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun studyScopeDescription(scope: StudyScope): String = when (scope) {
    StudyScope.All -> stringResource(R.string.scope_all_description)
    StudyScope.Frequent50 -> stringResource(R.string.scope_first_50_description)
    StudyScope.Frequent -> stringResource(R.string.scope_frequent_description)
    StudyScope.Frequent300 -> stringResource(R.string.scope_top_300_description)
    StudyScope.Frequent500 -> stringResource(R.string.scope_top_500_description)
    StudyScope.Prayer -> stringResource(R.string.scope_prayer_description)
    StudyScope.ShortSurahs -> stringResource(R.string.scope_short_description)
    StudyScope.Custom -> stringResource(R.string.scope_custom_description)
    StudyScope.Surahs -> stringResource(R.string.scope_surah_description)
}

@Composable
private fun ProgressPathChip(
    selectedScopes: Set<StudyScope>,
    scope: StudyScope,
    labelRes: Int,
    onSelect: (StudyScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = scope in selectedScopes,
        onClick = { onSelect(scope) },
        label = { Text(stringResource(labelRes)) },
        modifier = modifier,
    )
}

@Composable
private fun ActivityCalendar(progress: StudyProgress, pronouncer: ArabicPronouncer) {
    val today = LocalDate.now()
    val todayHijri = HijriCalendar.from(today)
    val counts = remember(progress.reviewEvents) { ReviewHistory.countByDay(progress.reviewEvents) }
    val days = (13L downTo 0L).map(today::minusDays)
    Text(
        stringResource(R.string.activity_14_days),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Text(
        stringResource(R.string.activity_14_days_note),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(Modifier.height(10.dp))
    HijriTodayCard(todayHijri, pronouncer)
    Spacer(Modifier.height(12.dp))
    days.chunked(7).forEach { week ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            week.forEach { day ->
                val hijriDay = HijriCalendar.from(day)
                val count = counts[day] ?: 0
                val translatedDate = stringResource(
                    R.string.hijri_date_translation,
                    weekdayName(hijriDay.dayOfWeek),
                    hijriDay.dayOfMonth,
                    monthName(hijriDay.monthOfYear),
                    hijriDay.year,
                )
                val dayDescription = pluralStringResource(
                    R.plurals.activity_day_description,
                    count,
                    translatedDate,
                    count,
                )
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = dayDescription },
                    color = if (count == 0) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = (0.3f + count * 0.1f).coerceAtMost(1f))
                    },
                    border = if (day == today) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
                    } else {
                        null
                    },
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Column(Modifier.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            HijriCalendar.weekday(hijriDay.dayOfWeek).shortArabic,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(weekdayShortName(hijriDay.dayOfWeek), style = MaterialTheme.typography.labelSmall)
                        Text(
                            HijriCalendar.arabicIndicNumber(hijriDay.dayOfMonth),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text("×$count", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(7.dp))
    }
    Text(
        stringResource(R.string.activity_today_key),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
private fun HijriTodayCard(date: HijriCalendarDate, pronouncer: ArabicPronouncer) {
    val weekday = HijriCalendar.weekday(date.dayOfWeek)
    val month = HijriCalendar.month(date.monthOfYear)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.today_hijri_calendar),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            ArabicText(
                "${weekday.arabic}، ${HijriCalendar.arabicIndicNumber(date.dayOfMonth)} " +
                    "${month.arabic} ${HijriCalendar.arabicIndicNumber(date.year)} هـ",
                modifier = Modifier.fillMaxWidth(),
                size = 25,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(
                    R.string.hijri_date_translation,
                    weekdayName(date.dayOfWeek),
                    date.dayOfMonth,
                    monthName(date.monthOfYear),
                    date.year,
                ),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FoundationPronunciationButton(
                    text = weekday.arabic,
                    pronouncer = pronouncer,
                    modifier = Modifier.weight(1f),
                    labelRes = R.string.hear_weekday,
                )
                FoundationPronunciationButton(
                    text = month.arabic,
                    pronouncer = pronouncer,
                    modifier = Modifier.weight(1f),
                    labelRes = R.string.hear_month,
                )
            }
        }
    }
}

@Composable
private fun weekdayName(day: DayOfWeek): String = stringResource(
    when (day) {
        DayOfWeek.MONDAY -> R.string.weekday_monday
        DayOfWeek.TUESDAY -> R.string.weekday_tuesday
        DayOfWeek.WEDNESDAY -> R.string.weekday_wednesday
        DayOfWeek.THURSDAY -> R.string.weekday_thursday
        DayOfWeek.FRIDAY -> R.string.weekday_friday
        DayOfWeek.SATURDAY -> R.string.weekday_saturday
        DayOfWeek.SUNDAY -> R.string.weekday_sunday
    },
)

@Composable
private fun weekdayShortName(day: DayOfWeek): String = stringResource(
    when (day) {
        DayOfWeek.MONDAY -> R.string.weekday_monday_short
        DayOfWeek.TUESDAY -> R.string.weekday_tuesday_short
        DayOfWeek.WEDNESDAY -> R.string.weekday_wednesday_short
        DayOfWeek.THURSDAY -> R.string.weekday_thursday_short
        DayOfWeek.FRIDAY -> R.string.weekday_friday_short
        DayOfWeek.SATURDAY -> R.string.weekday_saturday_short
        DayOfWeek.SUNDAY -> R.string.weekday_sunday_short
    },
)

@Composable
private fun monthName(month: Int): String = stringResource(
    when (month) {
        1 -> R.string.hijri_month_muharram
        2 -> R.string.hijri_month_safar
        3 -> R.string.hijri_month_rabi_awwal
        4 -> R.string.hijri_month_rabi_thani
        5 -> R.string.hijri_month_jumada_awwal
        6 -> R.string.hijri_month_jumada_thani
        7 -> R.string.hijri_month_rajab
        8 -> R.string.hijri_month_shaban
        9 -> R.string.hijri_month_ramadan
        10 -> R.string.hijri_month_shawwal
        11 -> R.string.hijri_month_dhu_qidah
        12 -> R.string.hijri_month_dhu_hijjah
        else -> error("Invalid Hijri month: $month")
    },
)

@Composable
private fun DifficultWords(difficult: List<Pair<QuranWord, Int>>?) {
    Text(stringResource(R.string.difficult_words), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text(stringResource(R.string.difficult_words_note), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(10.dp))
    if (difficult == null) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    } else if (difficult.isEmpty()) {
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
private fun RootMastery(roots: List<Triple<String, Int, Int>>?) {
    Text(stringResource(R.string.root_mastery), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text(
        stringResource(R.string.root_mastery_note),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(Modifier.height(10.dp))
    if (roots == null) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    } else if (roots.orEmpty().isEmpty()) {
        Text(stringResource(R.string.no_history_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else roots.orEmpty().forEach { (root, familiar, total) ->
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(root, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.mastery_value, familiar, total), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(5.dp))
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else familiar.toFloat() / total },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}
