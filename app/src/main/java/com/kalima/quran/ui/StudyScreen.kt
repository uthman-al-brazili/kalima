package com.kalima.quran.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.WordRepository
import com.kalima.quran.ui.theme.Forest
import com.kalima.quran.ui.theme.Gold
import com.kalima.quran.ui.theme.Muted
import java.time.LocalDate

@Composable
fun StudyScreen(
    progress: StudyProgress,
    onAnswer: (String, Boolean) -> Unit,
    onEnableLockScreen: () -> Unit,
) {
    val selectionKey = progress.selectedSurahs.sorted().joinToString(",")
    val session = remember(progress.studyScope, selectionKey) {
        val words = WordRepository.wordsFor(progress.studyScope, progress.selectedSurahs)
        val start = words.indexOf(WordRepository.wordFor(LocalDate.now(), words)).coerceAtLeast(0)
        words.drop(start) + words.take(start)
    }
    var currentIndex by rememberSaveable(progress.studyScope.name, selectionKey) { mutableIntStateOf(0) }
    val word = session[currentIndex % session.size]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        StudyHeader(progress)
        if (!progress.lockScreenEnabled) {
            Spacer(Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Estude sempre que ligar a tela",
                            color = Forest,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Ative a função principal do Kalima para receber uma palavra a cada desbloqueio.",
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = onEnableLockScreen) {
                        Text("Ativar")
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        WordCard(word, progress)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    onAnswer(word.id, false)
                    currentIndex += 1
                },
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Revisar de novo", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = {
                    onAnswer(word.id, true)
                    currentIndex += 1
                },
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Forest),
            ) {
                Text("Já aprendi", fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "O significado acompanha este contexto. Uma mesma palavra pode mudar de nuance em outro versículo.",
            modifier = Modifier.fillMaxWidth(),
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StudyHeader(progress: StudyProgress) {
    val fraction = (progress.todayCompleted.toFloat() / progress.dailyGoal).coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("السَّلَامُ عَلَيْكُمْ", color = Forest, style = MaterialTheme.typography.titleMedium)
            Text("Sua palavra de hoje", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                when (progress.studyScope) {
                    StudyScope.All -> "Todo o conteúdo disponível"
                    StudyScope.Frequent -> "100 palavras mais frequentes"
                    StudyScope.Surahs -> if (progress.selectedSurahs.size <= 4) {
                        "Suras ${progress.selectedSurahs.sorted().joinToString(", ")}"
                    } else {
                        "${progress.selectedSurahs.size} suras selecionadas"
                    }
                },
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Surface(color = Gold.copy(alpha = 0.35f), shape = RoundedCornerShape(100.dp)) {
            Text(
                "🔥 ${progress.streakDays} dias",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.weight(1f).height(7.dp),
            color = Gold,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "${progress.todayCompleted}/${progress.dailyGoal}",
            color = Muted,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun WordCard(word: QuranWord, progress: StudyProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(100.dp)) {
                    Text(
                        word.category,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Forest,
                    )
                }
                WordStatusPill(progress.statusFor(word.id))
            }
            Spacer(Modifier.height(22.dp))
            ArabicText(word.arabic, modifier = Modifier.fillMaxWidth(), size = 50, color = Forest)
            Text(
                word.transliteration,
                color = Muted,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                word.meaning,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(18.dp))
            RootAndGrammar(word.root, word.grammar)
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth()) {
                Column {
                    Text(word.reference, color = Forest, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    ArabicText(
                        word.verseArabic,
                        modifier = Modifier.fillMaxWidth(),
                        size = 25,
                        align = TextAlign.End,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(word.verseMeaning, color = Muted, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(18.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    "💡 ${word.insight}",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Forest,
                )
            }
        }
    }
}
