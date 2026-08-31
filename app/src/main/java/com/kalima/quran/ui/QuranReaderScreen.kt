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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kalima.quran.R
import com.kalima.quran.data.QuranPageToken
import com.kalima.quran.data.QuranReaderRepository
import com.kalima.quran.data.initializeQuranReader
import com.kalima.quran.data.preloadQuranFirstPage
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
    onOpenSettings: () -> Unit,
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
            onOpenSettings = onOpenSettings,
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            beyondViewportPageCount = 0,
            reverseLayout = true,
            key = { it },
        ) { pageIndex ->
            // Pager content may already be retained when the deferred word index becomes ready.
            // Recreate the visible page at that boundary so a saved-on overlay cannot stay stale.
            key(pageIndex, readerIndexReady) {
                QuranPage(
                    pageNumber = pageIndex + 1,
                    tokens = QuranReaderRepository.page(pageIndex + 1),
                    fontSizeSp = fontSizeSp,
                    progress = progress,
                    learningOverlayEnabled = learningOverlayEnabled,
                    readerIndexReady = readerIndexReady,
                    learningNow = learningNow,
                    pageSelectionEnabled = readerReady,
                    onChoosePage = { pagePickerVisible = true },
                    onWordClick = { selectedToken = it },
                )
            }
        }
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
        WordExplorerSheet(
            word = indexedWord ?: token.asUnindexedWord(verseArabic),
            indexed = indexedWord != null,
            onDismiss = { selectedToken = null },
            concealDetailsForRecall = shouldConcealQuranReaderWordDetails(studyAction),
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

private const val LEARNING_STATE_REFRESH_MILLIS = 60_000L
