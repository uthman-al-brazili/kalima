package com.kalima.quran.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.data.WordRepository

@Composable
internal fun SurahSelectionDialog(
    selectedSurahs: Set<Int>,
    onToggleSurah: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredSurahs = remember(query) {
        val term = query.trim().lowercase()
        if (term.isEmpty()) {
            WordRepository.selectableSurahs
        } else {
            WordRepository.selectableSurahs.filter { surah ->
                surah.number.toString().contains(term) ||
                    surah.transliteratedName.lowercase().contains(term) ||
                    surah.arabicName.contains(term)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(stringResource(R.string.choose_surahs))
                Text(
                    pluralStringResource(
                        R.plurals.surahs_selected,
                        selectedSurahs.size,
                        selectedSurahs.size,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal,
                )
            }
        },
        text = {
            Column(Modifier.heightIn(max = 520.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.surah_search_hint)) },
                    singleLine = true,
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(filteredSurahs, key = { it.number }) { surah ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSurah(surah.number) }
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = surah.number in selectedSurahs,
                                onCheckedChange = { onToggleSurah(surah.number) },
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${surah.number} · ${surah.transliteratedName}",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(surah.arabicName, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done))
            }
        },
    )
}
