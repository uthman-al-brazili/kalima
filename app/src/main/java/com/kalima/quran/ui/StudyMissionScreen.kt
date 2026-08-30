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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.audio.ArabicPronouncer
import com.kalima.quran.data.SessionLevel
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudySessionPlan
import java.time.format.TextStyle

@Composable
internal fun DailyMissionScreen(
    plan: StudySessionPlan,
    progress: StudyProgress,
    streakDays: Int,
    activity: List<DailyMissionActivity>,
    pathQuizReady: Boolean,
    lockScreenEnabled: Boolean,
    onEnableLockScreen: () -> Unit,
    onOpenQuiz: () -> Unit,
    onSessionLevelChange: (SessionLevel) -> Unit,
    onStart: () -> Unit,
) {
    val levelName = stringResource(sessionLevelNameRes(plan.level))
    val reviewSummary = pluralStringResource(
        if (plan.freePractice) R.plurals.session_practice_words_count
        else R.plurals.session_reviews_count,
        plan.reviewWordIds.size,
        plan.reviewWordIds.size,
    )
    val guidedLessonCompletedToday = plan.currentGuidedLessonCompletedToday
    val lessonSummary = if (guidedLessonCompletedToday) {
        stringResource(R.string.guided_lesson_completed_today)
    } else if (plan.guidedStageIndex != null) {
        pluralStringResource(
            R.plurals.session_ayah_words_count,
            plan.lessonWordIds.size,
            plan.lessonWordIds.size,
        )
    } else {
        pluralStringResource(
            R.plurals.session_new_words_count,
            plan.lessonWordIds.size,
            plan.lessonWordIds.size,
        )
    }
    val planParts = buildList {
        if (plan.reviewWordIds.isNotEmpty()) add(reviewSummary)
        if (plan.lessonWordIds.isNotEmpty()) add(lessonSummary)
    }
    val guidedMissionComplete = plan.isEmpty && guidedLessonCompletedToday
    val planSummary = when {
        guidedMissionComplete -> stringResource(R.string.daily_arabic_goal_complete)
        planParts.isEmpty() -> stringResource(
            if (plan.level == SessionLevel.Quick) R.string.session_quick_caught_up
            else R.string.mission_all_caught_up,
        )
        else -> stringResource(
            R.string.session_plan_with_level,
            levelName,
            planParts.joinToString(" + "),
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.today_mission_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    stringResource(R.string.today_mission_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.width(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(100.dp),
            ) {
                Text(
                    pluralStringResource(R.plurals.streak_days, streakDays, streakDays),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.session_level_choose),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(6.dp))
        SessionLevelSelector(
            selected = plan.level,
            onSelected = onSessionLevelChange,
            guided = plan.guidedStageIndex != null,
        )
        Spacer(Modifier.height(14.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                if (plan.guidedStageIndex != null && plan.guidedStageCount != null) {
                    Text(
                        if (plan.guidedAyahNumber != null) {
                            stringResource(
                                R.string.guided_session_stage_ayah,
                                plan.guidedStageIndex + 1,
                                plan.guidedStageCount,
                                plan.guidedAyahNumber,
                            )
                        } else {
                            stringResource(
                                R.string.guided_session_stage,
                                plan.guidedStageIndex + 1,
                                plan.guidedStageCount,
                            )
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    planSummary,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                )
                if (plan.currentGuidedLessonWaiting) {
                    Text(
                        stringResource(R.string.guided_quick_lesson_waiting),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MissionStat(
                        value = plan.reviewWordIds.size.toString(),
                        label = reviewSummary,
                        modifier = Modifier.weight(1f),
                    )
                    MissionStat(
                        value = when {
                            plan.lessonWordIds.isNotEmpty() -> "+${plan.lessonWordIds.size}"
                            guidedLessonCompletedToday -> "✓"
                            else -> "—"
                        },
                        label = lessonSummary,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (plan.requestsContextCheckpoint) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.session_context_included),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(R.string.activity_7_days),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(10.dp))
                WeeklyMissionActivity(activity)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !plan.isEmpty,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        if (plan.isEmpty) {
                            stringResource(
                                if (guidedMissionComplete) R.string.daily_arabic_goal_complete
                                else R.string.mission_all_caught_up,
                            )
                        } else {
                            pluralStringResource(
                                R.plurals.start_session_words,
                                plan.wordIds.size,
                                plan.wordIds.size,
                            )
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onOpenQuiz,
            modifier = Modifier.fillMaxWidth(),
            enabled = pathQuizReady,
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                stringResource(
                    if (progress.activeUnderstandPath == null) {
                        R.string.open_quiz
                    } else if (!pathQuizReady) {
                        R.string.understand_path_quiz_locked
                    } else {
                        R.string.understand_path_quiz
                    },
                ),
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (!lockScreenEnabled) {
            Spacer(Modifier.height(18.dp))
            LockScreenLearningCard(onEnableLockScreen = onEnableLockScreen)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MissionStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                value,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun WeeklyMissionActivity(activity: List<DailyMissionActivity>) {
    val locale = LocalConfiguration.current.locales[0]
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        activity.forEach { day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(Modifier.height(5.dp))
                Surface(
                    modifier = Modifier.size(34.dp),
                    color = if (day.completedReviews > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (day.completedReviews > 0) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    border = if (day.isToday) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
                    } else {
                        null
                    },
                    shape = CircleShape,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            if (day.completedReviews > 0) day.completedReviews.toString() else "·",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContextCheckpointScreen(
    question: ContextCheckpointQuestion,
    pronouncer: ArabicPronouncer,
    onAnswer: (String, Boolean) -> Unit,
    onContinue: () -> Unit,
) {
    var selectedOptionIndex by rememberSaveable(question.word.id) { mutableStateOf<Int?>(null) }
    var showCompleteAyah by rememberSaveable(question.word.id) { mutableStateOf(false) }
    val completeAyahSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val feedback = contextCheckpointFeedbackState(
        selectedOptionIndex = selectedOptionIndex,
        correctOptionIndex = question.correctOptionIndex,
    )
    val answered = feedback != ContextCheckpointFeedbackState.Unanswered
    val blankAyahDescription = stringResource(
        R.string.checkpoint_blank_ayah_description,
        question.ayah.text,
    )
    if (showCompleteAyah) {
        ModalBottomSheet(
            onDismissRequest = { showCompleteAyah = false },
            sheetState = completeAyahSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    stringResource(R.string.checkpoint_restored_ayah),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    question.word.reference,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                VerseExplorerPanel(
                    word = question.word,
                    highlightedWordIds = setOf(question.word.id),
                    showVersePronunciation = false,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    question.word.verseMeaning,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.checkpoint_scope_note),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(6.dp))
                VersePronunciationButton(
                    word = question.word,
                    pronouncer = pronouncer,
                    modifier = Modifier.fillMaxWidth(),
                    labelRes = R.string.hussary_verse_recitation,
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            stringResource(R.string.context_checkpoint_title),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(R.string.context_checkpoint_intro),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(10.dp))
        if (!answered) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.quiz_cloze_prompt),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        ArabicText(
                            question.ayah.text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = blankAyahDescription },
                            size = 27,
                            color = MaterialTheme.colorScheme.primary,
                            align = TextAlign.End,
                        )
                    }
                    Text(
                        question.word.reference,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            question.options.forEachIndexed { index, option ->
                val choiceDescription = stringResource(
                    R.string.checkpoint_choice_description,
                    index + 1,
                    option,
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    QuizOption(
                        text = option,
                        arabic = true,
                        selected = selectedOptionIndex == index,
                        correct = index == question.correctOptionIndex,
                        answered = answered,
                        accessibilityDescription = choiceDescription,
                        onClick = {
                            if (selectedOptionIndex == null) {
                                selectedOptionIndex = index
                                onAnswer(question.word.id, index == question.correctOptionIndex)
                            }
                        },
                    )
                }
                Spacer(Modifier.height(5.dp))
            }
        }

        if (answered) {
            Spacer(Modifier.height(3.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (feedback == ContextCheckpointFeedbackState.Correct) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        stringResource(
                            if (feedback == ContextCheckpointFeedbackState.Correct) {
                                R.string.checkpoint_correct
                            } else {
                                R.string.checkpoint_incorrect
                            },
                        ),
                        color = if (feedback == ContextCheckpointFeedbackState.Correct) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.checkpoint_correct_word),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        ArabicText(
                            question.word.arabic,
                            modifier = Modifier.fillMaxWidth().padding(6.dp),
                            size = 30,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(
                            R.string.checkpoint_contextual_meaning,
                            question.word.meaning,
                        ),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(
                            R.string.checkpoint_transliteration,
                            question.word.transliteration,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { showCompleteAyah = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.show_complete_ayah), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.continue_action), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
internal fun StudyCompletionScreen(
    payoff: StudyCompletionPayoff,
    recognizedWordIds: Set<String>,
    pronouncer: ArabicPronouncer,
    onAdvancePath: (() -> Unit)? = null,
    onFinish: () -> Unit,
) {
    var showInteractiveAyah by rememberSaveable(payoff.featuredWord.id) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.study_session_complete),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.completion_payoff_subtitle),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.words_practiced),
            modifier = Modifier.fillMaxWidth(),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            payoff.reviewedWords.forEach { word ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        ArabicText(
                            word.arabic,
                            size = 23,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(word.meaning, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    payoff.featuredWord.reference,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                if (!showInteractiveAyah) {
                    ArabicText(
                        payoff.featuredWord.verseArabic,
                        modifier = Modifier.fillMaxWidth(),
                        size = 30,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    payoff.featuredWord.verseMeaning,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        pluralStringResource(
                            R.plurals.words_recognized_in_ayah,
                            payoff.recognizedWordCount,
                            payoff.recognizedWordCount,
                        ),
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(10.dp))
                if (showInteractiveAyah) {
                    VerseExplorerPanel(
                        word = payoff.featuredWord,
                        highlightedWordIds = recognizedWordIds,
                        showVersePronunciation = false,
                    )
                    Spacer(Modifier.height(10.dp))
                    VersePronunciationButton(
                        word = payoff.featuredWord,
                        pronouncer = pronouncer,
                        modifier = Modifier.fillMaxWidth(),
                        labelRes = R.string.hussary_verse_recitation,
                    )
                } else {
                    Button(
                        onClick = { showInteractiveAyah = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Text(
                            stringResource(R.string.explore_ayah_word_by_word),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        if (onAdvancePath != null) {
            Button(
                onClick = onAdvancePath,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.understand_path_next_stage), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.finish_action), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
    }
}
