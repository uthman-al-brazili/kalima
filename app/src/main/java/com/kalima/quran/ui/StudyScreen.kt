package com.kalima.quran.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.kalima.quran.data.ArabicFoundations
import com.kalima.quran.data.ReviewQueue
import com.kalima.quran.data.ReviewSchedule
import com.kalima.quran.data.SpacedRepetition
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.WordRepository
import com.kalima.quran.data.WordStatus
import com.kalima.quran.data.limitNewWords
import com.kalima.quran.data.hasNumberFoundationLesson
import com.kalima.quran.data.needsAlphabetFoundation
import java.time.Instant
import java.time.LocalDate

@Composable
fun StudyScreen(
    progress: StudyProgress,
    onAnswer: (String, Boolean) -> Unit,
    onCurrentWordChange: (String) -> Unit,
    onEnableLockScreen: () -> Unit,
    onToggleCustomList: (String) -> Unit,
    onToggleAlreadyKnown: (String) -> Unit,
    onShowCompleteAyahChange: (Boolean) -> Unit,
    onCompleteAlphabetLesson: () -> Unit,
    onCompleteNumberLesson: () -> Unit,
    pronouncer: ArabicPronouncer,
    launchTarget: StudyLaunchTarget? = null,
) {
    if (progress.needsAlphabetFoundation) {
        AlphabetFoundationScreen(
            progress = progress,
            onCompleteAlphabetLesson = onCompleteAlphabetLesson,
            onCompleteNumberLesson = onCompleteNumberLesson,
        )
        return
    }
    val selectionKey = progress.selectedSurahs.sorted().joinToString(",")
    val selectedWords = remember(
        progress.studyScope,
        selectionKey,
        progress.customStudyIds,
    ) {
        WordRepository.wordsFor(
            progress.studyScope,
            progress.selectedSurahs,
            progress.customStudyIds,
        )
    }
    val availableWords = remember(
        selectedWords,
        progress.maximumWords,
        progress.learnedIds,
        progress.reviewingIds,
        progress.alreadyKnownIds,
    ) {
        progress.limitNewWords(selectedWords)
    }
    if (availableWords.isEmpty()) {
        when {
            selectedWords.isNotEmpty() && selectedWords.all { it.id in progress.alreadyKnownIds } ->
                AllWordsAlreadyKnownState()
            progress.studyScope == StudyScope.Custom -> EmptyCollectionState()
            else -> LearningLimitEmptyState()
        }
        return
    }
    val words = remember(
        availableWords,
        progress.reviewSchedules,
        progress.spacedRepetitionEnabled,
    ) {
        val dailyStart = LocalDate.now().toEpochDay().toInt()
        if (progress.spacedRepetitionEnabled) {
            ReviewQueue.ordered(
                words = availableWords,
                schedules = progress.reviewSchedules,
                now = Instant.now(),
                newStartIndex = dailyStart,
            )
        } else {
            ReviewQueue.rotated(availableWords, dailyStart)
        }
    }
    if (words.isEmpty()) {
        AllCaughtUpState()
        return
    }
    val session = remember(words, launchTarget?.wordId) {
        val requestedWord = launchTarget?.wordId
            ?.let(WordRepository::wordById)
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
    val word = WordRepository.wordById(currentWordId) ?: session.first()
    var meaningRevealed by rememberSaveable(word.id) {
        mutableStateOf(shouldRevealMeaningInitially(progress.statusFor(word.id)))
    }
    val moveToNextWord = {
        val currentIndex = session.indexOfFirst { it.id == word.id }
        val nextWord = if (currentIndex < 0) session.first() else session[(currentIndex + 1) % session.size]
        currentWordId = nextWord.id
        onCurrentWordChange(nextWord.id)
    }
    val scrollState = rememberScrollState()

    LaunchedEffect(word.id) {
        onCurrentWordChange(word.id)
        scrollState.scrollTo(0)
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
            if (progress.hasNumberFoundationLesson) {
                Spacer(Modifier.height(16.dp))
                NumberFoundationCard(
                    progress = progress,
                    onCompleteNumberLesson = onCompleteNumberLesson,
                )
            }
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
            WordCard(
                word = word,
                progress = progress,
                pronouncer = pronouncer,
                meaningRevealed = meaningRevealed,
                onRevealChange = { meaningRevealed = it },
                onToggleCustomList = onToggleCustomList,
                onToggleAlreadyKnown = { wordId ->
                    val markingAsKnown = wordId !in progress.alreadyKnownIds
                    onToggleAlreadyKnown(wordId)
                    if (markingAsKnown && wordId == word.id) moveToNextWord()
                },
                showCompleteAyah = progress.showCompleteAyah,
                onShowCompleteAyahChange = onShowCompleteAyahChange,
                onOpenWord = { wordId ->
                    currentWordId = wordId
                    onCurrentWordChange(wordId)
                },
            )
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.context_meaning_note),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(112.dp))
        }

        StudyActionBar(
            meaningRevealed = meaningRevealed,
            spacedRepetitionEnabled = progress.spacedRepetitionEnabled,
            goodTiming = goodReviewTiming(
                spacedRepetitionEnabled = progress.spacedRepetitionEnabled,
                schedule = progress.scheduleFor(word.id),
            ),
            onRevealMeaning = { meaningRevealed = true },
            onAgain = {
                onAnswer(word.id, false)
                moveToNextWord()
            },
            onRemembered = {
                onAnswer(word.id, true)
                moveToNextWord()
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun AlphabetFoundationScreen(
    progress: StudyProgress,
    onCompleteAlphabetLesson: () -> Unit,
    onCompleteNumberLesson: () -> Unit,
) {
    val lessonIndex = progress.completedAlphabetLessons
        .coerceIn(0, ArabicFoundations.alphabetLessons.lastIndex)
    val lesson = ArabicFoundations.alphabetLessons[lessonIndex]
    val fraction = lessonIndex.toFloat() / ArabicFoundations.alphabetLessonCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            stringResource(R.string.alphabet_course_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.alphabet_course_words_locked),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.weight(1f).height(7.dp),
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(
                    R.string.foundation_step_progress,
                    lessonIndex + 1,
                    ArabicFoundations.alphabetLessonCount,
                ),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(18.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    stringResource(
                        if (lesson.teachesVowels) {
                            R.string.alphabet_vowels_lesson
                        } else {
                            R.string.alphabet_letters_lesson
                        },
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                lesson.symbols.chunked(2).forEach { rowSymbols ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Place the first lesson symbol on the right, matching Arabic reading order.
                        rowSymbols.asReversed().forEach { symbol ->
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    ArabicText(
                                        symbol.arabic,
                                        modifier = Modifier.fillMaxWidth(),
                                        size = 40,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        symbol.transliteration,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                        if (rowSymbols.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Text(
                    stringResource(
                        if (lesson.teachesVowels) {
                            R.string.alphabet_vowels_note
                        } else {
                            R.string.alphabet_letters_note
                        },
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onCompleteAlphabetLesson,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        stringResource(
                            if (lessonIndex == ArabicFoundations.alphabetLessons.lastIndex) {
                                R.string.finish_alphabet_course
                            } else {
                                R.string.complete_alphabet_step
                            },
                        ),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        if (progress.hasNumberFoundationLesson) {
            Spacer(Modifier.height(16.dp))
            NumberFoundationCard(
                progress = progress,
                onCompleteNumberLesson = onCompleteNumberLesson,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NumberFoundationCard(
    progress: StudyProgress,
    onCompleteNumberLesson: () -> Unit,
) {
    val lessonIndex = progress.completedNumberLessons
        .coerceIn(0, ArabicFoundations.numberLessons.lastIndex)
    val lesson = ArabicFoundations.numberLessons[lessonIndex]
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.number_course_title),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(
                            R.string.foundation_step_progress,
                            lessonIndex + 1,
                            ArabicFoundations.numberLessonCount,
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Text(
                    "${lesson.arabicDigit}  =  ${lesson.westernDigit}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            ArabicText(
                lesson.arabicName,
                modifier = Modifier.fillMaxWidth(),
                size = 32,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                lesson.transliteration,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onCompleteNumberLesson,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    stringResource(
                        if (lessonIndex == ArabicFoundations.numberLessons.lastIndex) {
                            R.string.finish_number_course
                        } else {
                            R.string.next_number
                        },
                    ),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun StudyActionBar(
    meaningRevealed: Boolean,
    spacedRepetitionEnabled: Boolean,
    goodTiming: String,
    onRevealMeaning: () -> Unit,
    onAgain: () -> Unit,
    onRemembered: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        if (!meaningRevealed) {
            Button(
                onClick = onRevealMeaning,
                modifier = Modifier.padding(8.dp).fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.reveal_meaning), fontWeight = FontWeight.Bold)
            }
        } else {
            Row(
                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onAgain,
                    modifier = Modifier.weight(1f).height(68.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    ReviewActionContent(
                        title = stringResource(
                            if (spacedRepetitionEnabled) R.string.review_again
                            else R.string.review_again_no_schedule,
                        ),
                        detail = stringResource(
                            if (spacedRepetitionEnabled) R.string.review_again_timing
                            else R.string.review_without_schedule,
                        ),
                    )
                }
                Button(
                    onClick = onRemembered,
                    modifier = Modifier.weight(1f).height(68.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    ReviewActionContent(
                        title = stringResource(R.string.review_remembered),
                        detail = goodTiming,
                    )
                }
            }
        }
    }
}

internal fun shouldRevealMeaningInitially(status: WordStatus): Boolean = status == WordStatus.New

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
private fun goodReviewTiming(
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
    meaningRevealed: Boolean,
    onRevealChange: (Boolean) -> Unit,
    onToggleCustomList: (String) -> Unit,
    onToggleAlreadyKnown: (String) -> Unit,
    showCompleteAyah: Boolean,
    onShowCompleteAyahChange: (Boolean) -> Unit,
    onOpenWord: (String) -> Unit,
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
                Column(horizontalAlignment = Alignment.End) {
                    WordStatusPill(progress.statusFor(word.id))
                    OutlinedButton(
                        onClick = { onToggleAlreadyKnown(word.id) },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            stringResource(
                                if (word.id in progress.alreadyKnownIds) {
                                    R.string.restore_to_practice
                                } else {
                                    R.string.mark_already_known
                                },
                            ),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
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
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PronunciationButton(
                    word = word,
                    pronouncer = pronouncer,
                    modifier = Modifier.size(44.dp),
                    compact = true,
                    labelRes = R.string.device_voice_slow,
                    playbackRate = ArabicPronouncer.WORD_SLOW_RATE,
                )
            }
            Spacer(Modifier.height(14.dp))
            WordCollectionActions(
                word = word,
                inCustomList = word.id in progress.customStudyIds,
                alreadyKnown = word.id in progress.alreadyKnownIds,
                onToggleCustomList = onToggleCustomList,
                onToggleAlreadyKnown = onToggleAlreadyKnown,
                showAlreadyKnown = false,
            )
            if (meaningRevealed) {
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
                        if (showCompleteAyah) {
                            VerseExplorerPanel(word = word, onOpenWord = onOpenWord)
                            VersePronunciationButton(
                                word = word,
                                pronouncer = pronouncer,
                                modifier = Modifier.fillMaxWidth(),
                                labelRes = R.string.hussary_verse_recitation,
                            )
                            TextButton(
                                onClick = { onShowCompleteAyahChange(false) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.hide_complete_ayah))
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onShowCompleteAyahChange(true) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.show_complete_ayah))
                            }
                        }
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
                Spacer(Modifier.height(14.dp))
                CitationActions(word)
                Spacer(Modifier.height(8.dp))
                EditorialReviewPanel(word)
                TextButton(onClick = { onRevealChange(false) }) {
                    Text(stringResource(R.string.hide_meaning))
                }
            }
        }
    }
}
