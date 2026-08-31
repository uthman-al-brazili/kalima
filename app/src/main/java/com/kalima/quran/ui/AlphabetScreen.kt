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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.audio.ArabicPronouncer
import com.kalima.quran.data.AlphabetQuestionType
import com.kalima.quran.data.ArabicFoundations
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.hasAlphabetFoundationLesson
import java.time.Instant

@Composable
fun AlphabetScreen(
    progress: StudyProgress,
    onCompleteAlphabetLesson: () -> Unit,
    onAlphabetPracticeAnswer: (String, Boolean) -> Unit,
    onStartAlphabetFoundation: () -> Unit,
    onSkipAlphabetFoundation: () -> Unit,
    pronouncer: ArabicPronouncer,
) {
    if (progress.hasAlphabetFoundationLesson) {
        AlphabetFoundationScreen(
            progress = progress,
            onCompleteAlphabetLesson = onCompleteAlphabetLesson,
            onAlphabetPracticeAnswer = onAlphabetPracticeAnswer,
            onSkipAlphabetFoundation = onSkipAlphabetFoundation,
            pronouncer = pronouncer,
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        AlphabetAccessCard(
            progress = progress,
            onStartAlphabetFoundation = onStartAlphabetFoundation,
        )
        Spacer(Modifier.height(14.dp))
        AlphabetReferenceTable()
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AlphabetFoundationScreen(
    progress: StudyProgress,
    onCompleteAlphabetLesson: () -> Unit,
    onAlphabetPracticeAnswer: (String, Boolean) -> Unit,
    onSkipAlphabetFoundation: () -> Unit,
    pronouncer: ArabicPronouncer,
) {
    val lessonIndex = progress.completedAlphabetLessons
        .coerceIn(0, ArabicFoundations.alphabetLessons.lastIndex)
    val lesson = ArabicFoundations.alphabetLessons[lessonIndex]
    var recalling by rememberSaveable(lessonIndex) { mutableStateOf(false) }
    var symbolIndex by rememberSaveable(lessonIndex) { mutableStateOf(0) }
    var selectedOptionIndex by rememberSaveable(lessonIndex) { mutableStateOf<Int?>(null) }
    val practiceQuestions = remember(lessonIndex) {
        ArabicFoundations.cumulativePracticeQuestions(
            lessonIndex = lessonIndex,
            schedules = progress.alphabetReviewSchedules,
            now = Instant.now(),
        ).filter { question ->
            question.type != AlphabetQuestionType.AudioToGlyph ||
                question.audioResourceName?.let(pronouncer::hasFoundationAudio) == true
        }
    }
    var decodingMilestone by rememberSaveable(lessonIndex) { mutableStateOf(false) }
    var showReference by rememberSaveable { mutableStateOf(false) }
    val foundationScrollState = rememberScrollState()
    val compactActiveStudy = recalling || decodingMilestone

    LaunchedEffect(recalling, decodingMilestone, symbolIndex) {
        foundationScrollState.scrollTo(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(foundationScrollState)
            .padding(
                horizontal = 20.dp,
                vertical = 8.dp,
            ),
    ) {
        if (!compactActiveStudy) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { showReference = !showReference },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        stringResource(
                            if (showReference) R.string.hide_alphabet_table_short
                            else R.string.open_alphabet_table_short,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    stringResource(
                        R.string.foundation_step_progress,
                        lessonIndex + 1,
                        ArabicFoundations.alphabetLessonCount,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(
                    onClick = onSkipAlphabetFoundation,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.skip_alphabet_short))
                }
            }
            if (showReference) {
                AlphabetReferenceTable()
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(6.dp))
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(if (decodingMilestone) 18.dp else 12.dp)) {
                if (recalling) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.alphabet_recall_title),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(
                                R.string.alphabet_symbol_progress,
                                symbolIndex + 1,
                                practiceQuestions.size,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                } else if (decodingMilestone) {
                    Text(
                        stringResource(R.string.alphabet_decode_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.alphabet_decode_instruction),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(16.dp))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.alphabet_letters_lesson),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(
                                R.string.alphabet_symbol_progress,
                                symbolIndex + 1,
                                lesson.symbols.size,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (decodingMilestone) {
                    AlphabetDecodingMilestone(
                        milestone = lesson.milestone,
                        pronouncer = pronouncer,
                        onComplete = onCompleteAlphabetLesson,
                    )
                } else if (!recalling) {
                    val symbol = lesson.symbols[symbolIndex]
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(22.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            ArabicText(
                                symbol.arabic,
                                modifier = Modifier.fillMaxWidth(),
                                size = 72,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (symbol.spokenArabic != symbol.arabic) {
                                ArabicText(
                                    symbol.spokenArabic,
                                    modifier = Modifier.fillMaxWidth(),
                                    size = 26,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Text(
                                symbol.transliteration,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(6.dp))
                            FoundationPronunciationButton(
                                text = symbol.spokenArabic,
                                pronouncer = pronouncer,
                                labelRes = R.string.hear_letter,
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                stringResource(R.string.alphabet_connected_forms),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                stringResource(R.string.alphabet_forms_reading_direction),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Spacer(Modifier.height(4.dp))
                            val labelledForms = listOfNotNull(
                                symbol.isolatedForm to stringResource(R.string.alphabet_form_isolated),
                                symbol.initialForm?.let {
                                    it to stringResource(R.string.alphabet_form_initial)
                                },
                                symbol.medialForm?.let {
                                    it to stringResource(R.string.alphabet_form_medial)
                                },
                                symbol.finalForm to stringResource(R.string.alphabet_form_final),
                            )
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    labelledForms.forEach { (form, label) ->
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            ArabicText(
                                                form,
                                                modifier = Modifier.fillMaxWidth(),
                                                size = 34,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                            CompositionLocalProvider(
                                                LocalLayoutDirection provides LayoutDirection.Ltr,
                                            ) {
                                                Text(
                                                    label,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                        alpha = 0.72f,
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    textAlign = TextAlign.Center,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (!symbol.connectsToFollowing) {
                                Text(
                                    stringResource(R.string.alphabet_break_letter),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { symbolIndex -= 1 },
                            modifier = Modifier.weight(1f).height(52.dp),
                            enabled = symbolIndex > 0,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(stringResource(R.string.previous_symbol))
                        }
                        Button(
                            onClick = {
                                if (symbolIndex == lesson.symbols.lastIndex) {
                                    symbolIndex = 0
                                    selectedOptionIndex = null
                                    recalling = true
                                } else {
                                    symbolIndex += 1
                                }
                            },
                            modifier = Modifier.weight(1.35f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(
                                stringResource(
                                    if (symbolIndex == lesson.symbols.lastIndex) {
                                        R.string.start_alphabet_recall
                                    } else {
                                        R.string.next_symbol
                                    },
                                ),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                } else {
                    val question = practiceQuestions[symbolIndex]
                    val answered = selectedOptionIndex != null
                    val answeredCorrectly = selectedOptionIndex == question.correctOptionIndex
                    Text(
                        stringResource(
                            when (question.type) {
                                AlphabetQuestionType.GlyphToSound -> R.string.alphabet_prompt_glyph_sound
                                AlphabetQuestionType.AudioToGlyph -> R.string.alphabet_prompt_audio_glyph
                                AlphabetQuestionType.ConnectedToGlyph -> R.string.alphabet_prompt_connected_glyph
                                AlphabetQuestionType.VowelledToSound -> R.string.alphabet_prompt_vowelled_sound
                            },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (question.promptArabic != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(22.dp),
                        ) {
                            AlphabetPromptArabicText(
                                question.promptArabic,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                size = 72,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        FoundationPronunciationButton(
                            text = requireNotNull(question.spokenArabic),
                            audioResourceName = question.audioResourceName,
                            pronouncer = pronouncer,
                            modifier = Modifier.fillMaxWidth(),
                            labelRes = R.string.hear_letter,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    question.options.forEachIndexed { optionIndex, option ->
                        val selected = selectedOptionIndex == optionIndex
                        val correct = answered && optionIndex == question.correctOptionIndex
                        val incorrect = selected && !correct
                        val containerColor = when {
                            correct -> MaterialTheme.colorScheme.primaryContainer
                            incorrect -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val contentColor = when {
                            correct -> MaterialTheme.colorScheme.onPrimaryContainer
                            incorrect -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        val borderColor = when {
                            correct -> MaterialTheme.colorScheme.primary
                            incorrect -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.outline
                        }
                        OutlinedButton(
                            onClick = {
                                if (!answered) {
                                    selectedOptionIndex = optionIndex
                                    onAlphabetPracticeAnswer(
                                        question.masteryKey,
                                        optionIndex == question.correctOptionIndex,
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (option.isArabic) 64.dp else 48.dp),
                            enabled = !answered,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = containerColor,
                                contentColor = contentColor,
                                disabledContainerColor = containerColor,
                                disabledContentColor = contentColor,
                            ),
                            border = BorderStroke(1.dp, borderColor),
                        ) {
                            if (option.isArabic) {
                                ArabicText(
                                    option.text,
                                    modifier = Modifier.fillMaxWidth(),
                                    size = 30,
                                    color = contentColor,
                                )
                            } else {
                                Text(option.text, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    if (answered) {
                        Text(
                            stringResource(
                                if (answeredCorrectly) {
                                    R.string.alphabet_answer_correct
                                } else {
                                    R.string.alphabet_try_again
                                },
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            color = if (answeredCorrectly) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    if (answered) {
                        Button(
                            onClick = {
                                if (symbolIndex == practiceQuestions.lastIndex) {
                                    decodingMilestone = true
                                    selectedOptionIndex = null
                                } else {
                                    symbolIndex += 1
                                    selectedOptionIndex = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(
                                stringResource(
                                    when {
                                        symbolIndex < practiceQuestions.lastIndex -> {
                                            R.string.next_alphabet_question
                                        }
                                        lessonIndex == ArabicFoundations.alphabetLessons.lastIndex -> {
                                            R.string.finish_alphabet_course
                                        }
                                        else -> R.string.complete_alphabet_step
                                    },
                                ),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            recalling = false
                            symbolIndex = 0
                            selectedOptionIndex = null
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                    ) {
                        Text(stringResource(R.string.review_alphabet_symbols))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
