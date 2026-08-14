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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kalima.quran.R
import com.kalima.quran.data.QuranPageToken
import com.kalima.quran.data.QuranReaderRepository
import com.kalima.quran.data.QuranSurah
import com.kalima.quran.data.QuranWordAudioLocation
import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.WordRepository
import kotlinx.coroutines.launch

@Composable
fun QuranReaderScreen() {
    val pageCount = QuranReaderRepository.pageCount
    if (pageCount == 0) {
        QuranReaderUnavailable()
        return
    }

    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    var surahPickerVisible by rememberSaveable { mutableStateOf(false) }
    var pagePickerVisible by rememberSaveable { mutableStateOf(false) }
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
            onChooseSurah = { surahPickerVisible = true },
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            beyondViewportPageCount = 1,
            reverseLayout = true,
            key = { it },
        ) { pageIndex ->
            QuranPage(
                pageNumber = pageIndex + 1,
                tokens = QuranReaderRepository.page(pageIndex + 1),
                onWordClick = { selectedToken = it },
            )
        }

        PageNavigation(
            pageNumber = currentPageNumber,
            pageCount = pageCount,
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
        val indexedWord = remember(token, verseArabic) {
            WordRepository.readerWordFor(token, verseArabic)
        }
        WordExplorerSheet(
            word = indexedWord ?: token.asUnindexedWord(verseArabic),
            indexed = indexedWord != null,
            onDismiss = { selectedToken = null },
            onOpenWord = null,
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
            pageCount = pageCount,
            onSelect = { pageNumber ->
                pagePickerVisible = false
                scope.launch { pagerState.scrollToPage(pageNumber - 1) }
            },
            onDismiss = { pagePickerVisible = false },
        )
    }
}

@Composable
private fun ReaderHeader(
    currentSurahs: List<Int>,
    onChooseSurah: () -> Unit,
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
        Text(
            stringResource(R.string.quran_reader_page_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun QuranPage(
    pageNumber: Int,
    tokens: List<QuranPageToken>,
    onWordClick: (QuranPageToken) -> Unit,
) {
    val lines = remember(tokens) { tokens.groupBy(QuranPageToken::lineNumber).toSortedMap() }
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
            lines.values.forEach { lineTokens ->
                lineTokens
                    .filter { it.ayahNumber == 1 && it.wordNumber == 1 && !it.isAyahMarker }
                    .map(QuranPageToken::surahNumber)
                    .distinct()
                    .forEach { surahNumber -> SurahPageHeader(surahNumber) }

                QuranPageLine(lineTokens, onWordClick)
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
private fun QuranPageLine(
    tokens: List<QuranPageToken>,
    onWordClick: (QuranPageToken) -> Unit,
) {
    val content = remember(tokens) { quranPageLineContent(tokens) }
    val markerColor = MaterialTheme.colorScheme.primary
    val line = remember(content, markerColor) {
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
            }
        }
    }
    val baseStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = QURAN_LINE_FONT_SIZE.sp,
        lineHeight = QURAN_LINE_HEIGHT.sp,
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

@Composable
private fun PageNavigation(
    pageNumber: Int,
    pageCount: Int,
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
            enabled = pageNumber < pageCount,
            modifier = Modifier.semantics { contentDescription = nextDescription },
        ) {
            Text("‹", style = MaterialTheme.typography.headlineSmall)
        }
        TextButton(onClick = onChoosePage) {
            Text(
                stringResource(R.string.quran_page_indicator, pageNumber, pageCount),
                fontWeight = FontWeight.SemiBold,
            )
        }
        TextButton(
            onClick = onPrevious,
            enabled = pageNumber > 1,
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
private const val QURAN_LINE_FONT_SIZE = 22f
private const val QURAN_LINE_HEIGHT = 34f
