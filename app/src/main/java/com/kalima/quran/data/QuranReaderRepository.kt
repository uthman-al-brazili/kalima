package com.kalima.quran.data

import java.io.InputStream

object QuranReaderRepository {
    @Volatile
    private var preloadedFirstPage: List<QuranPageToken> = emptyList()

    @Volatile
    private var pages: List<List<QuranPageToken>> = emptyList()

    @Volatile
    private var verseTextByLocation: Map<Pair<Int, Int>, String> = emptyMap()

    @Volatile
    private var verseTokensByLocation: Map<Pair<Int, Int>, List<QuranPageToken>> = emptyMap()

    @Volatile
    private var firstPageBySurah: Map<Int, Int> = emptyMap()

    @Synchronized
    fun preloadFirstPage(input: InputStream) {
        if (hasFirstPage) {
            input.close()
            return
        }
        val tokens = QuranTextAssetLoader.loadFirstPage(input)
        preloadedFirstPage = tokens
        firstPageBySurah = tokens.associate { it.surahNumber to 1 }
        val groupedVerseTokens = tokens
            .asSequence()
            .filterNot(QuranPageToken::isAyahMarker)
            .groupBy { it.surahNumber to it.ayahNumber }
        verseTokensByLocation = groupedVerseTokens
        verseTextByLocation = groupedVerseTokens.mapValues { (_, words) ->
            words.joinToString(" ", transform = QuranPageToken::arabic)
        }
    }

    @Synchronized
    fun initialize(input: InputStream) {
        if (pages.isNotEmpty()) {
            input.close()
            return
        }
        val tokens = QuranTextAssetLoader.load(input)
        val groupedPages = tokens.groupBy(QuranPageToken::pageNumber)
        require(groupedPages.keys == (1..TOTAL_PAGES).toSet()) {
            "Quran reader must contain all $TOTAL_PAGES pages"
        }
        val loadedPages = (1..TOTAL_PAGES).map { pageNumber -> groupedPages.getValue(pageNumber) }
        preloadedFirstPage = loadedPages.first()
        firstPageBySurah = buildMap {
            tokens.forEach { token -> putIfAbsent(token.surahNumber, token.pageNumber) }
        }
        val groupedVerseTokens = tokens
            .asSequence()
            .filterNot(QuranPageToken::isAyahMarker)
            .groupBy { it.surahNumber to it.ayahNumber }
        verseTokensByLocation = groupedVerseTokens
        verseTextByLocation = groupedVerseTokens.mapValues { (_, words) ->
            words.joinToString(" ", transform = QuranPageToken::arabic)
        }
        pages = loadedPages
    }

    fun page(pageNumber: Int): List<QuranPageToken> =
        pages.getOrNull(pageNumber - 1)
            ?: preloadedFirstPage.takeIf { pageNumber == 1 }
            ?: emptyList()

    fun firstPageForSurah(surahNumber: Int): Int = firstPageBySurah[surahNumber] ?: 1

    fun verseText(surahNumber: Int, ayahNumber: Int): String =
        verseTextByLocation[surahNumber to ayahNumber].orEmpty()

    fun verseTokens(surahNumber: Int, ayahNumber: Int): List<QuranPageToken> =
        verseTokensByLocation[surahNumber to ayahNumber].orEmpty()

    val pageCount: Int get() = pages.size

    val hasFirstPage: Boolean get() = pages.isNotEmpty() || preloadedFirstPage.isNotEmpty()

    val isInitialized: Boolean get() = pages.size == TOTAL_PAGES

    val tokenCount: Int get() = pages.sumOf(List<QuranPageToken>::size)

    const val TOTAL_PAGES = 604
}
