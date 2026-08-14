package com.kalima.quran.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.kalima.quran.data.QuranReaderRepository
import com.kalima.quran.data.QuranSurah
import com.kalima.quran.data.QuranVerse
import com.kalima.quran.data.WordRepository

@Composable
fun QuranReaderScreen() {
    var selectedSurahNumber by rememberSaveable { mutableIntStateOf(1) }
    var pickerVisible by rememberSaveable { mutableStateOf(false) }
    val surahs = WordRepository.selectableSurahs
    val selectedSurah = surahs.first { it.number == selectedSurahNumber }
    val verses = remember(selectedSurahNumber) {
        QuranReaderRepository.versesFor(selectedSurahNumber)
    }
    val listState = rememberLazyListState()

    LaunchedEffect(selectedSurahNumber) {
        listState.scrollToItem(0)
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(
                stringResource(R.string.quran_reader_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.quran_reader_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = { pickerVisible = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    stringResource(
                        R.string.quran_selected_surah,
                        selectedSurah.number,
                        selectedSurah.transliteratedName,
                        selectedSurah.arabicName,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { selectedSurahNumber-- },
                    enabled = selectedSurahNumber > 1,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("‹ ${stringResource(R.string.previous_surah)}")
                }
                OutlinedButton(
                    onClick = { selectedSurahNumber++ },
                    enabled = selectedSurahNumber < 114,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("${stringResource(R.string.next_surah)} ›")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        ) {
            item(key = "surah-header") {
                SurahReaderHeader(selectedSurah, verses.size)
            }
            if (selectedSurahNumber != 1 && selectedSurahNumber != 9) {
                item(key = "basmala") {
                    ArabicText(
                        BASMALA,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        size = 27,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            items(verses, key = QuranVerse::ayahNumber) { verse ->
                QuranVerseRow(verse)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (pickerVisible) {
        SurahPickerSheet(
            surahs = surahs,
            selectedSurahNumber = selectedSurahNumber,
            onSelect = { number ->
                selectedSurahNumber = number
                pickerVisible = false
            },
            onDismiss = { pickerVisible = false },
        )
    }
}

@Composable
private fun SurahReaderHeader(surah: QuranSurah, verseCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ArabicText(
                surah.arabicName,
                modifier = Modifier.fillMaxWidth(),
                size = 34,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "${surah.number}. ${surah.transliteratedName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                pluralStringResource(R.plurals.quran_ayah_count, verseCount, verseCount),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun QuranVerseRow(verse: QuranVerse) {
    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        ArabicText(
            verse.arabic,
            modifier = Modifier.fillMaxWidth(),
            size = 28,
            color = MaterialTheme.colorScheme.onSurface,
            align = TextAlign.End,
        )
        Text(
            "${verse.surahNumber}:${verse.ayahNumber}",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurahPickerSheet(
    surahs: List<QuranSurah>,
    selectedSurahNumber: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredSurahs = remember(query, surahs) {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) {
            surahs
        } else {
            surahs.filter { surah ->
                surah.number.toString() == normalized ||
                    surah.transliteratedName.lowercase().contains(normalized) ||
                    surah.arabicName.contains(query.trim())
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp),
        ) {
            Text(
                stringResource(R.string.choose_surah_to_read),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.quran_surah_search_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(filteredSurahs, key = QuranSurah::number) { surah ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(surah.number) },
                        color = if (surah.number == selectedSurahNumber) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                surah.number.toString(),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                                Text(surah.transliteratedName, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                surah.arabicName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private const val BASMALA = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
