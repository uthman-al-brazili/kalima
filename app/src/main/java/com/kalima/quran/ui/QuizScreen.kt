package com.kalima.quran.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kalima.quran.R
import com.kalima.quran.audio.ArabicPronouncer
import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.WordRepository
import com.kalima.quran.data.needsAlphabetFoundation
import com.kalima.quran.quiz.QuizEngine
import com.kalima.quran.quiz.QuizQuestion
import com.kalima.quran.quiz.QuizQuestionType
import com.kalima.quran.quiz.QuizMode
import com.kalima.quran.quiz.VerseExcerptBuilder
import kotlin.random.Random

internal data class QuizSessionKey(
    val studyScopes: Set<StudyScope>,
    val selectedSurahs: Set<Int>,
    val customStudyIds: Set<String>,
    val alreadyKnownIds: Set<String>,
    val mode: QuizMode,
    val version: Int,
)

internal fun StudyProgress.quizSessionKey(mode: QuizMode, version: Int) = QuizSessionKey(
    studyScopes = studyScopes,
    selectedSurahs = selectedSurahs,
    customStudyIds = customStudyIds,
    alreadyKnownIds = alreadyKnownIds,
    mode = mode,
    version = version,
)

@Composable
fun QuizScreen(
    progress: StudyProgress,
    onAnswer: (String, Boolean) -> Unit,
    pronouncer: ArabicPronouncer,
) {
    if (progress.needsAlphabetFoundation) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(R.string.quiz_after_alphabet_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.quiz_after_alphabet_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    val scopeKey = progress.studyScopes.map(StudyScope::name).sorted().joinToString(",")
    val selectionKey = progress.selectedSurahs.sorted().joinToString(",")
    val selectedSource = remember(
        scopeKey,
        selectionKey,
        progress.customStudyIds,
    ) {
        WordRepository.wordsFor(
            progress.studyScopes,
            progress.selectedSurahs,
            progress.customStudyIds,
        )
    }
    val selectedWords = remember(selectedSource, progress.alreadyKnownIds) {
        selectedSource.filterNot { it.id in progress.alreadyKnownIds }
    }
    if (selectedWords.isEmpty()) {
        when {
            selectedSource.isNotEmpty() && selectedSource.all { it.id in progress.alreadyKnownIds } ->
                AllWordsAlreadyKnownState()
            progress.studyScopes == setOf(StudyScope.Custom) -> EmptyCollectionState()
            else -> LearningLimitEmptyState()
        }
        return
    }
    val optionWords = remember(selectedWords, progress.alreadyKnownIds) {
        quizOptionPool(
            selectedWords = selectedWords,
            allWords = WordRepository.words,
            alreadyKnownIds = progress.alreadyKnownIds,
        )
    }
    var modeName by rememberSaveable { mutableStateOf(QuizMode.Mixed.name) }
    val mode = QuizMode.entries.firstOrNull { it.name == modeName } ?: QuizMode.Mixed
    var sessionVersion by rememberSaveable { mutableIntStateOf(0) }
    var quizStarted by rememberSaveable { mutableStateOf(false) }
    if (!quizStarted) {
        QuizModeStartScreen(
            mode = mode,
            onModeChange = { modeName = it.name },
            onStart = {
                sessionVersion += 1
                quizStarted = true
            },
        )
        return
    }
    val sessionKey = progress.quizSessionKey(mode, sessionVersion)
    val sessionSeed by rememberSaveable(sessionKey) { mutableIntStateOf(Random.Default.nextInt()) }
    val targets = remember(selectedWords, mode) {
        when (mode) {
            QuizMode.Roots -> selectedWords.filter { it.root.isNotBlank() && it.root != "—" }
            else -> selectedWords
        }
    }
    if (targets.isEmpty()) {
        QuizModeEmptyScreen(
            mode = mode,
            onModeChange = {
                modeName = it.name
                quizStarted = false
            },
        )
        return
    }
    val session = remember(sessionKey, optionWords) {
        QuizEngine.createSession(
            words = targets,
            optionWords = optionWords,
            random = Random(sessionSeed),
            mode = mode,
        )
    }
    if (session.isEmpty()) {
        QuizOptionsEmptyScreen(
            mode = mode,
            onModeChange = {
                modeName = it.name
                quizStarted = false
            },
        )
        return
    }
    var currentIndex by rememberSaveable(sessionKey) { mutableIntStateOf(0) }
    var selectedOption by rememberSaveable(sessionKey, currentIndex) {
        mutableStateOf<Int?>(null)
    }
    var score by rememberSaveable(sessionKey) { mutableIntStateOf(0) }

    if (currentIndex >= session.size) {
        QuizSummary(
            score = score,
            total = session.size,
            onNewQuiz = { quizStarted = false },
        )
        return
    }

    val question = session[currentIndex]
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { currentIndex.toFloat() / session.size },
                modifier = Modifier.weight(1f).height(7.dp),
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                "  ${currentIndex + 1}/${session.size}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(10.dp))
        QuizQuestionCard(question, pronouncer)
        Spacer(Modifier.height(8.dp))
        question.options.forEachIndexed { index, option ->
            QuizOption(
                text = option,
                arabic = question.type in setOf(
                    QuizQuestionType.PortugueseToArabic,
                    QuizQuestionType.ClozeToArabic,
                    QuizQuestionType.RootToArabic,
                ),
                selected = selectedOption == index,
                correct = index == question.correctOptionIndex,
                answered = selectedOption != null,
                onClick = {
                    if (selectedOption == null) {
                        selectedOption = index
                        val correct = index == question.correctOptionIndex
                        if (correct) score += 1
                        onAnswer(question.word.id, correct)
                    }
                },
            )
            Spacer(Modifier.height(6.dp))
        }
        if (selectedOption != null) {
            Spacer(Modifier.height(6.dp))
            QuizFeedback(
                question = question,
                correct = selectedOption == question.correctOptionIndex,
                onNext = { currentIndex += 1 },
                lastQuestion = currentIndex == session.lastIndex,
            )
        }
        if (selectedOption == null) {
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun QuizModeStartScreen(
    mode: QuizMode,
    onModeChange: (QuizMode) -> Unit,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Text(
            stringResource(R.string.quiz_choose_mode),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.quiz_intro),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        QuizModeSelector(mode = mode, onModeChange = onModeChange)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.start_quiz))
        }
    }
}

