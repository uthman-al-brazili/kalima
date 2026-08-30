package com.kalima.quran.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kalima.quran.R
import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.WordRepository
import com.kalima.quran.quiz.VerseExcerptBuilder

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VerseExplorerPanel(
    word: QuranWord,
    highlightedWordIds: Set<String> = emptySet(),
    showVersePronunciation: Boolean = true,
) {
    val tokens = remember(word.id, word.verseArabic) { WordRepository.verseTokens(word) }
    var selectedTokenIndex by rememberSaveable(word.id) { mutableStateOf<Int?>(null) }
    val selectedToken = selectedTokenIndex?.let(tokens::getOrNull)
    val selectedWord = selectedToken?.word ?: selectedToken?.let { token ->
        word.copy(
            id = "${word.id}:token:${token.index}",
            arabic = token.text,
            lemma = token.text,
            transliteration = "",
            meaning = "",
            root = "",
            grammar = "",
            category = "",
            insight = "",
        )
    }

    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.tap_verse_word),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.height(8.dp))
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp, alignment = androidx.compose.ui.Alignment.End),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                tokens.forEach { token ->
                    val linkedWord = token.word
                    Surface(
                        onClick = { selectedTokenIndex = token.index },
                        color = if (
                            linkedWord?.id == word.id || linkedWord?.id in highlightedWordIds
                        ) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        },
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            token.text,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
            }
        }
    }

    if (selectedWord != null) {
        WordExplorerSheet(
            word = selectedWord,
            indexed = selectedToken?.word != null,
            onDismiss = { selectedTokenIndex = null },
            showVersePronunciation = showVersePronunciation,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WordExplorerSheet(
    word: QuranWord,
    indexed: Boolean,
    onDismiss: () -> Unit,
    concealDetailsForRecall: Boolean = false,
    showVersePronunciation: Boolean = true,
    inCustomList: Boolean = false,
    onToggleCustomList: ((String) -> Unit)? = null,
) {
    val pronouncer = rememberArabicPronouncer()
    var detailsRevealed by remember(word.id, concealDetailsForRecall) {
        mutableStateOf(!concealDetailsForRecall)
    }
    val occurrences = remember(word.id, indexed) {
        if (indexed) WordRepository.concordance(word) else emptyList()
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        scrimColor = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            ArabicText(word.arabic, modifier = Modifier.fillMaxWidth(), size = 42)
            if (indexed) {
                Text(word.transliteration, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (detailsRevealed) {
                    Text(
                        word.meaning,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    Text(
                        stringResource(R.string.recall_before_reveal),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                Text(
                    stringResource(R.string.word_details_not_indexed),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (indexed && detailsRevealed) RootAndGrammar(word.root, word.grammar)
            if (detailsRevealed) {
                Text(
                    word.reference,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PronunciationButton(
                    word = word,
                    pronouncer = pronouncer,
                    modifier = Modifier.weight(1f),
                    dense = true,
                    centerLabel = true,
                    labelRes = R.string.word_audio_short,
                )
                if (showVersePronunciation && detailsRevealed) {
                    VersePronunciationButton(
                        word = word,
                        pronouncer = pronouncer,
                        modifier = Modifier.weight(1f),
                        dense = true,
                        centerLabel = true,
                        labelRes = R.string.ayah_audio_short,
                    )
                }
            }
            if (concealDetailsForRecall) {
                OutlinedButton(
                    onClick = { detailsRevealed = !detailsRevealed },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (detailsRevealed) R.string.hide_meaning else R.string.reveal_meaning,
                        ),
                    )
                }
            }
            if (indexed && onToggleCustomList != null) {
                OutlinedButton(
                    onClick = { onToggleCustomList(word.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (inCustomList) {
                                R.string.remove_custom_list
                            } else {
                                R.string.add_custom_list
                            },
                        ),
                    )
                }
            }
            if (detailsRevealed && occurrences.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                HorizontalDivider()
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.other_occurrences),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.other_occurrences_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                occurrences.forEach { occurrence ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                occurrence.reference,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(occurrence.meaning, fontWeight = FontWeight.SemiBold)
                            HighlightedOccurrenceAyah(occurrence)
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun HighlightedOccurrenceAyah(occurrence: QuranWord) {
    val highlightRange = remember(
        occurrence.id,
        occurrence.arabic,
        occurrence.lemma,
        occurrence.verseArabic,
    ) {
        VerseExcerptBuilder.findRange(occurrence.verseArabic, occurrence.arabic)
            ?: VerseExcerptBuilder.findRange(occurrence.verseArabic, occurrence.lemma)
    }
    val highlightBackground = MaterialTheme.colorScheme.secondary.copy(alpha = 0.42f)
    val highlightContent = MaterialTheme.colorScheme.primary
    val annotatedAyah = remember(
        occurrence.verseArabic,
        highlightRange,
        highlightBackground,
        highlightContent,
    ) {
        buildAnnotatedString {
            append(occurrence.verseArabic)
            highlightRange?.let { range ->
                addStyle(
                    SpanStyle(
                        background = highlightBackground,
                        color = highlightContent,
                        fontWeight = FontWeight.Bold,
                    ),
                    range.first,
                    range.last + 1,
                )
            }
        }
    }
    Text(
        text = annotatedAyah,
        modifier = Modifier.fillMaxWidth(),
        fontSize = 23.sp,
        lineHeight = (23 * 1.55).sp,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineLarge.copy(
            textDirection = TextDirection.Rtl,
            localeList = LocaleList(Locale("ar")),
        ),
    )
}
