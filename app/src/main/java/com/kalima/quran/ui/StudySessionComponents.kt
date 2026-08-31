package com.kalima.quran.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.audio.ArabicPronouncer
import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.ReviewSchedule
import com.kalima.quran.data.SpacedRepetition
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.WordStatus

@Composable
internal fun StudyActionBar(
    meaningRevealed: Boolean,
    isNewPresentation: Boolean,
    nextActionOpensCheckpoint: Boolean,
    nextActionCompletesSession: Boolean,
    spacedRepetitionEnabled: Boolean,
    goodTiming: String,
    onRevealMeaning: () -> Unit,
    onNextWord: () -> Unit,
    onAgain: () -> Unit,
    onRemembered: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
            if (!meaningRevealed) {
                Button(
                    onClick = onRevealMeaning,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(stringResource(R.string.reveal_meaning), fontWeight = FontWeight.Bold)
                }
            } else if (isNewPresentation) {
                Button(
                    onClick = onNextWord,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        stringResource(
                            if (nextActionOpensCheckpoint) {
                                R.string.continue_to_context_checkpoint
                            } else if (nextActionCompletesSession) {
                                R.string.finish_action
                            } else {
                                R.string.next_word
                            },
                        ),
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onAgain,
                        modifier = Modifier.weight(1f).height(58.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        ReviewActionContent(
                            title = stringResource(R.string.review_hard),
                            detail = stringResource(
                                if (spacedRepetitionEnabled) R.string.review_again_timing
                                else R.string.review_without_schedule,
                            ),
                        )
                    }
                    Button(
                        onClick = onRemembered,
                        modifier = Modifier.weight(1f).height(58.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        ReviewActionContent(
                            title = stringResource(R.string.review_easy),
                            detail = goodTiming,
                        )
                    }
                }
            }
    }
}

internal fun shouldRevealMeaningInitially(status: WordStatus): Boolean = status == WordStatus.New

internal fun studyPresentationStatus(
    status: WordStatus,
    isNewPresentation: Boolean,
): WordStatus = if (isNewPresentation) WordStatus.New else status

internal fun studyWordsForPresentation(
    queuedWords: List<QuranWord>,
    availableWords: List<QuranWord>,
    activeIntroductionId: String?,
): List<QuranWord> = if (queuedWords.isNotEmpty()) {
    queuedWords
} else {
    activeIntroductionId
        ?.let { introducedId -> availableWords.firstOrNull { it.id == introducedId } }
        ?.let(::listOf)
        .orEmpty()
}

internal fun shouldShowDailyMission(
    showMission: Boolean,
    hasActiveUnderstandPath: Boolean,
    hasQueuedWords: Boolean,
    showCompletion: Boolean,
    hasLaunchTarget: Boolean,
): Boolean = !hasLaunchTarget && (
    showMission || (hasActiveUnderstandPath && !hasQueuedWords && !showCompletion)
)

