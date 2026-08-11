package com.kalima.quran.ui

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.WordRepository
import com.kalima.quran.ui.theme.Forest
import com.kalima.quran.ui.theme.Gold
import com.kalima.quran.ui.theme.Muted
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(
    progress: StudyProgress,
    onLockScreenChange: (Boolean) -> Unit,
    onLockScreenQuizChange: (Boolean) -> Unit,
    onLockScreenQuizIntervalChange: (Int) -> Unit,
    onReminderChange: (Boolean) -> Unit,
    onDailyGoalChange: (Int) -> Unit,
    onStudyScopeChange: (StudyScope) -> Unit,
    onToggleSurah: (Int) -> Unit,
    onOpenAppSettings: () -> Unit,
    onPreviewLockScreen: () -> Unit,
) {
    val selectionKey = progress.selectedSurahs.sorted().joinToString(",")
    val activeWords = remember(progress.studyScope, selectionKey) {
        WordRepository.wordsFor(progress.studyScope, progress.selectedSurahs)
    }
    val learnedInScope = remember(activeWords, progress.learnedIds) {
        activeWords.count { it.id in progress.learnedIds }
    }
    val learnedFraction = learnedInScope.toFloat() / activeWords.size
    var showSurahDialog by rememberSaveable { mutableStateOf(false) }

    if (showSurahDialog) {
        SurahSelectionDialog(
            selectedSurahs = progress.selectedSurahs,
            onToggleSurah = onToggleSurah,
            onDismiss = { showSurahDialog = false },
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Seu progresso", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Consistência pequena, compreensão duradoura.", color = Muted)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBlock(
                value = progress.learnedIds.size.toString(),
                label = "aprendidas",
                modifier = Modifier.weight(1f),
            )
            StatBlock(
                value = progress.reviewingIds.size.toString(),
                label = "em revisão",
                modifier = Modifier.weight(1f),
            )
            StatBlock(
                value = "🔥 ${progress.streakDays}",
                label = "dias",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(14.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Forest),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Conteúdo selecionado", color = Gold, fontWeight = FontWeight.Bold)
                    Text(
                        "$learnedInScope/${activeWords.size}",
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { learnedFraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(9.dp),
                    color = Gold,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "O progresso acima acompanha apenas o conjunto que você escolheu estudar agora.",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Escolher palavras", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "A escolha também controla os cartões exibidos ao ligar a tela.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(10.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 1.dp,
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = progress.studyScope == StudyScope.All,
                        onClick = { onStudyScopeChange(StudyScope.All) },
                        label = { Text("Tudo") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = progress.studyScope == StudyScope.Frequent,
                        onClick = { onStudyScopeChange(StudyScope.Frequent) },
                        label = { Text("Mais usadas") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = progress.studyScope == StudyScope.Surahs,
                        onClick = { onStudyScopeChange(StudyScope.Surahs) },
                        label = { Text("Por sura") },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (progress.studyScope == StudyScope.Surahs) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (progress.selectedSurahs.size == 1) {
                            "1 sura selecionada: ${progress.selectedSurahs.first()}"
                        } else {
                            "${progress.selectedSurahs.size} suras selecionadas"
                        },
                        color = Forest,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (progress.selectedSurahs.isNotEmpty()) {
                        Text(
                            progress.selectedSurahs.sorted().take(8).joinToString(", ") +
                                if (progress.selectedSurahs.size > 8) "…" else "",
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showSurahDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Pesquisar e escolher suras")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "${activeWords.size} cartões no estudo atual",
                    color = Forest,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Rotina", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Estudo ao ligar a tela", color = Forest, fontWeight = FontWeight.Bold)
                        Text(
                            if (progress.lockScreenEnabled) {
                                "Ativo: uma nova palavra será aberta sempre que a tela acender."
                            } else {
                                "Requer a permissão “Aparecer sobre outros apps” e mantém um serviço ativo."
                            },
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = progress.lockScreenEnabled,
                        onCheckedChange = onLockScreenChange,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onPreviewLockScreen) {
                        Text("Testar cartão")
                    }
                    TextButton(onClick = onOpenAppSettings) {
                        Text("Configurações")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Quiz ao ligar a tela", color = Forest, fontWeight = FontWeight.Bold)
                        Text(
                            if (progress.lockScreenQuizEnabled) {
                                "Um quiz aparecerá depois de ${progress.lockScreenQuizInterval} palavra${if (progress.lockScreenQuizInterval == 1) "" else "s"}."
                            } else {
                                "Opcional: intercale perguntas de quatro alternativas com os cartões."
                            },
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = progress.lockScreenQuizEnabled,
                        onCheckedChange = onLockScreenQuizChange,
                    )
                }
                if (progress.lockScreenQuizEnabled) {
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Intervalo", fontWeight = FontWeight.SemiBold)
                        Text(
                            "${progress.lockScreenQuizInterval} palavra${if (progress.lockScreenQuizInterval == 1) "" else "s"}",
                            color = Forest,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Slider(
                        value = progress.lockScreenQuizInterval.toFloat(),
                        onValueChange = { onLockScreenQuizIntervalChange(it.roundToInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 1.dp,
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Lembrete diário", fontWeight = FontWeight.Bold)
                        Text(
                            "Notificação diária às 8h, respeitando as permissões do Android.",
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(checked = progress.reminderEnabled, onCheckedChange = onReminderChange)
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Meta diária", fontWeight = FontWeight.Bold)
                    Text("${progress.dailyGoal} palavras", color = Forest, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = progress.dailyGoal.toFloat(),
                    onValueChange = { onDailyGoalChange(it.roundToInt()) },
                    valueRange = 3f..20f,
                    steps = 16,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(
                "Nota editorial: raízes, morfologia e traduções de estudo devem passar por revisão de um especialista antes da publicação.",
                modifier = Modifier.padding(16.dp),
                color = Forest,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
