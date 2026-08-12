package com.kalima.quran.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.kalima.quran.data.AppThemeMode
import com.kalima.quran.data.QuranWord
import com.kalima.quran.localization.AppLanguage
import com.kalima.quran.quiz.LockScreenContent
import com.kalima.quran.quiz.QuizQuestion
import com.kalima.quran.quiz.QuizQuestionType
import com.kalima.quran.quiz.VerseExcerptBuilder
import com.kalima.quran.ui.theme.Forest
import com.kalima.quran.ui.theme.Gold
import com.kalima.quran.ui.theme.KalimaTheme
import kotlinx.coroutines.delay

@Composable
fun WelcomeBackWindow(
    content: LockScreenContent,
    language: AppLanguage,
    themeMode: AppThemeMode,
    onWordAnswer: (wordId: String, remembered: Boolean) -> Unit,
    onQuizAnswer: (wordId: String, correct: Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    onOpenApp: (wordId: String) -> Unit,
) {
    val word = when (content) {
        is LockScreenContent.WordCard -> content.word
        is LockScreenContent.QuizCard -> content.question.word
    }
    val state = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        width = 620.dp,
        height = 720.dp,
    )
    LaunchedEffect(content) {
        delay(AUTO_DISMISS_MILLIS)
        onDismiss()
    }
    Window(
        onCloseRequest = onDismiss,
        state = state,
        title = language.t("Kalima — Bem-vindo de volta", "Kalima — Welcome back"),
        alwaysOnTop = true,
        undecorated = true,
        transparent = true,
        resizable = false,
    ) {
        LaunchedEffect(Unit) {
            window.toFront()
            window.requestFocus()
        }
        KalimaTheme(themeMode) {
            Surface(
                modifier = Modifier.fillMaxSize().padding(14.dp).shadow(18.dp, RoundedCornerShape(30.dp)),
                shape = RoundedCornerShape(30.dp),
                color = Forest,
                contentColor = Color.White,
            ) {
                Column(Modifier.fillMaxSize().padding(24.dp)) {
                    ReturnCardHeader(language, onSnooze, onDismiss)
                    Spacer(Modifier.height(14.dp))
                    when (content) {
                        is LockScreenContent.WordCard -> ReturnWordCard(
                            word = content.word,
                            language = language,
                            onAnswer = { remembered -> onWordAnswer(content.word.id, remembered) },
                        )
                        is LockScreenContent.QuizCard -> ReturnQuizCard(
                            question = content.question,
                            language = language,
                            onAnswer = { correct -> onQuizAnswer(content.question.word.id, correct) },
                            onContinue = onDismiss,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    TextButton(
                        onClick = { onOpenApp(word.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = Gold),
                    ) {
                        Text(language.t("Abrir o Kalima", "Open Kalima"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReturnCardHeader(
    language: AppLanguage,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("كَلِمَة", color = Gold, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                language.t("BEM-VINDO DE VOLTA", "WELCOME BACK"),
                color = Color.White.copy(alpha = 0.66f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Row {
            TextButton(onClick = onSnooze, colors = ButtonDefaults.textButtonColors(contentColor = Gold)) {
                Text(language.t("Pausar 1 h", "Snooze 1h"))
            }
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                Text(language.t("Agora não  ✕", "Not now  ✕"))
            }
        }
    }
}

@Composable
private fun ColumnScope.ReturnWordCard(
    word: QuranWord,
    language: AppLanguage,
    onAnswer: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(color = Gold.copy(alpha = 0.18f), shape = RoundedCornerShape(100.dp)) {
            Text(
                word.category,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                color = Gold,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            word.arabic,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Gold,
            fontSize = 56.sp,
            lineHeight = 68.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(word.transliteration, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { DesktopPronouncer.speak(word.arabic, language) },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold),
            border = BorderStroke(1.dp, Gold.copy(alpha = 0.65f)),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("▶  ${language.t("Ouvir", "Listen")}")
        }
        Spacer(Modifier.height(12.dp))
        Text(
            word.meaning,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "${language.t("Raiz", "Root")}: ${word.root}  •  ${word.grammar}",
            color = Color.White.copy(alpha = 0.68f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(word.reference, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Text(
                    word.verseArabic,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    fontSize = 24.sp,
                    lineHeight = 36.sp,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { onAnswer(false) },
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(15.dp),
            ) {
                Text(language.t("Rever depois", "Again"), fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { onAnswer(true) },
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Forest),
                shape = RoundedCornerShape(15.dp),
            ) {
                Text(language.t("Já aprendi", "Got it"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ColumnScope.ReturnQuizCard(
    question: QuizQuestion,
    language: AppLanguage,
    onAnswer: (Boolean) -> Unit,
    onContinue: () -> Unit,
) {
    var selectedOption by remember(question.word.id) { mutableStateOf<Int?>(null) }
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            when (question.type) {
                QuizQuestionType.ArabicToPortuguese -> language.t("Qual é o significado?", "What does this word mean?")
                QuizQuestionType.PortugueseToArabic -> language.t("Qual é a palavra em árabe?", "Which is the Arabic word?")
                QuizQuestionType.ContextualMeaning -> language.t("Qual é o sentido no contexto?", "What does it mean in context?")
                else -> language.t("Escolha a resposta correta", "Choose the correct answer")
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ReturnQuizPrompt(question)
            }
        }
        Spacer(Modifier.height(12.dp))
        question.options.forEachIndexed { index, option ->
            val answered = selectedOption != null
            val correct = index == question.correctOptionIndex
            val selected = selectedOption == index
            val background = when {
                answered && correct -> MaterialTheme.colorScheme.primaryContainer
                answered && selected -> MaterialTheme.colorScheme.errorContainer
                else -> Color.White.copy(alpha = 0.08f)
            }
            val border = when {
                answered && correct -> Gold
                answered && selected -> MaterialTheme.colorScheme.error
                else -> Color.White.copy(alpha = 0.38f)
            }
            Surface(
                onClick = {
                    if (!answered) {
                        selectedOption = index
                        onAnswer(correct)
                    }
                },
                enabled = !answered,
                modifier = Modifier.fillMaxWidth(),
                color = background,
                contentColor = Color.White,
                border = BorderStroke(1.5.dp, border),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    option,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    textAlign = TextAlign.Center,
                    fontSize = if (question.type == QuizQuestionType.PortugueseToArabic) 26.sp else 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        if (selectedOption != null) {
            Text(
                if (selectedOption == question.correctOptionIndex) language.t("Correto", "Correct")
                else language.t("Resposta: ${question.correctAnswer}", "Answer: ${question.correctAnswer}"),
                color = Gold,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Forest),
            ) {
                Text(language.t("Continuar", "Continue"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReturnQuizPrompt(question: QuizQuestion) {
    when (question.type) {
        QuizQuestionType.ArabicToPortuguese -> {
            Text(question.word.arabic, fontSize = 48.sp, color = MaterialTheme.colorScheme.primary)
            Text(question.word.transliteration, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        QuizQuestionType.PortugueseToArabic -> Text(
            question.word.meaning,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        QuizQuestionType.ContextualMeaning -> {
            val excerpt = remember(question.word.id) { VerseExcerptBuilder.build(question.word, maxChars = 150) }
            val text = buildAnnotatedString {
                append(excerpt.text)
                if (excerpt.hasHighlight) {
                    addStyle(
                        SpanStyle(background = Gold.copy(alpha = 0.5f), fontWeight = FontWeight.Bold),
                        excerpt.highlightStart,
                        excerpt.highlightEnd,
                    )
                }
            }
            Text(text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, fontSize = 24.sp, lineHeight = 36.sp)
            Text(question.word.reference, color = MaterialTheme.colorScheme.primary)
        }
        else -> Text(question.word.arabic, fontSize = 48.sp, color = MaterialTheme.colorScheme.primary)
    }
}

private const val AUTO_DISMISS_MILLIS = 45_000L
