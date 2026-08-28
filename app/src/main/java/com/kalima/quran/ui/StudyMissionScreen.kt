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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.audio.ArabicPronouncer
import java.time.format.TextStyle

@Composable
internal fun DailyMissionScreen(
    mission: DailyMissionState,
    streakDays: Int,
    canStart: Boolean,
    lockScreenEnabled: Boolean,
    onEnableLockScreen: () -> Unit,
    onStart: () -> Unit,
) {
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
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
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
        Spacer(Modifier.height(20.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    if (mission.goalComplete) {
                        stringResource(R.string.daily_arabic_goal_complete)
                    } else {
                        stringResource(
                            R.string.mission_words_completed,
                            mission.completedWords,
                            mission.goalWords,
                        )
                    },
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                )
                if (mission.goalComplete) {
                    Text(
                        stringResource(
                            R.string.mission_words_completed,
                            mission.completedWords,
                            mission.goalWords,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = {
                        (mission.completedWords.toFloat() / mission.goalWords.coerceAtLeast(1))
                            .coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth().height(9.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MissionStat(
                        value = mission.dueReviews.toString(),
                        label = pluralStringResource(
                            R.plurals.mission_reviews_due,
                            mission.dueReviews,
                            mission.dueReviews,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    MissionStat(
                        value = if (mission.newWordsReady > 0) "+${mission.newWordsReady}" else "—",
                        label = if (mission.newWordsReady > 0) {
                            pluralStringResource(
                                R.plurals.mission_new_words_ready,
                                mission.newWordsReady,
                            )
                        } else {
                            stringResource(R.string.mission_no_new_word)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(R.string.activity_7_days),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(10.dp))
                WeeklyMissionActivity(mission.activity)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = canStart,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        when {
                            !canStart -> stringResource(R.string.mission_all_caught_up)
                            mission.goalComplete -> stringResource(R.string.keep_practicing)
                            else -> pluralStringResource(
                                R.plurals.continue_words,
                                mission.remainingWords,
                                mission.remainingWords,
                            )
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
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

@Composable
internal fun StudyCompletionScreen(
    payoff: StudyCompletionPayoff,
    recognizedWordIds: Set<String>,
    pronouncer: ArabicPronouncer,
    onFinish: () -> Unit,
) {
    var showInteractiveAyah by rememberSaveable(payoff.featuredWord.id) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.daily_arabic_goal_complete),
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
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.words_practiced),
            modifier = Modifier.fillMaxWidth(),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            payoff.reviewedWords.forEach { word ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        ArabicText(
                            word.arabic,
                            size = 24,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(word.meaning, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    payoff.featuredWord.reference,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                if (!showInteractiveAyah) {
                    ArabicText(
                        payoff.featuredWord.verseArabic,
                        modifier = Modifier.fillMaxWidth(),
                        size = 32,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    payoff.featuredWord.verseMeaning,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(14.dp))
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
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(14.dp))
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
                        modifier = Modifier.fillMaxWidth().height(52.dp),
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
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.finish_action), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
    }
}
