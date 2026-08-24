package com.kalima.quran.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.data.StudyScope
import com.kalima.quran.ui.theme.Forest
import kotlin.math.roundToInt

private data class StarterPath(
    val scope: StudyScope,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
)

private val starterPaths = listOf(
    StarterPath(StudyScope.Frequent, R.string.path_top_100, R.string.path_top_100_desc),
    StarterPath(StudyScope.Prayer, R.string.path_prayer, R.string.path_prayer_desc),
    StarterPath(StudyScope.ShortSurahs, R.string.path_short_surahs, R.string.path_short_surahs_desc),
    StarterPath(StudyScope.All, R.string.path_explore_all, R.string.path_explore_all_desc),
)

@Composable
fun OnboardingScreen(onComplete: (StudyScope, Int, Boolean, Boolean) -> Unit) {
    var selectedScope by rememberSaveable { mutableStateOf(StudyScope.Frequent) }
    var dailyGoal by rememberSaveable { mutableIntStateOf(5) }
    var knowsArabicAlphabet by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var knowsArabicNumbers by rememberSaveable { mutableStateOf<Boolean?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Forest),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.onboarding_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.onboarding_lock_screen_title),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.onboarding_lock_screen_description),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.onboarding_foundations_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.onboarding_foundations_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(10.dp))
            KnowledgeQuestion(
                questionRes = R.string.onboarding_knows_alphabet,
                answer = knowsArabicAlphabet,
                onAnswer = { knowsArabicAlphabet = it },
            )
            Spacer(Modifier.height(9.dp))
            KnowledgeQuestion(
                questionRes = R.string.onboarding_knows_numbers,
                answer = knowsArabicNumbers,
                onAnswer = { knowsArabicNumbers = it },
            )
            if (knowsArabicAlphabet == false || knowsArabicNumbers == false) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            stringResource(R.string.onboarding_your_plan),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                        if (knowsArabicAlphabet == false) {
                            Text(
                                stringResource(R.string.onboarding_alphabet_plan),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (knowsArabicNumbers == false) {
                            Text(
                                stringResource(R.string.onboarding_numbers_plan),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            if (shouldShowWordStudySetup(knowsArabicAlphabet)) {
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.onboarding_path_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                starterPaths.forEach { path ->
                    val selected = path.scope == selectedScope
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedScope = path.scope },
                        shape = RoundedCornerShape(18.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected, onClick = { selectedScope = path.scope })
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(path.titleRes), fontWeight = FontWeight.Bold)
                                Text(
                                    stringResource(path.descriptionRes),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.onboarding_daily_goal, dailyGoal),
                    fontWeight = FontWeight.Bold,
                )
                Slider(
                    value = dailyGoal.toFloat(),
                    onValueChange = { dailyGoal = it.roundToInt() },
                    valueRange = 3f..15f,
                    steps = 11,
                )
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    onComplete(
                        selectedScope,
                        dailyGoal,
                        requireNotNull(knowsArabicAlphabet),
                        requireNotNull(knowsArabicNumbers),
                    )
                },
                enabled = knowsArabicAlphabet != null && knowsArabicNumbers != null,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    stringResource(
                        if (knowsArabicAlphabet == false) {
                            R.string.onboarding_start_alphabet
                        } else {
                            R.string.onboarding_start
                        },
                    ),
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

internal fun shouldShowWordStudySetup(knowsArabicAlphabet: Boolean?): Boolean =
    knowsArabicAlphabet == true

@Composable
private fun KnowledgeQuestion(
    @StringRes questionRes: Int,
    answer: Boolean?,
    onAnswer: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(stringResource(questionRes), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = answer == true,
                    onClick = { onAnswer(true) },
                    label = { Text(stringResource(R.string.yes)) },
                )
                FilterChip(
                    selected = answer == false,
                    onClick = { onAnswer(false) },
                    label = { Text(stringResource(R.string.no)) },
                )
            }
        }
    }
}
