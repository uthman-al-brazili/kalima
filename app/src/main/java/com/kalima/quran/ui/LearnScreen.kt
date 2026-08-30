package com.kalima.quran.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.audio.ArabicPronouncer
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.UnderstandPathId

internal enum class LearnSection(
    @param:StringRes val labelRes: Int,
) {
    Dictionary(R.string.tab_dictionary),
    Quiz(R.string.tab_quiz),
}

@Composable
internal fun LearnScreen(
    progress: StudyProgress,
    selectedSection: LearnSection,
    onSectionSelected: (LearnSection) -> Unit,
    pronouncer: ArabicPronouncer,
    onToggleCustomList: (String) -> Unit,
    onToggleAlreadyKnown: (String) -> Unit,
    onShowCompleteAyahChange: (Boolean) -> Unit,
    openExcludedWordsRequestId: Long,
    onQuizAnswer: (String, Boolean) -> Unit,
    quizUnderstandPath: UnderstandPathId?,
    onChooseQuizMode: () -> Unit,
) {
    val sectionStateHolder = rememberSaveableStateHolder()

    Column(Modifier.fillMaxSize()) {
        LearnSectionBar(
            selectedSection = selectedSection,
            onSectionSelected = onSectionSelected,
        )
        Box(Modifier.fillMaxWidth().weight(1f)) {
            sectionStateHolder.SaveableStateProvider(selectedSection.name) {
                when (selectedSection) {
                    LearnSection.Dictionary -> LibraryScreen(
                        progress = progress,
                        pronouncer = pronouncer,
                        onToggleCustomList = onToggleCustomList,
                        onToggleAlreadyKnown = onToggleAlreadyKnown,
                        onShowCompleteAyahChange = onShowCompleteAyahChange,
                        openExcludedWordsRequestId = openExcludedWordsRequestId,
                    )
                    LearnSection.Quiz -> QuizScreen(
                        progress = progress,
                        onAnswer = onQuizAnswer,
                        pronouncer = pronouncer,
                        understandPath = quizUnderstandPath,
                        onChooseQuizMode = onChooseQuizMode,
                    )
                }
            }
        }
    }
}

@Composable
private fun LearnSectionBar(
    selectedSection: LearnSection,
    onSectionSelected: (LearnSection) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LearnSection.entries.forEach { section ->
                FilterChip(
                    selected = selectedSection == section,
                    onClick = { onSectionSelected(section) },
                    label = { Text(stringResource(section.labelRes)) },
                )
            }
        }
    }
}
