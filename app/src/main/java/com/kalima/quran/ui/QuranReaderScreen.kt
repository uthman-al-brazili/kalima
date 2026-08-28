package com.kalima.quran.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.VerbatimTtsAnnotation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
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
import com.kalima.quran.data.initializeQuranReader
import com.kalima.quran.data.preloadQuranFirstPage
import com.kalima.quran.data.QuranReaderTypography
import com.kalima.quran.data.QuranSurah
import com.kalima.quran.data.QuranWordAudioLocation
import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.WordRepository
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun QuranReaderScreen(
    progress: StudyProgress,
    fontSizeSp: Int,
    customStudyIds: Set<String>,
    learningOverlayEnabled: Boolean,
    onFontSizeChange: (Int) -> Unit,
    onLearningOverlayChange: (Boolean) -> Unit,
    onToggleCustomList: (String) -> Unit,
    onStudyWord: (String) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val firstPageAvailable by produceState<Boolean?>(
        initialValue = true.takeIf { QuranReaderRepository.hasFirstPage },
        key1 = context,
    ) {
        if (value != true) {
            withContext(Dispatchers.IO) {
                preloadQuranFirstPage(context)
            }
            value = QuranReaderRepository.hasFirstPage
        }
    }
    if (firstPageAvailable == null) {
        QuranReaderLoading()
        return
    }
    if (firstPageAvailable == false) {
        QuranReaderUnavailable()
        return
    }

    val readerReady by produceState(
        initialValue = QuranReaderRepository.isInitialized,
        key1 = context,
    ) {
        if (!value) {
            withContext(Dispatchers.IO) {
                initializeQuranReader(context)
            }
            value = QuranReaderRepository.isInitialized
        }
    }

    // Word lookup is useful after a tap, but it must not delay the first readable Quran page.
    val readerIndexReady by produceState(
        initialValue = WordRepository.isReaderIndexPrepared(),
        key1 = context,
    ) {
        if (!value) {
            withContext(Dispatchers.Default) {
                WordRepository.prepareReaderIndex()
            }
            value = WordRepository.isReaderIndexPrepared()
        }
    }
    val learningNow by produceState(
        initialValue = Instant.now(),
        key1 = learningOverlayEnabled,
    ) {
        if (!learningOverlayEnabled) return@produceState
        while (true) {
            delay(LEARNING_STATE_REFRESH_MILLIS)
            value = Instant.now()
        }
    }

    val availablePageCount = if (readerReady) QuranReaderRepository.pageCount else 1

    val pagerState = rememberPagerState(pageCount = { availablePageCount })
    val scope = rememberCoroutineScope()
    var surahPickerVisible by rememberSaveable { mutableStateOf(false) }
    var pagePickerVisible by rememberSaveable { mutableStateOf(false) }
    var learningLegendVisible by rememberSaveable { mutableStateOf(false) }
    var selectedToken by remember { mutableStateOf<QuranPageToken?>(null) }
    val currentPageNumber = pagerState.currentPage + 1
    val currentPage = remember(currentPageNumber) {
        QuranReaderRepository.page(currentPageNumber)
    }
    val currentSurahs = remember(currentPage) {
        currentPage.map(QuranPageToken::surahNumber).distinct()
    }

    Column(Modifier.fillMaxSize()) {
        ReaderHeader(
            currentSurahs = currentSurahs,
            fontSizeSp = fontSizeSp,
            learningOverlayEnabled = learningOverlayEnabled,
            surahSelectionEnabled = readerReady,
            onChooseSurah = { surahPickerVisible = true },
            onFontSizeChange = onFontSizeChange,
            onLearningOverlayChange = onLearningOverlayChange,
            onOpenLearningLegend = { learningLegendVisible = true },
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            beyondViewportPageCount = 0,
            reverseLayout = true,
            key = { it },
        ) { pageIndex ->
            QuranPage(
                pageNumber = pageIndex + 1,
                tokens = QuranReaderRepository.page(pageIndex + 1),
                fontSizeSp = fontSizeSp,
                progress = progress,
                learningOverlayEnabled = learningOverlayEnabled,
                readerIndexReady = readerIndexReady,
                learningNow = learningNow,
                onWordClick = { selectedToken = it },
            )
        }

        PageNavigation(
            pageNumber = currentPageNumber,
            pageCount = QuranReaderRepository.TOTAL_PAGES,
            navigationEnabled = readerReady,
            onPrevious = {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
            },
            onChoosePage = { pagePickerVisible = true },
            onNext = {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
        )
    }

    selectedToken?.let { token ->
        val verseArabic = QuranReaderRepository.verseText(token.surahNumber, token.ayahNumber)
        val indexedWord = remember(token, verseArabic, readerIndexReady) {
            if (readerIndexReady) WordRepository.readerWordFor(token, verseArabic) else null
        }
        val learningState = remember(
            indexedWord?.id,
            progress.learnedIds,
            progress.reviewingIds,
            progress.alreadyKnownIds,
            progress.reviewSchedules,
            progress.spacedRepetitionEnabled,
        ) {
            classifyQuranReaderWord(indexedWord?.id, progress)
        }
        val studyAction = quranReaderStudyActionFor(learningState)
        val studyActionLabel = studyAction?.let { action ->
            stringResource(
                when (action) {
                    QuranReaderStudyAction.Learn -> R.string.learn_this_word
                    QuranReaderStudyAction.Review -> R.string.practice_from_memory
                    QuranReaderStudyAction.PracticeAgain -> R.string.practice_again
                },
            )
        }
        WordExplorerSheet(
            word = indexedWord ?: token.asUnindexedWord(verseArabic),
            indexed = indexedWord != null,
            onDismiss = { selectedToken = null },
            onOpenWord = indexedWord?.let {
                { wordId ->
                    launchQuranReaderWordStudy(wordId, learningState, onStudyWord)
                    Unit
                }
            },
            studyActionLabel = studyActionLabel,
            inCustomList = indexedWord?.id?.let { it in customStudyIds } == true,
            onToggleCustomList = onToggleCustomList,
        )
    }

    if (surahPickerVisible) {
        SurahPickerSheet(
            surahs = WordRepository.selectableSurahs,
            selectedSurahNumber = currentSurahs.firstOrNull(),
            onSelect = { surahNumber ->
                surahPickerVisible = false
                scope.launch {
                    pagerState.scrollToPage(
                        QuranReaderRepository.firstPageForSurah(surahNumber) - 1,
                    )
                }
            },
            onDismiss = { surahPickerVisible = false },
        )
    }

    if (pagePickerVisible) {
        PagePickerSheet(
            currentPage = currentPageNumber,
            pageCount = QuranReaderRepository.TOTAL_PAGES,
            onSelect = { pageNumber ->
                pagePickerVisible = false
                scope.launch { pagerState.scrollToPage(pageNumber - 1) }
            },
            onDismiss = { pagePickerVisible = false },
        )
    }

    if (learningLegendVisible) {
        QuranLearningLegendSheet(onDismiss = { learningLegendVisible = false })
    }
}

@Composable
private fun ReaderHeader(
    currentSurahs: List<Int>,
    fontSizeSp: Int,
    learningOverlayEnabled: Boolean,
    surahSelectionEnabled: Boolean,
    onChooseSurah: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onLearningOverlayChange: (Boolean) -> Unit,
    onOpenLearningLegend: () -> Unit,
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

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.quran_reader_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
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
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.quran_reader_page_hint),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
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
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.learning_overlay),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
            )
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
private fun QuranPage(
    pageNumber: Int,
    tokens: List<QuranPageToken>,
    fontSizeSp: Int,
    progress: StudyProgress,
    learningOverlayEnabled: Boolean,
    readerIndexReady: Boolean,
    learningNow: Instant,
    onWordClick: (QuranPageToken) -> Unit,
) {
    val sections = remember(tokens) { quranPageSections(tokens) }
    // Resolve stable vocabulary matches once for this composed page. When the overlay is off,
    // no page-wide word or verse lookup is performed.
    val indexedWordIds = remember(tokens, learningOverlayEnabled, readerIndexReady) {
        if (!learningOverlayEnabled || !readerIndexReady) {
            null
        } else {
            val verseTextByReference = mutableMapOf<Pair<Int, Int>, String>()
            tokens.asSequence()
                .filterNot(QuranPageToken::isAyahMarker)
                .associateWith { token ->
                    val reference = token.surahNumber to token.ayahNumber
                    val verseArabic = verseTextByReference.getOrPut(reference) {
                        QuranReaderRepository.verseText(token.surahNumber, token.ayahNumber)
                    }
                    WordRepository.readerWordFor(token, verseArabic)?.id
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
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
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
                pageNumber.toString(),
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
private fun QuranLearningLegendSheet(onDismiss: () -> Unit) {
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

@Composable
private fun PageNavigation(
    pageNumber: Int,
    pageCount: Int,
    navigationEnabled: Boolean,
    onPrevious: () -> Unit,
    onChoosePage: () -> Unit,
    onNext: () -> Unit,
) {
    val previousDescription = stringResource(R.string.previous_page)
    val nextDescription = stringResource(R.string.next_page)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onNext,
            enabled = navigationEnabled && pageNumber < pageCount,
            modifier = Modifier.semantics { contentDescription = nextDescription },
        ) {
            Text("‹", style = MaterialTheme.typography.headlineSmall)
        }
        TextButton(
            onClick = onChoosePage,
            enabled = navigationEnabled,
        ) {
            Text(
                stringResource(R.string.quran_page_indicator, pageNumber, pageCount),
                fontWeight = FontWeight.SemiBold,
            )
        }
        TextButton(
            onClick = onPrevious,
            enabled = navigationEnabled && pageNumber > 1,
            modifier = Modifier.semantics { contentDescription = previousDescription },
        ) {
            Text("›", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurahPickerSheet(
    surahs: List<QuranSurah>,
    selectedSurahNumber: Int?,
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
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(10.dp))
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(filteredSurahs.size, key = { filteredSurahs[it].number }) { index ->
                    val surah = filteredSurahs[index]
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(surah.number) },
                        color = if (surah.number == selectedSurahNumber) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                surah.number.toString(),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                surah.transliteratedName,
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    surah.arabicName,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    stringResource(
                                        R.string.quran_page_short,
                                        QuranReaderRepository.firstPageForSurah(surah.number),
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PagePickerSheet(
    currentPage: Int,
    pageCount: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var pageText by rememberSaveable(currentPage) { mutableStateOf(currentPage.toString()) }
    val pageNumber = pageText.toIntOrNull()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                stringResource(R.string.choose_quran_page),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = pageText,
                onValueChange = { value -> pageText = value.filter(Char::isDigit).take(3) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.quran_page_range, pageCount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onSelect(requireNotNull(pageNumber)) },
                enabled = pageNumber in 1..pageCount,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(stringResource(R.string.open_quran_page))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuranReaderLoading() {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.quran_reader_loading),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QuranReaderUnavailable() {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.quran_reader_unavailable),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun QuranPageToken.asUnindexedWord(verseArabic: String): QuranWord {
    val surahName = WordRepository.selectableSurahs.firstOrNull { it.number == surahNumber }
        ?.transliteratedName
        ?: "Surah $surahNumber"
    return QuranWord(
        id = "reader-$surahNumber-$ayahNumber-$wordNumber",
        arabic = arabic,
        lemma = arabic,
        transliteration = "",
        meaning = "",
        root = "",
        grammar = "",
        category = "",
        reference = "$surahName $surahNumber:$ayahNumber",
        verseArabic = verseArabic,
        verseMeaning = "",
        insight = "",
        surahNumber = surahNumber,
        audioLocation = QuranWordAudioLocation(surahNumber, ayahNumber, wordNumber),
    )
}

private const val BASMALA = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"
private const val QURAN_WORD_ANNOTATION = "quran-word"
private const val QURAN_LINE_HEIGHT_MULTIPLIER = 1.55f
private const val LEARNING_STATE_REFRESH_MILLIS = 60_000L
