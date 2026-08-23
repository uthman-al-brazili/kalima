package com.kalima.quran.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.audio.ArabicPronouncer
import com.kalima.quran.audio.VerseAudioPlaybackProgress
import com.kalima.quran.audio.verseWordIndexAt
import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.WordRepository

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VerseExplorerPanel(
    word: QuranWord,
    onOpenWord: ((String) -> Unit)? = null,
    highlightedTokenIndex: Int? = null,
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
                        color = if (token.index == highlightedTokenIndex) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else if (linkedWord?.id == word.id) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        },
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            token.text,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            color = if (token.index == highlightedTokenIndex) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = if (token.index == highlightedTokenIndex) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
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
            onOpenWord = if (selectedToken?.word != null) onOpenWord else null,
        )
    }
}

@Composable
fun RecitableVerseExplorer(
    word: QuranWord,
    pronouncer: ArabicPronouncer,
    modifier: Modifier = Modifier,
    onOpenWord: ((String) -> Unit)? = null,
) {
    val tokenCount = remember(word.id, word.verseArabic) {
        WordRepository.verseTokens(word).size
    }
    var playbackProgress by remember(word.id) {
        mutableStateOf(VerseAudioPlaybackProgress())
    }
    DisposableEffect(word.id, pronouncer) {
        onDispose(pronouncer::stopVerse)
    }
    val highlightedTokenIndex = if (playbackProgress.isPlaying) {
        verseWordIndexAt(
            positionMs = playbackProgress.positionMs,
            durationMs = playbackProgress.durationMs,
            wordCount = tokenCount,
        )
    } else {
        null
    }

    Column(modifier) {
        VerseExplorerPanel(
            word = word,
            onOpenWord = onOpenWord,
            highlightedTokenIndex = highlightedTokenIndex,
        )
        VersePronunciationButton(
            word = word,
            pronouncer = pronouncer,
            modifier = Modifier.fillMaxWidth(),
            labelRes = R.string.hussary_verse_recitation,
            onPlaybackProgress = { playbackProgress = it },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WordExplorerSheet(
    word: QuranWord,
    indexed: Boolean,
    onDismiss: () -> Unit,
    onOpenWord: ((String) -> Unit)?,
    inCustomList: Boolean = false,
    onToggleCustomList: ((String) -> Unit)? = null,
) {
    val pronouncer = rememberArabicPronouncer()
    val occurrences = remember(word.id, indexed) {
        if (indexed) WordRepository.concordance(word) else emptyList()
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            ArabicText(word.arabic, modifier = Modifier.fillMaxWidth(), size = 42)
            if (indexed) {
                Text(word.transliteration, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(word.meaning, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            } else {
                Text(
                    stringResource(R.string.word_details_not_indexed),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (indexed) RootAndGrammar(word.root, word.grammar)
            Text(word.reference, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            CitationActions(word)
            PronunciationButton(
                word = word,
                pronouncer = pronouncer,
                modifier = Modifier.fillMaxWidth(),
            )
            VersePronunciationButton(
                word = word,
                pronouncer = pronouncer,
                modifier = Modifier.fillMaxWidth(),
            )
            if (indexed && onToggleCustomList != null) {
                Button(
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
            if (onOpenWord != null) {
                Button(
                    onClick = {
                        onOpenWord(word.id)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.open_word_for_study))
                }
            }
            if (occurrences.isNotEmpty()) {
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
                            ArabicText(
                                occurrence.verseArabic,
                                modifier = Modifier.fillMaxWidth(),
                                size = 23,
                            )
                            if (onOpenWord != null) {
                                TextButton(onClick = { selectedOccurrenceAction(onOpenWord, occurrence, onDismiss) }) {
                                    Text(stringResource(R.string.open_word_for_study))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

private fun selectedOccurrenceAction(
    onOpenWord: ((String) -> Unit)?,
    occurrence: QuranWord,
    onDismiss: () -> Unit,
) {
    onOpenWord?.invoke(occurrence.id)
    onDismiss()
}
