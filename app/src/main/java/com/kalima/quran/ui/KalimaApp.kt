package com.kalima.quran.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.annotation.StringRes
import com.kalima.quran.R
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.localization.AppLanguage
import com.kalima.quran.ui.theme.KalimaTheme

private enum class AppTab(@param:StringRes val labelRes: Int, val symbol: String) {
    Study(R.string.tab_study, "ا"),
    Library(R.string.tab_words, "ب"),
    Quiz(R.string.tab_quiz, "؟"),
    Progress(R.string.tab_progress, "ج"),
}

@Composable
fun KalimaApp(
    progress: StudyProgress,
    onAnswer: (String, Boolean) -> Unit,
    onQuizAnswer: (String, Boolean) -> Unit,
    onLockScreenChange: (Boolean) -> Unit,
    onLockScreenQuizChange: (Boolean) -> Unit,
    onLockScreenQuizIntervalChange: (Int) -> Unit,
    onReminderChange: (Boolean) -> Unit,
    onDailyGoalChange: (Int) -> Unit,
    onStudyScopeChange: (StudyScope) -> Unit,
    onToggleSurah: (Int) -> Unit,
    onOpenAppSettings: () -> Unit,
    onPreviewLockScreen: () -> Unit,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    var selectedName by rememberSaveable { mutableStateOf(AppTab.Study.name) }
    val selected = AppTab.valueOf(selectedName)

    KalimaTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    AppTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selected == tab,
                            onClick = { selectedName = tab.name },
                            icon = {
                                Text(
                                    tab.symbol,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (selected) {
                    AppTab.Study -> StudyScreen(
                        progress = progress,
                        onAnswer = onAnswer,
                        onEnableLockScreen = { onLockScreenChange(true) },
                    )
                    AppTab.Library -> LibraryScreen(progress = progress)
                    AppTab.Quiz -> QuizScreen(progress = progress, onAnswer = onQuizAnswer)
                    AppTab.Progress -> ProgressScreen(
                        progress = progress,
                        onLockScreenChange = onLockScreenChange,
                        onLockScreenQuizChange = onLockScreenQuizChange,
                        onLockScreenQuizIntervalChange = onLockScreenQuizIntervalChange,
                        onReminderChange = onReminderChange,
                        onDailyGoalChange = onDailyGoalChange,
                        onStudyScopeChange = onStudyScopeChange,
                        onToggleSurah = onToggleSurah,
                        onOpenAppSettings = onOpenAppSettings,
                        onPreviewLockScreen = onPreviewLockScreen,
                        currentLanguage = currentLanguage,
                        onLanguageChange = onLanguageChange,
                    )
                }
            }
        }
    }
}
