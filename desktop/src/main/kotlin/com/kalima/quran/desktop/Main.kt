package com.kalima.quran.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.kalima.quran.data.StudyScope
import com.kalima.quran.localization.AppLanguage
import com.kalima.quran.ui.theme.Gold
import com.kalima.quran.ui.theme.KalimaTheme
import java.awt.Dimension

private enum class DesktopTab(val glyph: String) {
    Study("ك"),
    Quiz("?"),
    Library("⌕"),
    Progress("↗"),
    Settings("⚙"),
}

fun main(args: Array<String>) {
    val store = DesktopProgressStore()
    if ("--smoke-test" in args) {
        check(com.kalima.quran.data.WordRepository.words.size == 42_117)
        check(com.kalima.quran.data.WordRepository.selectableSurahs.size == 114)
        println("Kalima Windows OK: ${com.kalima.quran.data.WordRepository.words.size} cards")
        return
    }
    DesktopReminderManager.start(store)
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Kalima — Árabe corânico",
            state = rememberWindowState(width = 1180.dp, height = 780.dp),
            icon = painterResource("ic_launcher-playstore.png"),
        ) {
            LaunchedEffect(Unit) {
                window.minimumSize = Dimension(920, 640)
            }
            KalimaTheme(store.progress.themeMode) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (store.progress.onboardingComplete) {
                        DesktopApp(store)
                    } else {
                        DesktopOnboarding(store)
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopApp(store: DesktopProgressStore) {
    var selectedTab by remember { mutableStateOf(DesktopTab.Study) }
    var libraryWordId by remember { mutableStateOf<String?>(null) }
    Row(Modifier.fillMaxSize()) {
        DesktopSidebar(
            store = store,
            selected = selectedTab,
            onSelected = { selectedTab = it },
        )
        Box(Modifier.weight(1f).fillMaxHeight()) {
            when (selectedTab) {
                DesktopTab.Study -> StudyScreen(store)
                DesktopTab.Quiz -> QuizScreen(store)
                DesktopTab.Library -> LibraryScreen(store, libraryWordId) { libraryWordId = it }
                DesktopTab.Progress -> ProgressScreen(store)
                DesktopTab.Settings -> SettingsScreen(store)
            }
        }
    }
}

@Composable
private fun DesktopSidebar(
    store: DesktopProgressStore,
    selected: DesktopTab,
    onSelected: (DesktopTab) -> Unit,
) {
    val language = store.language
    val progress = store.progress
    Surface(
        modifier = Modifier.width(212.dp).fillMaxHeight(),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                        .background(Gold),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("ك", color = MaterialTheme.colorScheme.primary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Kalima", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text(
                        language.t("Árabe corânico", "Quranic Arabic"),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.68f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Spacer(Modifier.height(34.dp))
            DesktopTab.entries.forEach { tab ->
                val label = when (tab) {
                    DesktopTab.Study -> language.t("Estudar", "Study")
                    DesktopTab.Quiz -> "Quiz"
                    DesktopTab.Library -> language.t("Biblioteca", "Library")
                    DesktopTab.Progress -> language.t("Progresso", "Progress")
                    DesktopTab.Settings -> language.t("Configurações", "Settings")
                }
                val active = selected == tab
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(if (active) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.13f) else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onSelected(tab) }.padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                        Text(tab.glyph, color = if (active) Gold else MaterialTheme.colorScheme.onPrimary, fontSize = 19.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(label, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                }
                Spacer(Modifier.height(5.dp))
            }
            Spacer(Modifier.weight(1f))
            Surface(
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f),
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(language.t("Meta de hoje", "Today's goal"), style = MaterialTheme.typography.labelMedium)
                        Text("${progress.todayCompleted}/${progress.dailyGoal}", fontWeight = FontWeight.Bold, color = Gold)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (progress.todayCompleted.toFloat() / progress.dailyGoal).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = Gold,
                        trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        language.t("Sequência: ${progress.streakDays} dias", "Streak: ${progress.streakDays} days"),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopOnboarding(store: DesktopProgressStore) {
    val language = store.language
    var scope by remember { mutableStateOf(StudyScope.Frequent) }
    var goal by remember { mutableIntStateOf(5) }
    Scaffold { padding ->
        Row(
            modifier = Modifier.fillMaxSize().padding(padding).padding(48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.width(430.dp)) {
                Text("كلمة", color = MaterialTheme.colorScheme.primary, fontSize = 60.sp, fontWeight = FontWeight.Bold)
                Text("Kalima", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text(
                    language.t(
                        "Aprenda a reconhecer o vocabulário do Alcorão em pequenos momentos.",
                        "Learn to recognize Quranic vocabulary in small moments.",
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(22.dp))
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
            Spacer(Modifier.width(58.dp))
            Card(
                modifier = Modifier.width(430.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(3.dp),
            ) {
                Column(Modifier.padding(30.dp)) {
                    Text(language.t("Seu primeiro caminho", "Your first path"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        language.t("Você poderá mudar isto a qualquer momento.", "You can change this at any time."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(18.dp))
                    listOf(StudyScope.Frequent50, StudyScope.Frequent, StudyScope.Prayer, StudyScope.ShortSurahs).forEach { option ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            onClick = { scope = option },
                            shape = RoundedCornerShape(14.dp),
                            color = if (scope == option) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(18.dp).clip(CircleShape)
                                        .background(if (scope == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(option.label(language), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(language.t("Meta diária", "Daily goal"), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 5, 10, 15).forEach { option ->
                            FilterChip(selected = goal == option, onClick = { goal = option }, label = { Text("$option") })
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { store.completeOnboarding(scope, goal) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(language.t("Começar a estudar", "Start learning"), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        language.t("Funciona offline. Sem conta e sem coleta de dados.", "Works offline. No account or data collection."),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
