package com.kalima.quran.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.VerbatimTtsAnnotation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kalima.quran.R
import com.kalima.quran.data.QuranPageToken
import com.kalima.quran.data.QuranReaderRepository
import com.kalima.quran.data.QuranReaderTypography
import com.kalima.quran.data.QuranSurah
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.WordRepository
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ReaderHeader(
    currentSurahs: List<Int>,
    fontSizeSp: Int,
    learningOverlayEnabled: Boolean,
    surahSelectionEnabled: Boolean,
    onChooseSurah: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onLearningOverlayChange: (Boolean) -> Unit,
    onOpenLearningLegend: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val surahsByNumber = remember {
        WordRepository.selectableSurahs.associateBy(QuranSurah::number)
    }
    val currentNames = currentSurahs.mapNotNull(surahsByNumber::get)
    val surahLabel = when (currentNames.size) {
        0 -> stringResource(R.string.quran_reader_title)
        1 -> with(currentNames.first()) {
            "$number  $transliteratedName  ·  $arabicName"
        }
        else -> "${currentNames.first().transliteratedName} – ${currentNames.last().transliteratedName}"
    }
    val decreaseTextSizeDescription = stringResource(R.string.decrease_quran_text_size)
    val increaseTextSizeDescription = stringResource(R.string.increase_quran_text_size)
    val learningOverlayDescription = stringResource(R.string.learning_overlay_toggle_description)

    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onChooseSurah,
                enabled = surahSelectionEnabled,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    surahLabel,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.tab_settings),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.learning_overlay),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
            )
            TextButton(
                onClick = {
                    onFontSizeChange(fontSizeSp - QuranReaderTypography.FONT_SIZE_STEP_SP)
                },
                enabled = fontSizeSp > QuranReaderTypography.MIN_FONT_SIZE_SP,
                modifier = Modifier.semantics {
                    contentDescription = decreaseTextSizeDescription
                },
            ) {
                Text("A−")
            }
            TextButton(
                onClick = {
                    onFontSizeChange(fontSizeSp + QuranReaderTypography.FONT_SIZE_STEP_SP)
                },
                enabled = fontSizeSp < QuranReaderTypography.MAX_FONT_SIZE_SP,
                modifier = Modifier.semantics {
                    contentDescription = increaseTextSizeDescription
                },
            ) {
                Text("A+")
            }
            if (learningOverlayEnabled) {
                TextButton(onClick = onOpenLearningLegend) {
                    Text(stringResource(R.string.learning_overlay_legend))
                }
            }
            Switch(
                checked = learningOverlayEnabled,
                onCheckedChange = onLearningOverlayChange,
                modifier = Modifier.semantics {
                    contentDescription = learningOverlayDescription
                },
            )
        }
    }
}