internal fun quizOptionPool(
    selectedWords: List<QuranWord>,
    allWords: List<QuranWord>,
    alreadyKnownIds: Set<String>,
): List<QuranWord> {
    val selectedIds = selectedWords.mapTo(mutableSetOf()) { it.id }
    return selectedWords + allWords.filterNot { word ->
        word.id in selectedIds || word.id in alreadyKnownIds
    }
}

@Composable
private fun QuizQuestionCard(question: QuizQuestion, pronouncer: ArabicPronouncer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                when (question.type) {
                    QuizQuestionType.ArabicToPortuguese -> stringResource(R.string.quiz_arabic_to_meaning)
                    QuizQuestionType.PortugueseToArabic -> stringResource(R.string.quiz_meaning_to_arabic)
                    QuizQuestionType.ContextualMeaning -> stringResource(R.string.quiz_contextual_meaning)
                    QuizQuestionType.ListeningToPortuguese -> stringResource(R.string.quiz_listening_prompt)
                    QuizQuestionType.ClozeToArabic -> stringResource(R.string.quiz_cloze_prompt)
                    QuizQuestionType.RootToArabic -> stringResource(R.string.quiz_root_prompt)
                },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(10.dp))
            when (question.type) {
                QuizQuestionType.ArabicToPortuguese -> {
                    ArabicText(
                        question.word.arabic,
                        modifier = Modifier.fillMaxWidth(),
                        size = 48,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        question.word.transliteration,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    PronunciationButton(
                        word = question.word,
                        pronouncer = pronouncer,
                        modifier = Modifier.size(44.dp),
                        compact = true,
                    )
                }

                QuizQuestionType.PortugueseToArabic -> Text(
                    question.word.meaning,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                QuizQuestionType.ContextualMeaning -> ContextualVerse(question)

                QuizQuestionType.ListeningToPortuguese -> {
                    Text(
                        stringResource(R.string.tap_to_listen),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(10.dp))
                    PronunciationButton(
                        word = question.word,
                        pronouncer = pronouncer,
                        labelRes = R.string.device_voice_slow,
                        playbackRate = ArabicPronouncer.WORD_SLOW_RATE,
                    )
                }

                QuizQuestionType.ClozeToArabic -> {
                    ArabicText(
                        VerseExcerptBuilder.buildCloze(question.word),
                        modifier = Modifier.fillMaxWidth(),
                        size = 24,
                        align = TextAlign.End,
                    )
                    Text(
                        question.word.reference,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                QuizQuestionType.RootToArabic -> {
                    Text(
                        question.word.root,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        question.word.grammar,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizModeSelector(mode: QuizMode, onModeChange: (QuizMode) -> Unit) {
    val labels = listOf(
        QuizMode.Mixed to R.string.quiz_mode_mixed,
        QuizMode.Listening to R.string.quiz_mode_listening,
        QuizMode.Cloze to R.string.quiz_mode_cloze,
        QuizMode.Roots to R.string.quiz_mode_roots,
    )
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEach { (option, label) ->
            FilterChip(
                selected = mode == option,
                onClick = { onModeChange(option) },
                label = { Text(stringResource(label)) },
            )
        }
    }
}

@Composable
private fun QuizModeEmptyScreen(mode: QuizMode, onModeChange: (QuizMode) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        QuizModeSelector(mode, onModeChange)
        Spacer(Modifier.height(28.dp))
        Text(
            stringResource(R.string.no_words_found),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QuizOptionsEmptyScreen(mode: QuizMode, onModeChange: (QuizMode) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        QuizModeSelector(mode, onModeChange)
        Spacer(Modifier.height(28.dp))
        Text(
            stringResource(R.string.quiz_not_enough_options),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ContextualVerse(question: QuizQuestion) {
    val excerpt = remember(question.word.id) { VerseExcerptBuilder.build(question.word) }
    val annotated = buildAnnotatedString {
        append(excerpt.text)
        if (excerpt.hasHighlight) {
            addStyle(
                SpanStyle(
                    background = MaterialTheme.colorScheme.secondary.copy(alpha = 0.42f),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                ),
                excerpt.highlightStart,
                excerpt.highlightEnd,
            )
        }
    }
    Text(
        text = annotated,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.End,
        fontSize = 25.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Medium,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        question.word.reference,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
internal fun QuizOption(
    text: String,
    arabic: Boolean,
    selected: Boolean,
    correct: Boolean,
    answered: Boolean,
    accessibilityDescription: String? = null,
    onClick: () -> Unit,
) {
    val container = when {
        answered && correct -> MaterialTheme.colorScheme.primaryContainer
        answered && selected -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val border = when {
        answered && correct -> MaterialTheme.colorScheme.primary
        answered && selected -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (accessibilityDescription != null) {
                    Modifier.semantics { contentDescription = accessibilityDescription }
                } else {
                    Modifier
                },
            ),
        enabled = !answered,
        color = container,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, border),
    ) {
        if (arabic) {
            ArabicText(
                text,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
                size = 24,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Text(
                text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun QuizFeedback(
    question: QuizQuestion,
    correct: Boolean,
    onNext: () -> Unit,
    lastQuestion: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (correct) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                if (correct) {
                    stringResource(R.string.correct)
                } else {
                    stringResource(R.string.correct_answer, question.correctAnswer)
                },
                color = if (correct) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.quiz_word_translation, question.word.meaning),
                color = if (correct) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${stringResource(
                    R.string.reference_root,
                    question.word.transliteration,
                    question.word.root,
                )} • ${question.word.reference}",
                color = if (correct) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                } else {
                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.78f)
                },
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(
                        if (lastQuestion) R.string.view_result else R.string.next_question,
                    ),
                )
            }
        }
    }
}

@Composable
private fun QuizSummary(
    score: Int,
    total: Int,
    onNewQuiz: () -> Unit,
) {
    val sessionAccuracy = quizSessionAccuracy(score, total)
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.quiz_completed), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "$score/$total",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            when {
                score == total -> stringResource(R.string.quiz_excellent)
                score * 2 >= total -> stringResource(R.string.quiz_good)
                else -> stringResource(R.string.quiz_keep_practicing)
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.quiz_session_accuracy, sessionAccuracy),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(22.dp))
        Button(onClick = onNewQuiz) {
            Text(stringResource(R.string.choose_quiz_mode_action))
        }
    }
}

internal fun quizSessionAccuracy(score: Int, total: Int): Int =
    if (total <= 0) 0 else (score.coerceIn(0, total) * 100) / total
