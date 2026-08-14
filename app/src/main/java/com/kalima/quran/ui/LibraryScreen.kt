package com.kalima.quran.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.audio.ArabicPronouncer
import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.WordRepository
import com.kalima.quran.data.WordStatus

private enum class LibraryFilter(@param:StringRes val labelRes: Int) {
    All(R.string.filter_all),
    New(R.string.filter_new),
    Reviewing(R.string.filter_reviewing),
    Learned(R.string.filter_learned),
    AlreadyKnown(R.string.filter_already_known),
    MyList(R.string.my_list_filter),
}

@Composable
fun LibraryScreen(
    progress: StudyProgress,
    pronouncer: ArabicPronouncer,
    onToggleCustomList: (String) -> Unit,
    onToggleAlreadyKnown: (String) -> Unit,
    onShowCompleteAyahChange: (Boolean) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable { mutableStateOf(LibraryFilter.All.name) }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    val filter = LibraryFilter.valueOf(filterName)
    val selectionKey = progress.selectedSurahs.sorted().joinToString(",")
    val activeWords = remember(
        progress.studyScope,
        selectionKey,
        progress.customStudyIds,
    ) {
        WordRepository.wordsFor(
            progress.studyScope,
            progress.selectedSurahs,
            progress.customStudyIds,
        )
    }
    val words = remember(
        query,
        filter,
        activeWords,
        progress.learnedIds,
        progress.reviewingIds,
        progress.alreadyKnownIds,
    ) {
        WordRepository.search(query, activeWords).filter { word ->
            when (filter) {
                LibraryFilter.All -> true
                LibraryFilter.New -> progress.statusFor(word.id) == WordStatus.New
                LibraryFilter.Reviewing -> progress.statusFor(word.id) == WordStatus.Reviewing
                LibraryFilter.Learned -> progress.statusFor(word.id) == WordStatus.Learned
                LibraryFilter.AlreadyKnown -> progress.statusFor(word.id) == WordStatus.AlreadyKnown
                LibraryFilter.MyList -> word.id in progress.customStudyIds
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(stringResource(R.string.library_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                when (progress.studyScope) {
                    StudyScope.All -> pluralStringResource(
                        R.plurals.library_available_cards,
                        activeWords.size,
                        activeWords.size,
                    )
                    StudyScope.Frequent -> pluralStringResource(
                        R.plurals.library_frequent_forms,
                        activeWords.size,
                        activeWords.size,
                    )
                    StudyScope.Frequent300,
                    StudyScope.Frequent50,
                    StudyScope.Frequent500,
                    StudyScope.Prayer,
                    StudyScope.ShortSurahs,
                    StudyScope.Custom,
                    -> pluralStringResource(
                        R.plurals.library_available_cards,
                        activeWords.size,
                        activeWords.size,
                    )
                    StudyScope.Surahs -> if (progress.selectedSurahs.size <= 4) {
                        stringResource(
                            R.string.library_surah_list,
                            activeWords.size,
                            progress.selectedSurahs.sorted().joinToString(", "),
                        )
                    } else {
                        stringResource(
                            R.string.library_surah_count,
                            activeWords.size,
                            progress.selectedSurahs.size,
                        )
                    }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.library_search_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LibraryFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filterName = option.name },
                        label = { Text(stringResource(option.labelRes)) },
                    )
                }
            }
        }
        if (words.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.no_words_found),
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(words, key = { it.id }) { word ->
            LibraryWordCard(
                word = word,
                status = progress.statusFor(word.id),
                expanded = expandedId == word.id,
                onClick = { expandedId = if (expandedId == word.id) null else word.id },
                pronouncer = pronouncer,
                inCustomList = word.id in progress.customStudyIds,
                alreadyKnown = word.id in progress.alreadyKnownIds,
                onToggleCustomList = onToggleCustomList,
                onToggleAlreadyKnown = onToggleAlreadyKnown,
                showCompleteAyah = progress.showCompleteAyah,
                onShowCompleteAyahChange = onShowCompleteAyahChange,
            )
        }
    }
}

@Composable
private fun LibraryWordCard(
    word: QuranWord,
    status: WordStatus,
    expanded: Boolean,
    onClick: () -> Unit,
    pronouncer: ArabicPronouncer,
    inCustomList: Boolean,
    alreadyKnown: Boolean,
    onToggleCustomList: (String) -> Unit,
    onToggleAlreadyKnown: (String) -> Unit,
    showCompleteAyah: Boolean,
    onShowCompleteAyahChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        word.transliteration,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(word.meaning, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                PronunciationButton(
                    word = word,
                    pronouncer = pronouncer,
                    compact = true,
                )
                ArabicText(word.arabic, size = 30, color = MaterialTheme.colorScheme.primary)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.reference_root, word.reference, word.root),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                WordStatusPill(status)
            }
            if (expanded) {
                Spacer(Modifier.height(14.dp))
                if (showCompleteAyah) {
                    VerseExplorerPanel(word)
                    TextPronunciationButton(
                        arabic = word.verseArabic,
                        pronouncer = pronouncer,
                        modifier = Modifier.fillMaxWidth(),
                        labelRes = R.string.device_voice_verse,
                    )
                    OutlinedButton(
                        onClick = { onShowCompleteAyahChange(false) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.hide_complete_ayah))
                    }
                } else {
                    OutlinedButton(
                        onClick = { onShowCompleteAyahChange(true) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.show_complete_ayah))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    word.insight,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                WordCollectionActions(
                    word = word,
                    inCustomList = inCustomList,
                    alreadyKnown = alreadyKnown,
                    onToggleCustomList = onToggleCustomList,
                    onToggleAlreadyKnown = onToggleAlreadyKnown,
                )
                CitationActions(word)
                EditorialReviewPanel(word)
            }
        }
    }
}
