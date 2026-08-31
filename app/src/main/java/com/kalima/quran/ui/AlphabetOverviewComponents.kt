package com.kalima.quran.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.data.ArabicFoundations
import com.kalima.quran.data.StudyProgress

@Composable
internal fun AlphabetReferenceTable() {
    var requestedPage by rememberSaveable { mutableStateOf(0) }
    val pages = remember {
        ArabicFoundations.alphabetReference.chunked(ArabicFoundations.alphabetReferencePageSize)
    }
    val pageIndex = requestedPage.coerceIn(0, pages.lastIndex)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.alphabet_reference_title),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.alphabet_reference_page, pageIndex + 1, pages.size),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
    }
    Text(
        stringResource(R.string.alphabet_reference_compact_hint),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(Modifier.height(6.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(6.dp)) {
            pages[pageIndex].forEachIndexed { index, reference ->
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(0.9f)) {
                            ArabicText(
                                reference.letter.arabic,
                                modifier = Modifier.fillMaxWidth(),
                                size = 28,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                reference.letter.transliteration,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        reference.vowelVariants.forEach { variant ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                ArabicText(
                                    variant.arabic,
                                    modifier = Modifier.fillMaxWidth(),
                                    size = 24,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    variant.transliteration.ifEmpty { "—" },
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
                if (index != pages[pageIndex].lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = { requestedPage = pageIndex - 1 },
            modifier = Modifier.weight(1f),
            enabled = pageIndex > 0,
        ) {
            Text(stringResource(R.string.previous_page))
        }
        Button(
            onClick = { requestedPage = pageIndex + 1 },
            modifier = Modifier.weight(1f),
            enabled = pageIndex < pages.lastIndex,
        ) {
            Text(stringResource(R.string.next_page))
        }
    }
}

@Composable
internal fun AlphabetAccessCard(
    progress: StudyProgress,
    onStartAlphabetFoundation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canResume = progress.completedAlphabetLessons in
        1 until ArabicFoundations.alphabetLessonCount
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.foundation_course_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    if (canResume) {
                        stringResource(
                            R.string.alphabet_shortcut_progress,
                            progress.completedAlphabetLessons,
                            ArabicFoundations.alphabetLessonCount,
                        )
                    } else {
                        stringResource(R.string.alphabet_shortcut_description)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onStartAlphabetFoundation) {
                Text(
                    stringResource(
                        if (canResume) R.string.continue_alphabet
                        else R.string.study_alphabet,
                    ),
                )
            }
        }
    }
}
