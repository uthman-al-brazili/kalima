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
import androidx.compose.material3.HorizontalDivider
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
import com.kalima.quran.data.UnderstandPathId
import com.kalima.quran.ui.theme.Forest
import kotlin.math.roundToInt

internal data class StarterPlan(
    val scope: StudyScope,
    val understandPath: UnderstandPathId?,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
)

internal val starterPlans = listOf(
    StarterPlan(
        scope = StudyScope.Prayer,
        understandPath = UnderstandPathId.AlFatihahSevenDays,
        titleRes = R.string.understand_path_fatihah_title,
        descriptionRes = R.string.understand_path_fatihah_description,
    ),
    StarterPlan(
        scope = StudyScope.ShortSurahs,
        understandPath = UnderstandPathId.LastTenSurahs,
        titleRes = R.string.understand_path_last_ten_title,
        descriptionRes = R.string.understand_path_last_ten_description,
    ),
    StarterPlan(
        scope = StudyScope.Frequent,
        understandPath = null,
        titleRes = R.string.path_top_100,
        descriptionRes = R.string.path_top_100_desc,
    ),
    StarterPlan(
        scope = StudyScope.All,
        understandPath = null,
        titleRes = R.string.path_explore_all,
        descriptionRes = R.string.path_explore_all_desc,
    ),
)

@Composable
fun OnboardingScreen(onComplete: (StudyScope, UnderstandPathId?, Int, Boolean, Boolean) -> Unit) {
    var selectedPlanIndex by rememberSaveable { mutableIntStateOf(2) }
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Forest),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.onboarding_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.onboarding_subtitle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        stringResource(R.string.onboarding_lock_screen_title),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.onboarding_lock_screen_description),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.onboarding_foundations_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.onboarding_foundations_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(6.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    KnowledgeQuestion(
                        questionRes = R.string.onboarding_knows_alphabet,
                        answer = knowsArabicAlphabet,
                        onAnswer = { knowsArabicAlphabet = it },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    KnowledgeQuestion(
                        questionRes = R.string.onboarding_knows_numbers,
                        answer = knowsArabicNumbers,
                        onAnswer = { knowsArabicNumbers = it },
                    )
                    if (knowsArabicAlphabet == false || knowsArabicNumbers == false) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Text(
                            stringResource(R.string.onboarding_your_plan),
                            modifier = Modifier.padding(top = 6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                        )
                        if (knowsArabicAlphabet == false) {
                            Text(
                                stringResource(R.string.onboarding_alphabet_plan),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (knowsArabicNumbers == false) {
                            Text(
                                stringResource(R.string.onboarding_numbers_plan),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            if (shouldShowWordStudySetup(knowsArabicAlphabet)) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.onboarding_path_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                starterPlans.chunked(2).forEachIndexed { index, rowPlans ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowPlans.forEach { plan ->
                            CompactPlanChoice(
                                plan = plan,
                                selected = starterPlans.indexOf(plan) == selectedPlanIndex,
                                onClick = { selectedPlanIndex = starterPlans.indexOf(plan) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (index < starterPlans.chunked(2).lastIndex) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
                val selectedPlan = starterPlans[selectedPlanIndex]
                Text(
                    stringResource(selectedPlan.descriptionRes),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.onboarding_daily_goal, dailyGoal),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Slider(
                        value = dailyGoal.toFloat(),
                        onValueChange = { dailyGoal = it.roundToInt() },
                        valueRange = 3f..15f,
                        steps = 11,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val selectedPlan = starterPlans[selectedPlanIndex]
                    onComplete(
                        selectedPlan.scope,
                        selectedPlan.understandPath,
                        dailyGoal,
                        requireNotNull(knowsArabicAlphabet),
                        requireNotNull(knowsArabicNumbers),
                    )
                },
                enabled = knowsArabicAlphabet != null && knowsArabicNumbers != null,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
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
            Spacer(Modifier.height(12.dp))
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(questionRes),
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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

@Composable
private fun CompactPlanChoice(
    plan: StarterPlan,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Text(
                stringResource(plan.titleRes),
                modifier = Modifier.padding(end = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
