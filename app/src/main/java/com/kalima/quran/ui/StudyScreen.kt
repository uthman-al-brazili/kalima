package com.kalima.quran.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.audio.ArabicPronouncer
import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.ReviewQueue
import com.kalima.quran.data.ReviewSchedule
import com.kalima.quran.data.SpacedRepetition
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.WordRepository
import com.kalima.quran.data.limitNewWords
import java.time.Instant
import java.time.LocalDate

@Composable
fun StudyScreen(
    progress: StudyProgress,
    onAnswer: (String, Boolean) -> Unit,
    onCurrentWordChange: (String) -> Unit,
    onEnableLockScreen: () -> Unit,
    pronouncer: ArabicPronouncer,
    launchTarget: StudyLaunchTarget? = null,
) {
    val selectionKey = progress.selectedSurahs.sorted().joinToString(",")
    val availableWords = remember(
        progress.studyScope,
        selectionKey,
        progress.maximumWords,
        progress.learnedIds,
        progress.reviewingIds,
    ) {
        progress.limitNewWords(
            WordRepository.wordsFor(progress.studyScope, progress.selectedSurahs),
        )
    }
    if (availableWords.isEmpty()) {
        LearningLimitEmptyState()
        return
    }
    val words = remember(availableWords, progress.reviewSchedules) {
        ReviewQueue.ordered(
            words = availableWords,
            schedules = progress.reviewSchedules,
            now = Instant.now(),
            newStartIndex = LocalDate.now().toEpochDay().toInt(),
        )
    }
    if (words.isEmpty()) {
        AllCaughtUpState()
        return
    }
    val session = remember(words, launchTarget?.wordId) {
        val requestedWord = launchTarget?.wordId
            ?.let { requestedId -> WordRepository.words.firstOrNull { it.id == requestedId } }
        val resumedWord = progress.currentStudyWordId
            ?.let { currentId -> words.firstOrNull { it.id == currentId } }
        buildStudySession(
            words = words,
            defaultWord = resumedWord ?: words.first(),
            requestedWord = requestedWord,
        )
    }
    var currentWordId by rememberSaveable(
        progress.studyScope.name,
        selectionKey,
        launchTarget?.requestId,
    ) { mutableStateOf(session.first().id) }
    val word = session.firstOrNull { it.id == currentWordId } ?: session.first()
    val moveToNextWord = {
        val currentIndex = session.indexOfFirst { it.id == word.id }.coerceAtLeast(0)
        val nextWord = session[(currentIndex + 1) % session.size]
        currentWordId = nextWord.id
        onCurrentWordChange(nextWord.id)
    }
    val scrollState = rememberScrollState()

    LaunchedEffect(word.id) {
        onCurrentWordChange(word.id)
        scrollState.scrollTo(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        StudyHeader(
            progress = progress,
            dueCount = progress.dueReviewCount(availableWords.mapTo(mutableSetOf()) { it.id }),
        )
        if (!progress.lockScreenEnabled) {
            Spacer(Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.study_lock_screen_title),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.study_lock_screen_description),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = onEnableLockScreen) {
                        Text(stringResource(R.string.enable))
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        WordCard(word, progress, pronouncer)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    onAnswer(word.id, false)
                    moveToNextWord()
                },
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.review_again), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = {
                    onAnswer(word.id, true)
                    moveToNextWord()
                },
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    goodReviewLabel(progress.scheduleFor(word.id)),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.context_meaning_note),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun goodReviewLabel(schedule: ReviewSchedule?): String {
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

@Composable
private fun StudyHeader(progress: StudyProgress, dueCount: Int) {
    val fraction = (progress.todayCompleted.toFloat() / progress.dailyGoal).coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                "السَّلَامُ عَلَيْكُمْ",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(stringResource(R.string.today_word), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                when (progress.studyScope) {
                    StudyScope.All -> stringResource(R.string.scope_all_description)
                    StudyScope.Frequent -> stringResource(R.string.scope_frequent_description)
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
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            if (dueCount > 0) {
                Text(
                    pluralStringResource(R.plurals.reviews_due, dueCount, dueCount),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
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
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.weight(1f).height(7.dp),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "${progress.todayCompleted}/${progress.dailyGoal}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun WordCard(
    word: QuranWord,
    progress: StudyProgress,
    pronouncer: ArabicPronouncer,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(100.dp)) {
                    Text(
                        word.category,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                WordStatusPill(progress.statusFor(word.id))
            }
            Spacer(Modifier.height(22.dp))
            ArabicText(
                word.arabic,
                modifier = Modifier.fillMaxWidth(),
                size = 50,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                word.transliteration,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            PronunciationButton(arabic = word.arabic, pronouncer = pronouncer)
            Spacer(Modifier.height(14.dp))
            Text(
                word.meaning,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(18.dp))
            RootAndGrammar(word.root, word.grammar)
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        word.reference,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    ArabicText(
                        word.verseArabic,
                        modifier = Modifier.fillMaxWidth(),
                        size = 25,
                        align = TextAlign.End,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    "💡 ${word.insight}",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
