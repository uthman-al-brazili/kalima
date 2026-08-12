package com.kalima.quran.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kalima.quran.data.AppThemeMode
import com.kalima.quran.data.LearningWordLimiter
import com.kalima.quran.data.QuranSurah
import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.ReviewHistory
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.WordRepository
import com.kalima.quran.data.WordStatus
import com.kalima.quran.data.limitNewWords
import com.kalima.quran.localization.AppLanguage
import java.time.Instant
import java.time.LocalDate

private enum class LibraryFilter {
    All,
    New,
    Reviewing,
    Learned,
    Favorites,
}

@Composable
fun LibraryScreen(
    store: DesktopProgressStore,
    selectedWordId: String?,
    onSelectedWord: (String?) -> Unit,
) {
    val progress = store.progress
    val language = store.language
    val scopedWords = remember(
        progress.studyScope,
        progress.selectedSurahs,
        progress.favoriteIds,
        progress.customStudyIds,
    ) {
        WordRepository.wordsFor(progress.studyScope, progress.selectedSurahs, progress.favoriteIds, progress.customStudyIds)
    }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(LibraryFilter.All) }
    val filtered = remember(query, filter, scopedWords, progress.learnedIds, progress.reviewingIds, progress.favoriteIds) {
        WordRepository.search(query, scopedWords).filter { word ->
            when (filter) {
                LibraryFilter.All -> true
                LibraryFilter.New -> progress.statusFor(word.id) == WordStatus.New
                LibraryFilter.Reviewing -> progress.statusFor(word.id) == WordStatus.Reviewing
                LibraryFilter.Learned -> progress.statusFor(word.id) == WordStatus.Learned
                LibraryFilter.Favorites -> word.id in progress.favoriteIds
            }
        }
    }
    val selected = selectedWordId?.let { id -> WordRepository.words.firstOrNull { it.id == id } }
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(0.48f).fillMaxHeight().padding(28.dp)) {
            Text(language.t("Biblioteca", "Library"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                language.t("${scopedWords.size} palavras no caminho atual", "${scopedWords.size} words in the current path"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(language.t("Buscar palavra, raiz ou referência", "Search word, root, or reference")) },
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                LibraryFilter.entries.forEach { option ->
                    val label = when (option) {
                        LibraryFilter.All -> language.t("Todas", "All")
                        LibraryFilter.New -> language.t("Novas", "New")
                        LibraryFilter.Reviewing -> language.t("Em revisão", "Reviewing")
                        LibraryFilter.Learned -> language.t("Aprendidas", "Learned")
                        LibraryFilter.Favorites -> language.t("Favoritas", "Favorites")
                    }
                    FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                language.t("${filtered.size} resultados", "${filtered.size} results"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(filtered, key = QuranWord::id) { word ->
                    LibraryRow(word, progress, selectedWordId == word.id, language) { onSelectedWord(word.id) }
                }
            }
        }
        Surface(
            modifier = Modifier.weight(0.52f).fillMaxHeight(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        ) {
            if (selected == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⌕", fontSize = 56.sp, color = MaterialTheme.colorScheme.primary)
                        Text(language.t("Selecione uma palavra", "Select a word"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            language.t("Os detalhes aparecerão aqui.", "Details will appear here."),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LibraryDetail(selected, progress, language, store)
            }
        }
    }
}

@Composable
private fun LibraryRow(
    word: QuranWord,
    progress: StudyProgress,
    selected: Boolean,
    language: AppLanguage,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(word.arabic, fontSize = 25.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                Text(word.meaning, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text("${word.transliteration}  •  ${word.reference}", maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (word.id in progress.favoriteIds) Text("★", color = MaterialTheme.colorScheme.primary)
                Text(progress.statusFor(word.id).label(language), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LibraryDetail(
    word: QuranWord,
    progress: StudyProgress,
    language: AppLanguage,
    store: DesktopProgressStore,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp)) {
                Text(progress.statusFor(word.id).label(language), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.primary)
            }
            Text(word.reference, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        Text(word.arabic, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, fontSize = 50.sp, lineHeight = 66.sp, color = MaterialTheme.colorScheme.primary)
        Text(word.transliteration, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Text(word.meaning, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { DesktopPronouncer.speak(word.arabic) }) { Text("▶  ${language.t("Ouvir", "Listen")}") }
            OutlinedButton(onClick = { store.toggleFavorite(word.id) }) {
                Text((if (word.id in progress.favoriteIds) "★ " else "☆ ") + language.t("Favorita", "Favorite"))
            }
            OutlinedButton(onClick = { store.toggleCustomStudy(word.id) }) {
                Text((if (word.id in progress.customStudyIds) "✓ " else "+ ") + language.t("Lista", "List"))
            }
        }
        Spacer(Modifier.height(20.dp))
        DetailBlock(language.t("Raiz e gramática", "Root and grammar"), "${word.root}  •  ${word.grammar}\n${word.category}")
        Spacer(Modifier.height(12.dp))
        DetailBlock(language.t("Contexto corânico", "Quranic context"), word.verseArabic, arabic = true)
        Spacer(Modifier.height(12.dp))
        DetailBlock(language.t("Sentido de apoio", "Supporting meaning"), word.verseMeaning)
        Spacer(Modifier.height(12.dp))
        DetailBlock(language.t("Nota de estudo", "Study note"), word.insight)
        Spacer(Modifier.height(16.dp))
        Text(
            language.t(
                "Rascunho editorial — conteúdo ainda requer revisão especializada.",
                "Editorial draft — content still requires specialist review.",
            ),
            color = MaterialTheme.colorScheme.tertiary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun DetailBlock(title: String, body: String, arabic: Boolean = false) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                body,
                modifier = Modifier.fillMaxWidth(),
                textAlign = if (arabic) TextAlign.End else TextAlign.Start,
                fontSize = if (arabic) 25.sp else 15.sp,
                lineHeight = if (arabic) 39.sp else 22.sp,
            )
        }
    }
}

@Composable
fun ProgressScreen(store: DesktopProgressStore) {
    val progress = store.progress
    val language = store.language
    var showSurahs by remember { mutableStateOf(false) }
    val scopedWords = remember(
        progress.studyScope,
        progress.selectedSurahs,
        progress.favoriteIds,
        progress.customStudyIds,
    ) { WordRepository.wordsFor(progress.studyScope, progress.selectedSurahs, progress.favoriteIds, progress.customStudyIds) }
    val activeWords = remember(scopedWords, progress.maximumWords, progress.learnedIds, progress.reviewingIds) {
        progress.limitNewWords(scopedWords)
    }
    val activeIds = activeWords.mapTo(mutableSetOf(), QuranWord::id)
    val accuracy = progress.accuracy(30) ?: 0
    val difficult = progress.reviewSchedules.entries
        .filter { it.value.lapses > 0 && it.key in activeIds }
        .sortedByDescending { it.value.lapses }
        .take(6)
        .mapNotNull { entry -> WordRepository.words.firstOrNull { it.id == entry.key }?.let { it to entry.value.lapses } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(34.dp)) {
        Text(language.t("Progresso", "Progress"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            language.t("Seu aprendizado permanece somente neste computador.", "Your learning stays only on this computer."),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(language.t("Aprendidas", "Learned"), progress.learnedIds.count(activeIds::contains).toString(), "✓", Modifier.weight(1f))
            StatCard(language.t("Em revisão", "Reviewing"), progress.reviewingIds.count(activeIds::contains).toString(), "↻", Modifier.weight(1f))
            StatCard(language.t("Vencidas", "Due"), progress.dueReviewCount(activeIds).toString(), "!", Modifier.weight(1f))
            StatCard(language.t("Precisão 30d", "30d accuracy"), "$accuracy%", "◎", Modifier.weight(1f))
            StatCard(language.t("Sequência", "Streak"), "${progress.streakDays}", "◇", Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        SectionCard(language.t("Caminho de estudo", "Study path")) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StudyScope.entries.forEach { scope ->
                    FilterChip(
                        selected = progress.studyScope == scope,
                        onClick = {
                            if (scope == StudyScope.Surahs) showSurahs = true else store.setStudyScope(scope)
                        },
                        label = { Text(scope.label(language)) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                language.t("${activeWords.size} palavras disponíveis", "${activeWords.size} words available"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (progress.studyScope == StudyScope.Surahs) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { showSurahs = true }) {
                    Text(language.t("Escolher suras (${progress.selectedSurahs.size})", "Choose surahs (${progress.selectedSurahs.size})"))
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            SectionCard(language.t("Atividade — últimos 14 dias", "Activity — last 14 days"), Modifier.weight(1.2f)) {
                ActivityChart(progress, language)
            }
            SectionCard(language.t("Palavras difíceis", "Difficult words"), Modifier.weight(0.8f)) {
                if (difficult.isEmpty()) {
                    Text(language.t("Nenhum lapso registrado ainda.", "No lapses recorded yet."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    difficult.forEach { (word, lapses) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(word.arabic, modifier = Modifier.width(90.dp), fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                            Text(word.meaning, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$lapses×", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        SectionCard(language.t("Resumo do quiz", "Quiz summary")) {
            val quizAccuracy = if (progress.quizTotalAnswers == 0) 0 else progress.quizCorrectAnswers * 100 / progress.quizTotalAnswers
            Text(
                language.t(
                    "${progress.quizCorrectAnswers} acertos em ${progress.quizTotalAnswers} respostas • $quizAccuracy% de precisão",
                    "${progress.quizCorrectAnswers} correct out of ${progress.quizTotalAnswers} answers • $quizAccuracy% accuracy",
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(28.dp))
    }
    if (showSurahs) {
        SurahDialog(store, language) { showSurahs = false }
    }
}

@Composable
private fun StatCard(label: String, value: String, glyph: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(glyph, color = MaterialTheme.colorScheme.primary, fontSize = 22.sp)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun ActivityChart(progress: StudyProgress, language: AppLanguage) {
    val today = LocalDate.now()
    val counts = ReviewHistory.countByDay(progress.reviewEvents)
    val days = (13 downTo 0).map { today.minusDays(it.toLong()) }
    val maximum = days.maxOfOrNull { counts[it] ?: 0 }?.coerceAtLeast(1) ?: 1
    Row(Modifier.fillMaxWidth().height(145.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        days.forEach { day ->
            val count = counts[day] ?: 0
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                if (count > 0) Text("$count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier.fillMaxWidth().height((10 + 84 * count / maximum).dp)
                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                        .background(if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                )
                Spacer(Modifier.height(5.dp))
                Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SurahDialog(store: DesktopProgressStore, language: AppLanguage, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val selected = store.progress.selectedSurahs
    val filtered = remember(query) {
        val term = query.trim().lowercase()
        if (term.isEmpty()) WordRepository.selectableSurahs else WordRepository.selectableSurahs.filter {
            it.number.toString().contains(term) || it.transliteratedName.lowercase().contains(term) || it.arabicName.contains(term)
        }
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(language.t("Escolher suras", "Choose surahs"), fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.width(520.dp).heightIn(max = 520.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(language.t("Número ou nome da sura", "Surah number or name")) },
                )
                Spacer(Modifier.height(10.dp))
                Text(language.t("${selected.size} selecionadas", "${selected.size} selected"), color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                LazyColumn {
                    items(filtered, key = QuranSurah::number) { surah ->
                        Row(
                            Modifier.fillMaxWidth().clickable { store.toggleSurah(surah.number) }.padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = surah.number in selected, onCheckedChange = { store.toggleSurah(surah.number) })
                            Text("${surah.number}. ${surah.transliteratedName}", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Text(surah.arabicName, fontSize = 19.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onClose) { Text(language.t("Concluir", "Done")) } },
    )
}

@Composable
fun SettingsScreen(store: DesktopProgressStore) {
    val progress = store.progress
    val language = store.language
    var maximumText by remember(progress.maximumWords) {
        mutableStateOf(if (progress.maximumWords == LearningWordLimiter.UNLIMITED) "" else progress.maximumWords.toString())
    }
    var showReset by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(34.dp)) {
        Text(language.t("Configurações", "Settings"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(language.t("Ajuste sua experiência no Windows.", "Adjust your Windows experience."), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(22.dp))
        SettingsBlock(language.t("Idioma", "Language")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { option ->
                    FilterChip(
                        selected = language == option,
                        onClick = { store.changeLanguage(option) },
                        label = { Text(if (option == AppLanguage.Portuguese) "Português" else "English") },
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        SettingsBlock(language.t("Aparência", "Appearance")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppThemeMode.entries.forEach { mode ->
                    FilterChip(selected = progress.themeMode == mode, onClick = { store.setThemeMode(mode) }, label = { Text(mode.label(language)) })
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        SettingsBlock(language.t("Plano de estudo", "Study plan")) {
            Text(language.t("Meta diária", "Daily goal"), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 5, 10, 15, 20).forEach { goal ->
                    FilterChip(selected = progress.dailyGoal == goal, onClick = { store.setDailyGoal(goal) }, label = { Text("$goal") })
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(language.t("Máximo de palavras em aprendizado", "Maximum learning words"), fontWeight = FontWeight.SemiBold)
            Text(
                language.t("Deixe vazio para não limitar. Palavras já iniciadas nunca são removidas.", "Leave blank for no limit. Started words are never removed."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = maximumText,
                    onValueChange = { value -> if (value.all(Char::isDigit) && value.length <= 6) maximumText = value },
                    modifier = Modifier.width(180.dp),
                    singleLine = true,
                    label = { Text(language.t("Sem limite", "Unlimited")) },
                )
                Spacer(Modifier.width(10.dp))
                Button(onClick = {
                    val value = maximumText.toIntOrNull() ?: LearningWordLimiter.UNLIMITED
                    store.setMaximumWords(value)
                    maximumText = if (value == LearningWordLimiter.UNLIMITED) "" else value.coerceAtLeast(1).toString()
                }) { Text(language.t("Aplicar", "Apply")) }
            }
        }
        Spacer(Modifier.height(14.dp))
        SettingsBlock(language.t("Lembrete no Windows", "Windows reminder")) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(language.t("Lembrete diário", "Daily reminder"), fontWeight = FontWeight.SemiBold)
                    Text(
                        language.t("Mostra uma notificação enquanto o Kalima estiver aberto.", "Shows a notification while Kalima is open."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = progress.reminderEnabled, onCheckedChange = store::setReminderEnabled)
            }
            if (progress.reminderEnabled) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(language.t("Horário:", "Time:"), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(12.dp))
                    TimeStepper(store.reminderHour, 0..23) { store.setReminderTime(it, store.reminderMinute) }
                    Text(" : ", fontWeight = FontWeight.Bold)
                    TimeStepper(store.reminderMinute, 0..59, step = 5) { store.setReminderTime(store.reminderHour, it) }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        SettingsBlock(language.t("Dados locais", "Local data")) {
            Text(language.t("Pasta de dados", "Data folder"), fontWeight = FontWeight.SemiBold)
            Text(store.dataDirectory.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { runCatching { ProcessBuilder("explorer.exe", store.dataDirectory.toString()).start() } }) {
                    Text(language.t("Abrir pasta", "Open folder"))
                }
                OutlinedButton(onClick = { showReset = true }) {
                    Text(language.t("Zerar progresso", "Reset progress"), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        SettingsBlock(language.t("Sobre esta versão", "About this version")) {
            Text("Kalima 0.14.0 — Windows", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                language.t(
                    "Aplicativo offline para estudo de vocabulário do árabe corânico. A integração com a tela de bloqueio permanece exclusiva do Android porque a tela segura do Windows não aceita sobreposição de apps comuns.",
                    "Offline Quranic Arabic vocabulary study. Lock-screen integration remains Android-only because the secure Windows sign-in screen does not allow regular app overlays.",
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                language.t("Conteúdo em rascunho editorial; revisão especializada pendente.", "Editorial draft content; specialist review pending."),
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(30.dp))
    }
    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text(language.t("Zerar todo o progresso?", "Reset all progress?"), fontWeight = FontWeight.Bold) },
            text = { Text(language.t("Esta ação remove revisões, sequência, favoritos e histórico neste computador.", "This removes reviews, streak, favorites, and history on this computer.")) },
            confirmButton = {
                Button(onClick = { store.resetProgress(); showReset = false }) { Text(language.t("Zerar", "Reset")) }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text(language.t("Cancelar", "Cancel")) } },
        )
    }
}

@Composable
private fun SettingsBlock(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun TimeStepper(value: Int, range: IntRange, step: Int = 1, onChange: (Int) -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                val next = value - step
                onChange(if (next < range.first) range.last else next)
            }) { Text("−") }
            Text(value.toString().padStart(2, '0'), modifier = Modifier.width(28.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            TextButton(onClick = {
                val next = value + step
                onChange(if (next > range.last) range.first else next)
            }) { Text("+") }
        }
    }
}