@Composable
internal fun QuranPage(
    pageNumber: Int,
    tokens: List<QuranPageToken>,
    fontSizeSp: Int,
    progress: StudyProgress,
    learningOverlayEnabled: Boolean,
    readerIndexReady: Boolean,
    learningNow: Instant,
    pageSelectionEnabled: Boolean,
    onChoosePage: () -> Unit,
    onWordClick: (QuranPageToken) -> Unit,
) {
    val sections = remember(tokens) { quranPageSections(tokens) }
    // Resolve stable vocabulary matches once for this composed page. When the overlay is off,
    // no page-wide word or verse lookup is performed.
    val indexedWordIds by produceState<Map<QuranPageToken, String?>?>(
        initialValue = null,
        key1 = tokens,
        key2 = learningOverlayEnabled,
        key3 = readerIndexReady,
    ) {
        value = when {
            !learningOverlayEnabled -> null
            readerIndexReady -> withContext(Dispatchers.Default) {
                tokens.associateWith(WordRepository::readerWordIdFor)
            }
            else -> withContext(Dispatchers.Default) {
                WordRepository.readerWordIdsForPage(tokens)
            }
        }
    }
    val learningStates = remember(
        indexedWordIds,
        progress.learnedIds,
        progress.reviewingIds,
        progress.alreadyKnownIds,
        progress.reviewSchedules,
        progress.spacedRepetitionEnabled,
        learningNow,
    ) {
        indexedWordIds?.mapValues { (_, wordId) ->
            classifyQuranReaderWord(wordId, progress, learningNow)
        }
    }
    Surface(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 2.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            sections.forEach { section ->
                section.startingSurahNumber?.let { surahNumber ->
                    SurahPageHeader(surahNumber)
                }
                QuranPageTextBlock(
                    tokens = section.tokens,
                    fontSizeSp = fontSizeSp,
                    learningOverlayEnabled = learningOverlayEnabled,
                    learningStates = learningStates,
                    onWordClick = onWordClick,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(
                    R.string.quran_page_indicator,
                    pageNumber,
                    QuranReaderRepository.TOTAL_PAGES,
                ),
                modifier = Modifier.clickable(
                    enabled = pageSelectionEnabled,
                    onClick = onChoosePage,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun SurahPageHeader(surahNumber: Int) {
    val surah = WordRepository.selectableSurahs.first { it.number == surahNumber }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ArabicText(
                surah.arabicName,
                modifier = Modifier.fillMaxWidth(),
                size = 24,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "${surah.number} · ${surah.transliteratedName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (surahNumber != 1 && surahNumber != 9) {
        ArabicText(
            BASMALA,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            size = 21,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun QuranPageTextBlock(
    tokens: List<QuranPageToken>,
    fontSizeSp: Int,
    learningOverlayEnabled: Boolean,
    learningStates: Map<QuranPageToken, QuranWordLearningState>?,
    onWordClick: (QuranPageToken) -> Unit,
) {
    val content = remember(tokens) { quranPageLineContent(tokens) }
    val markerColor = MaterialTheme.colorScheme.primary
    val learningColors = quranLearningColors()
    val accessibilityLabels = QuranLearningAccessibilityLabels(
        recognized = stringResource(R.string.recognized_word_accessibility),
        reviewing = stringResource(R.string.reviewing_word_accessibility),
        due = stringResource(R.string.review_due_accessibility),
        unknown = stringResource(R.string.new_word_accessibility),
        unindexed = stringResource(R.string.unindexed_word_accessibility),
    )
    val line = remember(
        content,
        markerColor,
        learningOverlayEnabled,
        learningStates,
        learningColors,
        accessibilityLabels,
    ) {
        buildAnnotatedString {
            append(content.text)
            content.segments.filter { it.token.isAyahMarker }.forEach { segment ->
                addStyle(
                    style = SpanStyle(color = markerColor),
                    start = segment.start,
                    end = segment.endExclusive,
                )
            }
            content.segments.filterNot { it.token.isAyahMarker }.forEachIndexed { index, segment ->
                addStringAnnotation(
                    tag = QURAN_WORD_ANNOTATION,
                    annotation = index.toString(),
                    start = segment.start,
                    end = segment.endExclusive,
                )
                learningStates?.get(segment.token)?.let { classifiedState ->
                    displayedQuranWordLearningState(
                        overlayEnabled = learningOverlayEnabled,
                        state = classifiedState,
                    )
                }?.let { state ->
                    addStyle(
                        style = quranLearningSpanStyle(state, learningColors),
                        start = segment.start,
                        end = segment.endExclusive,
                    )
                    addTtsAnnotation(
                        ttsAnnotation = VerbatimTtsAnnotation(
                            "${segment.token.arabic}, ${accessibilityLabels.forState(state)}",
                        ),
                        start = segment.start,
                        end = segment.endExclusive,
                    )
                }
            }
        }
    }
    val baseStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp * QURAN_LINE_HEIGHT_MULTIPLIER).sp,
        textAlign = TextAlign.Center,
        textDirection = TextDirection.Rtl,
        localeList = LocaleList(Locale("ar")),
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        @Suppress("DEPRECATION")
        ClickableText(
            text = line,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            style = baseStyle,
            softWrap = true,
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Clip,
            onClick = { offset ->
                line.getStringAnnotations(QURAN_WORD_ANNOTATION, offset, offset)
                    .firstOrNull()
                    ?.item
                    ?.toIntOrNull()
                    ?.let(content.words::getOrNull)
                    ?.let(onWordClick)
            },
        )
    }
}

private data class QuranLearningColors(
    val recognized: Color,
    val reviewing: Color,
    val due: Color,
    val unknown: Color,
    val unindexed: Color,
)

private data class QuranLearningAccessibilityLabels(
    val recognized: String,
    val reviewing: String,
    val due: String,
    val unknown: String,
    val unindexed: String,
) {
    fun forState(state: QuranWordLearningState): String = when (state) {
        QuranWordLearningState.Recognized -> recognized
        QuranWordLearningState.Reviewing -> reviewing
        QuranWordLearningState.Due -> due
        QuranWordLearningState.Unknown -> unknown
        QuranWordLearningState.Unindexed -> unindexed
    }
}

@Composable
private fun quranLearningColors() = QuranLearningColors(
    recognized = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
    reviewing = MaterialTheme.colorScheme.primary,
    due = MaterialTheme.colorScheme.error,
    unknown = MaterialTheme.colorScheme.tertiary,
    unindexed = MaterialTheme.colorScheme.onSurface,
)

private fun quranLearningSpanStyle(
    state: QuranWordLearningState,
    colors: QuranLearningColors,
): SpanStyle = when (state) {
    QuranWordLearningState.Recognized -> SpanStyle(color = colors.recognized)
    QuranWordLearningState.Reviewing -> SpanStyle(
        color = colors.reviewing,
        fontWeight = FontWeight.Medium,
    )
    QuranWordLearningState.Due -> SpanStyle(
        color = colors.due,
        fontWeight = FontWeight.SemiBold,
        textDecoration = TextDecoration.Underline,
    )
    QuranWordLearningState.Unknown -> SpanStyle(
        color = colors.unknown,
        textDecoration = TextDecoration.Underline,
    )
    QuranWordLearningState.Unindexed -> SpanStyle(color = colors.unindexed)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuranLearningLegendSheet(onDismiss: () -> Unit) {
    val colors = quranLearningColors()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                stringResource(R.string.learning_overlay_legend_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.learning_overlay_legend_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(10.dp))
            QuranLearningLegendRow(
                state = QuranWordLearningState.Recognized,
                label = stringResource(R.string.learning_state_recognized),
                description = stringResource(R.string.learning_state_recognized_description),
                colors = colors,
            )
            QuranLearningLegendRow(
                state = QuranWordLearningState.Reviewing,
                label = stringResource(R.string.learning_state_reviewing),
                description = stringResource(R.string.learning_state_reviewing_description),
                colors = colors,
            )
            QuranLearningLegendRow(
                state = QuranWordLearningState.Due,
                label = stringResource(R.string.learning_state_due),
                description = stringResource(R.string.learning_state_due_description),
                colors = colors,
            )
            QuranLearningLegendRow(
                state = QuranWordLearningState.Unknown,
                label = stringResource(R.string.learning_state_unknown),
                description = stringResource(R.string.learning_state_unknown_description),
                colors = colors,
            )
            QuranLearningLegendRow(
                state = QuranWordLearningState.Unindexed,
                label = stringResource(R.string.learning_state_unindexed),
                description = stringResource(R.string.learning_state_unindexed_description),
                colors = colors,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuranLearningLegendRow(
    state: QuranWordLearningState,
    label: String,
    description: String,
    colors: QuranLearningColors,
) {
    val style = quranLearningSpanStyle(state, colors)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.learning_overlay_sample_word),
            modifier = Modifier.padding(end = 14.dp),
            color = style.color,
            fontWeight = style.fontWeight ?: FontWeight.Normal,
            textDecoration = style.textDecoration,
            fontSize = 23.sp,
        )
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private const val BASMALA = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"
private const val QURAN_WORD_ANNOTATION = "quran-word"
private const val QURAN_LINE_HEIGHT_MULTIPLIER = 1.55f
