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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kalima.quran.R
import com.kalima.quran.audio.ArabicPronouncer
import com.kalima.quran.data.QUIZ_MASTERY_DAYS
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.WordRepository
import com.kalima.quran.quiz.QuizEngine
import com.kalima.quran.quiz.QuizQuestion
import com.kalima.quran.quiz.QuizQuestionType
import com.kalima.quran.quiz.VerseExcerptBuilder
import com.kalima.quran.ui.theme.Forest
import com.kalima.quran.ui.theme.Gold
import com.kalima.quran.ui.theme.Muted

@Composable
fun QuizScreen(
    progress: StudyProgress,
    onAnswer: (String, Boolean) -> Unit,
    pronouncer: ArabicPronouncer,
) {
    val selectionKey = progress.selectedSurahs.sorted().joinToString(",")
    val activeWords = remember(progress.studyScope, selectionKey) {
        WordRepository.wordsFor(progress.studyScope, progress.selectedSurahs)
    }
    var sessionVersion by rememberSaveable { mutableIntStateOf(0) }
    val session = remember(progress.studyScope, selectionKey, sessionVersion) {
        QuizEngine.createSession(activeWords, progress::statusFor)
    }
    var currentIndex by rememberSaveable(sessionVersion) { mutableIntStateOf(0) }
    var selectedOption by rememberSaveable(sessionVersion, currentIndex) {
        mutableStateOf<Int?>(null)
    }
    var score by rememberSaveable(sessionVersion) { mutableIntStateOf(0) }

    if (currentIndex >= session.size) {
        QuizSummary(
            score = score,
            progress = progress,
            onNewQuiz = { sessionVersion += 1 },
        )
        return
    }

    val question = session[currentIndex]
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text("Quiz", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.quiz_intro),
            color = Muted,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { currentIndex.toFloat() / session.size },
                modifier = Modifier.weight(1f).height(7.dp),
                color = Gold,
            )
            Text(
                "  ${currentIndex + 1}/${session.size}",
                color = Forest,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(18.dp))
        QuizQuestionCard(question)
        Spacer(Modifier.height(14.dp))
        question.options.forEachIndexed { index, option ->
            QuizOption(
                text = option,
                arabic = question.type == QuizQuestionType.PortugueseToArabic,
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
            Spacer(Modifier.height(9.dp))
        }
        if (selectedOption != null) {
            Spacer(Modifier.height(6.dp))
            QuizFeedback(
                question = question,
                correct = selectedOption == question.correctOptionIndex,
                correctDays = progress.quizCorrectDayCount(question.word.id),
                onNext = { currentIndex += 1 },
                lastQuestion = currentIndex == session.lastIndex,
                pronouncer = pronouncer,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QuizQuestionCard(question: QuizQuestion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                when (question.type) {
                    QuizQuestionType.ArabicToPortuguese -> stringResource(R.string.quiz_arabic_to_meaning)
                    QuizQuestionType.PortugueseToArabic -> stringResource(R.string.quiz_meaning_to_arabic)
                    QuizQuestionType.ContextualMeaning -> stringResource(R.string.quiz_contextual_meaning)
                },
                modifier = Modifier.fillMaxWidth(),
                color = Muted,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(18.dp))
            when (question.type) {
                QuizQuestionType.ArabicToPortuguese -> {
                    ArabicText(question.word.arabic, modifier = Modifier.fillMaxWidth(), size = 48, color = Forest)
                    Text(question.word.transliteration, color = Muted)
                }

                QuizQuestionType.PortugueseToArabic -> Text(
                    question.word.meaning,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Forest,
                )

                QuizQuestionType.ContextualMeaning -> ContextualVerse(question)
            }
        }
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
                    background = Gold.copy(alpha = 0.42f),
                    color = Forest,
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
        fontSize = 27.sp,
        lineHeight = 43.sp,
        fontWeight = FontWeight.Medium,
    )
    Spacer(Modifier.height(10.dp))
    Text(question.word.reference, color = Forest, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun QuizOption(
    text: String,
    arabic: Boolean,
    selected: Boolean,
    correct: Boolean,
    answered: Boolean,
    onClick: () -> Unit,
) {
    val container = when {
        answered && correct -> Forest.copy(alpha = 0.16f)
        answered && selected -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val border = when {
        answered && correct -> Forest
        answered && selected -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    }
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = !answered,
        color = container,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, border),
    ) {
        if (arabic) {
            ArabicText(
                text,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                size = 28,
                color = Forest,
            )
        } else {
            Text(
                text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
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
    correctDays: Int,
    onNext: () -> Unit,
    lastQuestion: Boolean,
    pronouncer: ArabicPronouncer,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (correct) Forest.copy(alpha = 0.1f) else MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (correct) {
                    stringResource(R.string.correct)
                } else {
                    stringResource(R.string.correct_answer, question.correctAnswer)
                },
                color = if (correct) Forest else MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(
                    R.string.reference_root,
                    question.word.transliteration,
                    question.word.root,
                ),
                color = Muted,
            )
            Text(question.word.reference, color = Forest, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            PronunciationButton(
                arabic = question.word.arabic,
                pronouncer = pronouncer,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(7.dp))
            Text(
                stringResource(
                    R.string.quiz_mastery,
                    correctDays.coerceAtMost(QUIZ_MASTERY_DAYS),
                    QUIZ_MASTERY_DAYS,
                ),
                color = Forest,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(12.dp))
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
    progress: StudyProgress,
    onNewQuiz: () -> Unit,
) {
    val historicalAccuracy = if (progress.quizTotalAnswers == 0) {
        0
    } else {
        progress.quizCorrectAnswers * 100 / progress.quizTotalAnswers
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.quiz_completed), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "$score/${QuizEngine.SESSION_SIZE}",
            color = Forest,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            when (score) {
                5 -> stringResource(R.string.quiz_excellent)
                in 3..4 -> stringResource(R.string.quiz_good)
                else -> stringResource(R.string.quiz_keep_practicing)
            },
            color = Muted,
            textAlign = TextAlign.Center,
        )
        if (progress.quizTotalAnswers > 0) {
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.overall_accuracy, historicalAccuracy),
                color = Forest,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(22.dp))
        Button(onClick = onNewQuiz) {
            Text(stringResource(R.string.start_another_quiz))
        }
    }
}
