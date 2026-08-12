package com.kalima.quran.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.ReviewQueue
import com.kalima.quran.data.ReviewSchedule
import com.kalima.quran.data.SpacedRepetition
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.WordRepository
import com.kalima.quran.data.WordStatus
import com.kalima.quran.data.limitNewWords
import com.kalima.quran.localization.AppLanguage
import com.kalima.quran.quiz.QuizEngine
import com.kalima.quran.quiz.QuizMode
import com.kalima.quran.quiz.QuizQuestion
import com.kalima.quran.quiz.QuizQuestionType
import com.kalima.quran.quiz.VerseExcerptBuilder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun StudyScreen(store: DesktopProgressStore) {
    val progress = store.progress
    val language = store.language
    val scopedWords = rememberScopedWords(progress, language)
    val activeWords = remember(scopedWords, progress.maximumWords, progress.learnedIds, progress.reviewingIds) {
        progress.limitNewWords(scopedWords)
    }
    val queue = remember(activeWords, progress.reviewSchedules) {
        ReviewQueue.ordered(activeWords, progress.reviewSchedules, Instant.now())
    }
    val requested = progress.currentStudyWordId
        ?.let { id -> queue.firstOrNull { it.id == id } }
    val word = requested ?: queue.firstOrNull()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 36.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(language.t("Estudar", "Study"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    progress.studyScope.label(language),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp)) {
                Text(
                    language.t(
                        "${progress.dueReviewCount(activeWords.mapTo(mutableSetOf(), QuranWord::id))} revisões vencidas",
                        "${progress.dueReviewCount(activeWords.mapTo(mutableSetOf(), QuranWord::id))} reviews due",
                    ),
                    Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        if (word == null) {
            EmptyStudyState(language, progress.reviewSchedules.isNotEmpty())
        } else {
            StudyCard(
                word = word,
                progress = progress,
                language = language,
                onRemembered = {
                    store.answer(word.id, true)
                    store.setCurrentStudyWord(null)
                },
                onAgain = {
                    store.answer(word.id, false)
                    store.setCurrentStudyWord(null)
                },
                onToggleFavorite = { store.toggleFavorite(word.id) },
                onToggleCustom = { store.toggleCustomStudy(word.id) },
            )
        }
    }
}

@Composable
private fun StudyCard(
    word: QuranWord,
    progress: StudyProgress,
    language: AppLanguage,
    onRemembered: () -> Unit,
    onAgain: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleCustom: () -> Unit,
) {
    var revealed by remember(word.id) { mutableStateOf(false) }
    val status = progress.statusFor(word.id)
    Card(
        modifier = Modifier.fillMaxWidth().width(760.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        status.label(language),
                        Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(word.reference, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                word.arabic,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 56.sp,
                lineHeight = 72.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                word.transliteration,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = { DesktopPronouncer.speak(word.arabic) },
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("▶  ${language.t("Ouvir voz do dispositivo", "Hear device voice")}")
            }
            Spacer(Modifier.height(22.dp))
            if (!revealed) {
                Text(
                    language.t("Tente lembrar o significado antes de revelar.", "Try to recall the meaning before revealing it."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { revealed = true },
                    modifier = Modifier.width(300.dp).height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                ) {
                    Text(language.t("Revelar significado", "Reveal meaning"), fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(word.meaning, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            language.t("Raiz: ${word.root}  •  ${word.grammar}", "Root: ${word.root}  •  ${word.grammar}"),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(word.verseArabic, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, fontSize = 25.sp, lineHeight = 39.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(word.verseMeaning, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onAgain,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(language.t("De novo", "Again"), fontWeight = FontWeight.Bold)
                            Text("10 min", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Button(
                        onClick = onRemembered,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(language.t("Acertei", "Got it"), fontWeight = FontWeight.Bold)
                            Text(nextGoodInterval(progress.scheduleFor(word.id), language), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onToggleFavorite) {
                    Text((if (word.id in progress.favoriteIds) "★ " else "☆ ") + language.t("Favorita", "Favorite"))
                }
                TextButton(onClick = onToggleCustom) {
                    Text((if (word.id in progress.customStudyIds) "✓ " else "+ ") + language.t("Minha lista", "My list"))
                }
            }
            if (revealed) {
                Text(
                    word.insight,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun EmptyStudyState(language: AppLanguage, hasProgress: Boolean) {
    Box(Modifier.fillMaxWidth().heightIn(min = 420.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✓", color = MaterialTheme.colorScheme.primary, fontSize = 56.sp)
            Text(
                if (hasProgress) language.t("Tudo em dia por enquanto", "All caught up for now")
                else language.t("Não há palavras neste caminho", "There are no words in this path"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                language.t("Volte quando a próxima revisão vencer ou escolha outro caminho.", "Come back when the next review is due or choose another path."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun QuizScreen(store: DesktopProgressStore) {
    val progress = store.progress
    val language = store.language
    val scopedWords = rememberScopedWords(progress, language)
    val activeWords = remember(scopedWords, progress.maximumWords, progress.learnedIds, progress.reviewingIds) {
        progress.limitNewWords(scopedWords)
    }
    var mode by remember { mutableStateOf(QuizMode.Mixed) }
    var sessionVersion by remember { mutableIntStateOf(0) }
    val sourceKey = "${progress.studyScope}:${progress.selectedSurahs.sorted()}:${progress.favoriteIds.sorted()}:${progress.customStudyIds.sorted()}:$language"
    val session = remember(sourceKey, mode, sessionVersion) {
        createDesktopQuiz(activeWords, scopedWords, progress, mode)
    }
    var currentIndex by remember(sourceKey, mode, sessionVersion) { mutableIntStateOf(0) }
    var selectedOption by remember(sourceKey, mode, sessionVersion, currentIndex) { mutableStateOf<Int?>(null) }
    var score by remember(sourceKey, mode, sessionVersion) { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 36.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text("Quiz", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                language.t("Cinco perguntas, sem cronômetro nem punições.", "Five questions, with no timer or penalties."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuizMode.entries.forEach { option ->
                    FilterChip(selected = mode == option, onClick = { mode = option }, label = { Text(option.label(language)) })
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        if (session.isEmpty()) {
            Box(Modifier.fillMaxWidth().heightIn(min = 380.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("○", fontSize = 54.sp, color = MaterialTheme.colorScheme.primary)
                    Text(
                        language.t("Nenhuma pergunta disponível neste modo", "No questions available in this mode"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        language.t("Escolha outro modo ou amplie o caminho de estudo.", "Choose another mode or expand your study path."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (currentIndex >= session.size) {
            QuizSummary(score, session.size, progress, language) { sessionVersion += 1 }
        } else {
            val question = session[currentIndex]
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { currentIndex.toFloat() / session.size },
                    modifier = Modifier.weight(1f).height(7.dp).clip(CircleShape),
                )
                Spacer(Modifier.width(12.dp))
                Text("${currentIndex + 1}/${session.size}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(18.dp))
            QuizQuestionCard(question, language)
            Spacer(Modifier.height(14.dp))
            question.options.forEachIndexed { index, option ->
                QuizOption(
                    text = option,
                    arabic = question.type in setOf(QuizQuestionType.PortugueseToArabic, QuizQuestionType.ClozeToArabic, QuizQuestionType.RootToArabic),
                    selected = selectedOption == index,
                    correct = index == question.correctOptionIndex,
                    answered = selectedOption != null,
                ) {
                    if (selectedOption == null) {
                        selectedOption = index
                        val correct = index == question.correctOptionIndex
                        if (correct) score += 1
                        store.answerQuiz(question.word.id, correct)
                    }
                }
                Spacer(Modifier.height(9.dp))
            }
            if (selectedOption != null) {
                QuizFeedback(
                    question,
                    selectedOption == question.correctOptionIndex,
                    store.progress.scheduleFor(question.word.id),
                    language,
                ) { currentIndex += 1 }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

private fun createDesktopQuiz(
    activeWords: List<QuranWord>,
    optionWords: List<QuranWord>,
    progress: StudyProgress,
    mode: QuizMode,
): List<QuizQuestion> {
    val ordered = ReviewQueue.ordered(activeWords, progress.reviewSchedules, Instant.now())
    val targets = when (mode) {
        QuizMode.ReviewsOnly -> ReviewQueue.dueWords(activeWords, progress.reviewSchedules, Instant.now())
        QuizMode.Roots -> ordered.filter { it.root.isNotBlank() && it.root != "—" }
        QuizMode.Difficult -> ordered.filter { (progress.reviewSchedules[it.id]?.lapses ?: 0) > 0 }
        else -> ordered
    }
    if (targets.isEmpty() || optionWords.isEmpty()) return emptyList()
    return runCatching {
        QuizEngine.createSession(targets, progress::statusFor, optionWords = optionWords, mode = mode)
    }.getOrDefault(emptyList())
}

@Composable
private fun QuizQuestionCard(question: QuizQuestion, language: AppLanguage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val prompt = when (question.type) {
                QuizQuestionType.ArabicToPortuguese -> language.t("O que significa?", "What does it mean?")
                QuizQuestionType.PortugueseToArabic -> language.t("Qual é a palavra em árabe?", "Which is the Arabic word?")
                QuizQuestionType.ContextualMeaning -> language.t("Qual é o sentido no contexto?", "What is the meaning in context?")
                QuizQuestionType.ListeningToPortuguese -> language.t("Ouça e escolha o significado", "Listen and choose the meaning")
                QuizQuestionType.ClozeToArabic -> language.t("Qual palavra completa o trecho?", "Which word completes the excerpt?")
                QuizQuestionType.RootToArabic -> language.t("Qual palavra pertence a esta raiz?", "Which word belongs to this root?")
            }
            Text(prompt, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            when (question.type) {
                QuizQuestionType.ArabicToPortuguese -> ArabicQuestionText(question.word.arabic, question.word.transliteration)
                QuizQuestionType.PortugueseToArabic -> Text(question.word.meaning, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                QuizQuestionType.ContextualMeaning -> ContextualQuestion(question)
                QuizQuestionType.ListeningToPortuguese -> Button(onClick = { DesktopPronouncer.speak(question.word.arabic) }) {
                    Text("▶  ${language.t("Ouvir palavra", "Play word")}")
                }
                QuizQuestionType.ClozeToArabic -> {
                    Text(VerseExcerptBuilder.buildCloze(question.word), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, fontSize = 29.sp, lineHeight = 43.sp)
                    Text(question.word.reference, color = MaterialTheme.colorScheme.primary)
                }
                QuizQuestionType.RootToArabic -> {
                    Text(question.word.root, fontSize = 44.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(question.word.grammar, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ArabicQuestionText(arabic: String, transliteration: String) {
    Text(arabic, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 50.sp, color = MaterialTheme.colorScheme.primary)
    Text(transliteration, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ContextualQuestion(question: QuizQuestion) {
    val excerpt = remember(question.word.id) { VerseExcerptBuilder.build(question.word) }
    val annotated = buildAnnotatedString {
        append(excerpt.text)
        if (excerpt.hasHighlight) {
            addStyle(
                SpanStyle(background = MaterialTheme.colorScheme.secondary.copy(alpha = 0.42f), fontWeight = FontWeight.Bold),
                excerpt.highlightStart,
                excerpt.highlightEnd,
            )
        }
    }
    Text(annotated, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, fontSize = 28.sp, lineHeight = 43.sp)
    Text(question.word.reference, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun QuizOption(
    text: String,
    arabic: Boolean,
    selected: Boolean,
    correct: Boolean,
    answered: Boolean,
    onClick: () -> Unit,
) {
    val container = when {
        answered && correct -> MaterialTheme.colorScheme.primaryContainer
        answered && selected -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val border = when {
        answered && correct -> MaterialTheme.colorScheme.primary
        answered && selected -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = !answered,
        color = container,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.5.dp, border),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            textAlign = TextAlign.Center,
            fontSize = if (arabic) 28.sp else 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (arabic) MaterialTheme.colorScheme.primary else Color.Unspecified,
        )
    }
}

@Composable
private fun QuizFeedback(
    question: QuizQuestion,
    correct: Boolean,
    schedule: ReviewSchedule?,
    language: AppLanguage,
    onNext: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        color = if (correct) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (correct) language.t("Correto", "Correct")
                    else language.t("Resposta: ${question.correctAnswer}", "Answer: ${question.correctAnswer}"),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("${question.word.transliteration}  •  ${question.word.root}  •  ${question.word.reference}")
                Text(nextReview(schedule, language), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(onClick = { DesktopPronouncer.speak(question.word.arabic) }) { Text("▶") }
            Spacer(Modifier.width(10.dp))
            Button(onClick = onNext) { Text(language.t("Próxima", "Next")) }
        }
    }
}

@Composable
private fun QuizSummary(
    score: Int,
    total: Int,
    progress: StudyProgress,
    language: AppLanguage,
    onAgain: () -> Unit,
) {
    val accuracy = if (progress.quizTotalAnswers == 0) 0 else progress.quizCorrectAnswers * 100 / progress.quizTotalAnswers
    Box(Modifier.fillMaxWidth().heightIn(min = 420.dp), contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(26.dp), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(Modifier.padding(38.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(language.t("Quiz concluído", "Quiz complete"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("$score/$total", fontSize = 58.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(language.t("Precisão geral: $accuracy%", "Overall accuracy: $accuracy%"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                Button(onClick = onAgain) { Text(language.t("Novo quiz", "New quiz")) }
            }
        }
    }
}

@Composable
private fun rememberScopedWords(progress: StudyProgress, language: AppLanguage): List<QuranWord> = remember(
    progress.studyScope,
    progress.selectedSurahs,
    progress.favoriteIds,
    progress.customStudyIds,
    language,
) {
    WordRepository.wordsFor(
        progress.studyScope,
        progress.selectedSurahs,
        progress.favoriteIds,
        progress.customStudyIds,
    )
}

private fun nextGoodInterval(schedule: ReviewSchedule?, language: AppLanguage): String =
    when (val days = SpacedRepetition.nextGoodIntervalDays(schedule)) {
        1 -> language.t("amanhã", "tomorrow")
        else -> language.t("$days dias", "$days days")
    }

private fun nextReview(schedule: ReviewSchedule?, language: AppLanguage): String {
    if (schedule == null) return language.t("Revisão agora", "Review now")
    if (schedule.intervalDays == 0) return language.t("Revisão em 10 minutos", "Review in 10 minutes")
    val formatter = DateTimeFormatter.ofPattern(if (language == AppLanguage.Portuguese) "dd/MM/yyyy" else "MMM d, yyyy")
    val date = schedule.dueAt.atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
    return language.t("Próxima revisão: $date", "Next review: $date")
}
