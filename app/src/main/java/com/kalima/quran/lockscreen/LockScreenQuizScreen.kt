package com.kalima.quran.lockscreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kalima.quran.R
import com.kalima.quran.quiz.QuizQuestion
import com.kalima.quran.quiz.QuizQuestionType
import com.kalima.quran.quiz.VerseExcerptBuilder
import com.kalima.quran.ui.ArabicIndicClock
import com.kalima.quran.ui.ArabicText
import com.kalima.quran.ui.PronunciationButton
import com.kalima.quran.ui.rememberArabicPronouncer
import com.kalima.quran.ui.theme.Cream
import com.kalima.quran.ui.theme.Forest
import com.kalima.quran.ui.theme.Gold
import com.kalima.quran.ui.theme.KalimaTheme

@Composable
fun LockScreenQuizScreen(
    question: QuizQuestion,
    initialSelectedOption: Int?,
    onAnswered: (Int, Boolean) -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit,
) {
    var selectedOption by remember(question.word.id) { mutableStateOf(initialSelectedOption) }
    val pronouncer = rememberArabicPronouncer()

    KalimaTheme {
        Surface(color = Forest, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("كَلِمَة", color = Gold, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.quick_quiz), color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.not_now), color = Color.White)
                    }
                }
                ArabicIndicClock(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 4.dp),
                    color = Gold,
                )

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when (question.type) {
                            QuizQuestionType.ArabicToPortuguese -> stringResource(R.string.quiz_arabic_to_meaning)
                            QuizQuestionType.PortugueseToArabic -> stringResource(R.string.quiz_meaning_to_arabic)
                            QuizQuestionType.ContextualMeaning -> stringResource(R.string.quiz_contextual_meaning)
                            else -> stringResource(R.string.quiz_arabic_to_meaning)
                        },
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(18.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Cream,
                        shape = RoundedCornerShape(22.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            LockQuizPrompt(question)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    question.options.forEachIndexed { index, option ->
                        LockQuizOption(
                            text = option,
                            arabic = question.type == QuizQuestionType.PortugueseToArabic,
                            selected = selectedOption == index,
                            correct = index == question.correctOptionIndex,
                            answered = selectedOption != null,
                            onClick = {
                                if (selectedOption == null) {
                                    selectedOption = index
                                    onAnswered(index, index == question.correctOptionIndex)
                                }
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    if (selectedOption != null) {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Cream,
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    if (selectedOption == question.correctOptionIndex) {
                                        stringResource(R.string.correct)
                                    } else {
                                        stringResource(R.string.correct_answer, question.correctAnswer)
                                    },
                                    color = Forest,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    stringResource(
                                        R.string.reference_root,
                                        question.word.transliteration,
                                        question.word.root,
                                    ),
                                    color = Forest.copy(alpha = 0.72f),
                                )
                                Text(question.word.reference, color = Forest, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(10.dp))
                                PronunciationButton(
                                    word = question.word,
                                    pronouncer = pronouncer,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (selectedOption != null) {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Forest),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.confirm_and_continue), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                }
                OutlinedButton(
                    onClick = onOpenApp,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(stringResource(R.string.open_app), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LockQuizPrompt(question: QuizQuestion) {
    when (question.type) {
        QuizQuestionType.ArabicToPortuguese -> {
            ArabicText(question.word.arabic, modifier = Modifier.fillMaxWidth(), size = 47, color = Forest)
            Text(question.word.transliteration, color = Forest.copy(alpha = 0.7f))
        }

        QuizQuestionType.PortugueseToArabic -> Text(
            question.word.meaning,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Forest,
        )

        QuizQuestionType.ContextualMeaning -> {
            val excerpt = remember(question.word.id) { VerseExcerptBuilder.build(question.word, maxChars = 150) }
            val annotated = buildAnnotatedString {
                append(excerpt.text)
                if (excerpt.hasHighlight) {
                    addStyle(
                        SpanStyle(background = Gold.copy(alpha = 0.5f), fontWeight = FontWeight.Bold),
                        excerpt.highlightStart,
                        excerpt.highlightEnd,
                    )
                }
            }
            Text(
                annotated,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                fontSize = 25.sp,
                lineHeight = 39.sp,
                color = Forest,
            )
            Text(question.word.reference, color = Forest, style = MaterialTheme.typography.labelMedium)
        }
        else -> {
            ArabicText(
                question.word.arabic,
                modifier = Modifier.fillMaxWidth(),
                size = 46,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun LockQuizOption(
    text: String,
    arabic: Boolean,
    selected: Boolean,
    correct: Boolean,
    answered: Boolean,
    onClick: () -> Unit,
) {
    val color = when {
        answered && correct -> Gold.copy(alpha = 0.34f)
        answered && selected -> MaterialTheme.colorScheme.errorContainer
        else -> Color.White.copy(alpha = 0.1f)
    }
    val border = when {
        answered && correct -> Gold
        answered && selected -> MaterialTheme.colorScheme.error
        else -> Color.White.copy(alpha = 0.3f)
    }
    Surface(
        onClick = onClick,
        enabled = !answered,
        modifier = Modifier.fillMaxWidth(),
        color = color,
        border = BorderStroke(1.dp, border),
        shape = RoundedCornerShape(15.dp),
    ) {
        if (arabic) {
            ArabicText(
                text,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 14.dp),
                size = 27,
                color = Color.White,
            )
        } else {
            Text(
                text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
