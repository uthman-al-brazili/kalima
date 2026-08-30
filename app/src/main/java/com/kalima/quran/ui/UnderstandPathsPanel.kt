package com.kalima.quran.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.data.GeneratedQuranSurahs
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.UnderstandPathCatalog
import com.kalima.quran.data.UnderstandPathId
import com.kalima.quran.data.UnderstandPathMetric
import com.kalima.quran.data.UnderstandPathProgress
import com.kalima.quran.data.UnderstandPathStage
import com.kalima.quran.data.UnderstandPathState
import com.kalima.quran.data.WordRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnderstandPathLauncher(
    progress: StudyProgress,
    onSelectPath: (UnderstandPathId?) -> Unit,
    onAdvancePath: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeId = progress.activeUnderstandPath
    val activeState = remember(activeId, progress) {
        activeId?.let { UnderstandPathProgress.calculate(progress, it, WordRepository.words) }
    }
    var showPathManager by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showPathManager) {
        ModalBottomSheet(
            onDismissRequest = { showPathManager = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                UnderstandPathsPanel(
                    progress = progress,
                    onSelectPath = { pathId ->
                        onSelectPath(pathId)
                        showPathManager = false
                    },
                    onAdvancePath = onAdvancePath,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    Card(
        onClick = { showPathManager = true },
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(
                        if (activeId == null) {
                            R.string.study_plan_no_focus
                        } else {
                            R.string.study_plan_current_focus
                        },
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (activeId != null && activeState != null) {
                    Text(
                        understandPathTitle(activeId),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        understandPathStageLabel(
                            stage = activeState.definition.stages[activeState.currentStageIndex],
                            stageIndex = activeState.currentStageIndex,
                            stageCount = activeState.definition.stages.size,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(
                        stringResource(R.string.understand_paths_intro),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                    )
                }
            }
            Text(
                stringResource(
                    if (activeId == null) {
                        R.string.understand_path_choose
                    } else {
                        R.string.understand_path_manage
                    },
                ),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
internal fun UnderstandPathsPanel(
    progress: StudyProgress,
    onSelectPath: (UnderstandPathId?) -> Unit,
    onAdvancePath: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeId = progress.activeUnderstandPath
    val activeState = remember(activeId, progress) {
        activeId?.let { UnderstandPathProgress.calculate(progress, it, WordRepository.words) }
    }
    var showChoices by rememberSaveable(activeId?.name) { mutableStateOf(activeId == null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.understand_paths_title),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.understand_paths_intro),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))

            if (activeId != null && activeState != null && !showChoices) {
                ActivePathSummary(
                    progress = progress,
                    pathId = activeId,
                    state = activeState,
                    onChange = { showChoices = true },
                    onPause = { onSelectPath(null) },
                    onAdvance = onAdvancePath,
                )
            } else {
                UnderstandPathCatalog.definitions.forEachIndexed { index, definition ->
                    PathChoiceCard(
                        pathId = definition.id,
                        completed = definition.id in progress.completedUnderstandPaths,
                        onStart = {
                            onSelectPath(definition.id)
                            showChoices = false
                        },
                    )
                    if (index != UnderstandPathCatalog.definitions.lastIndex) {
                        Spacer(Modifier.height(10.dp))
                    }
                }
                if (activeId != null) {
                    TextButton(
                        onClick = { showChoices = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.understand_path_cancel_change))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivePathSummary(
    progress: StudyProgress,
    pathId: UnderstandPathId,
    state: UnderstandPathState,
    onChange: () -> Unit,
    onPause: () -> Unit,
    onAdvance: () -> Unit,
) {
    val completed = pathId in progress.completedUnderstandPaths || state.meetsCompletionGoal
    Surface(
        color = if (completed) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                stringResource(
                    if (completed) R.string.understand_path_completed else R.string.understand_path_active,
                ),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                understandPathTitle(pathId),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                understandPathStageLabel(
                    stage = state.definition.stages[state.currentStageIndex],
                    stageIndex = state.currentStageIndex,
                    stageCount = state.definition.stages.size,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))
            PathMetric(metric = state.metric, vocabulary = true)
            Spacer(Modifier.height(10.dp))
            PathMetric(metric = state.metric, vocabulary = false)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.understand_path_goal_note),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
            if (
                state.currentStageReadyToAdvance &&
                state.currentStageIndex < state.definition.stages.lastIndex
            ) {
                Spacer(Modifier.height(10.dp))
                Button(onClick = onAdvance, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.understand_path_next_stage))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onChange, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.understand_path_switch))
                }
                TextButton(onClick = onPause, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.understand_path_pause))
                }
            }
        }
    }
}

@Composable
private fun PathChoiceCard(
    pathId: UnderstandPathId,
    completed: Boolean,
    onStart: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    understandPathTitle(pathId),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (completed) {
                    Text(
                        stringResource(R.string.understand_path_completed_badge),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                understandPathDescription(pathId),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.understand_path_start))
            }
        }
    }
}

@Composable
private fun PathMetric(metric: UnderstandPathMetric, vocabulary: Boolean) {
    val percent = if (vocabulary) metric.coveragePercent else metric.recallPercent
    Text(
        if (vocabulary) {
            stringResource(R.string.understand_path_vocabulary_metric, percent)
        } else {
            stringResource(R.string.understand_path_recall_metric, percent)
        },
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.bodyMedium,
    )
    LinearProgressIndicator(
        progress = { percent / 100f },
        modifier = Modifier.fillMaxWidth().height(7.dp),
    )
    Text(
        if (vocabulary) {
            stringResource(
                R.string.understand_path_vocabulary_detail,
                metric.recognizedOccurrences,
                metric.totalOccurrences,
            )
        } else {
            stringResource(
                R.string.understand_path_recall_detail,
                metric.recalledConcepts,
                metric.totalConcepts,
            )
        },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
internal fun understandPathTitle(pathId: UnderstandPathId): String = stringResource(
    when (pathId) {
        UnderstandPathId.AlFatihahSevenDays -> R.string.understand_path_fatihah_title
        UnderstandPathId.LastTenSurahs -> R.string.understand_path_last_ten_title
    },
)

@Composable
private fun understandPathDescription(pathId: UnderstandPathId): String = stringResource(
    when (pathId) {
        UnderstandPathId.AlFatihahSevenDays -> R.string.understand_path_fatihah_description
        UnderstandPathId.LastTenSurahs -> R.string.understand_path_last_ten_description
    },
)

@Composable
private fun understandPathStageLabel(
    stage: UnderstandPathStage,
    stageIndex: Int,
    stageCount: Int,
): String = if (stage.ayahNumber != null) {
    stringResource(R.string.understand_path_ayah_stage, stageIndex + 1, stageCount)
} else {
    val surahName = remember(stage.surahNumber) {
        GeneratedQuranSurahs.all.firstOrNull { it.number == stage.surahNumber }
            ?.transliteratedName
            ?: stage.surahNumber.toString()
    }
    stringResource(
        R.string.understand_path_surah_stage,
        stageIndex + 1,
        stageCount,
        surahName,
    )
}