@Composable
private fun ReviewActionContent(title: String, detail: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            detail,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun goodReviewTiming(
    spacedRepetitionEnabled: Boolean,
    schedule: ReviewSchedule?,
): String {
    if (!spacedRepetitionEnabled) return stringResource(R.string.review_without_schedule)
    val intervalDays = SpacedRepetition.nextGoodIntervalDays(schedule)
    return if (intervalDays == 1) {
        stringResource(R.string.review_good_tomorrow)
    } else {
        pluralStringResource(
            R.plurals.review_good_days,
            intervalDays,
            intervalDays,
        )
    }
}

internal fun buildStudySession(
    words: List<QuranWord>,
    defaultWord: QuranWord,
    requestedWord: QuranWord?,
): List<QuranWord> {
    val firstWord = requestedWord ?: defaultWord
    val firstIndex = words.indexOfFirst { it.id == firstWord.id }
    return when {
        firstIndex >= 0 -> words.drop(firstIndex) + words.take(firstIndex)
        requestedWord != null -> listOf(requestedWord) + words.filterNot { it.id == requestedWord.id }
        else -> words
    }
}

internal fun displayedStudyWordId(
    savedWordId: String,
    launchTarget: StudyLaunchTarget?,
): String = launchTarget?.wordId ?: savedWordId

internal fun studyQueueSourceWords(
    availableWords: List<QuranWord>,
    requestedWord: QuranWord?,
): List<QuranWord> = availableWords.ifEmpty { listOfNotNull(requestedWord) }

internal fun studySessionPosition(completedWords: Int, sessionWords: Int): Int =
    (completedWords + 1).coerceIn(1, sessionWords.coerceAtLeast(1))

internal fun isFinalStudySessionWord(
    requiredWordIds: List<String>,
    completedWordIds: Collection<String>,
    currentWordId: String,
): Boolean = requiredWordIds.isNotEmpty() &&
    requiredWordIds.all { it == currentWordId || it in completedWordIds }

@Composable
internal fun StudyHeader(
    progress: StudyProgress,
    completedWords: Int,
    sessionWords: Int,
    reviewCount: Int,
    onOpenSettings: () -> Unit,
) {
    val currentPosition = studySessionPosition(completedWords, sessionWords)
    val fraction = (currentPosition.toFloat() / sessionWords.coerceAtLeast(1))
        .coerceIn(0f, 1f)
    val supportingSummary = if (progress.studyScopes.size > 1) {
        stringResource(R.string.study_paths_combined, progress.studyScopes.size)
    } else when (progress.studyScopes.single()) {
        StudyScope.All -> stringResource(R.string.scope_all_description)
        StudyScope.Frequent50 -> stringResource(R.string.scope_first_50_description)
        StudyScope.Frequent -> stringResource(R.string.scope_frequent_description)
        StudyScope.Frequent300 -> stringResource(R.string.scope_top_300_description)
        StudyScope.Frequent500 -> stringResource(R.string.scope_top_500_description)
        StudyScope.Prayer -> stringResource(R.string.scope_prayer_description)
        StudyScope.ShortSurahs -> stringResource(R.string.scope_short_description)
        StudyScope.Custom -> stringResource(R.string.scope_custom_description)
        StudyScope.Surahs -> if (progress.selectedSurahs.size <= 4) {
            stringResource(
                R.string.scope_surah_list,
                progress.selectedSurahs.sorted().joinToString(", "),
            )
        } else {
            pluralStringResource(
                R.plurals.selected_surahs_count,
                progress.selectedSurahs.size,
                progress.selectedSurahs.size,
            )
        }
    }
    val scopeSummary = progress.activeUnderstandPath?.let { pathId ->
        pluralStringResource(
            R.plurals.study_focus_with_supporting_sets,
            progress.studyScopes.size,
            understandPathTitle(pathId),
            progress.studyScopes.size,
        )
    } ?: supportingSummary
    val dueSummary = if (reviewCount > 0) {
        pluralStringResource(R.plurals.reviews_due, reviewCount, reviewCount)
    } else {
        null
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.today_word),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                listOfNotNull(dueSummary, scopeSummary).joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
            shape = RoundedCornerShape(100.dp),
        ) {
            Text(
                pluralStringResource(
                    R.plurals.streak_days,
                    progress.streakDays,
                    progress.streakDays,
                ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        TabSettingsButton(
            onClick = onOpenSettings,
            modifier = Modifier.alignTabSettingsButton(
                contentHorizontalPadding = 12.dp,
                contentTopPadding = 8.dp,
            ),
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.weight(1f).height(5.dp),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            drawStopIndicator = {},
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "$currentPosition/$sessionWords",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WordCard(
    word: QuranWord,
    progress: StudyProgress,
    displayedStatus: WordStatus,
    pronouncer: ArabicPronouncer,
    meaningRevealed: Boolean,
    onToggleCustomList: (String) -> Unit,
    onToggleAlreadyKnown: (String) -> Unit,
) {
    var showDetailsSheet by rememberSaveable(word.id) { mutableStateOf(false) }
    val inCustomList = word.id in progress.customStudyIds
    val customListDescription = stringResource(
        if (inCustomList) R.string.remove_custom_list else R.string.add_custom_list,
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(100.dp)) {
                    Text(
                        word.category,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                WordStatusPill(displayedStatus)
            }
            Spacer(Modifier.height(10.dp))
            ArabicText(
                word.arabic,
                modifier = Modifier.fillMaxWidth(),
                size = 44,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                word.transliteration,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PronunciationButton(
                    word = word,
                    pronouncer = pronouncer,
                    modifier = Modifier.size(44.dp),
                    compact = true,
                    labelRes = R.string.device_voice_slow,
                    playbackRate = ArabicPronouncer.WORD_SLOW_RATE,
                )
                Spacer(Modifier.width(2.dp))
                TextButton(
                    onClick = { onToggleCustomList(word.id) },
                    modifier = Modifier.semantics {
                        contentDescription = customListDescription
                    },
                ) {
                    Text(
                        "${if (inCustomList) "✓" else "+"} " +
                            stringResource(R.string.custom_list_action),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Spacer(Modifier.width(2.dp))
                TextButton(onClick = { onToggleAlreadyKnown(word.id) }) {
                    Text(
                        stringResource(
                            if (word.id in progress.alreadyKnownIds) {
                                R.string.restore_to_practice_short
                            } else {
                                R.string.mark_already_known_short
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            if (meaningRevealed) {
                Spacer(Modifier.height(4.dp))
                Text(
                    word.meaning,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = { showDetailsSheet = true }) {
                    Text(stringResource(R.string.word_more_details))
                }
                if (showDetailsSheet) {
                    ModalBottomSheet(onDismissRequest = { showDetailsSheet = false }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                        ) {
                            Text(
                                stringResource(R.string.word_details_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(10.dp))
                    RootAndGrammar(word.root, word.grammar)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.context_meaning_note),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.word_context_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        word.reference,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    VerseExplorerPanel(word = word)
                    VersePronunciationButton(
                        word = word,
                        pronouncer = pronouncer,
                        modifier = Modifier.fillMaxWidth(),
                        dense = true,
                        centerLabel = true,
                        labelRes = R.string.ayah_audio_short,
                    )
                Spacer(Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        "💡 ${word.learnerInsight}",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                            Spacer(Modifier.height(28.dp))
                        }
                    }
                }
            }
        }
    }
}
