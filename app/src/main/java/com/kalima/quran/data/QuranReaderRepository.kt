package com.kalima.quran.data

import java.io.InputStream

object QuranReaderRepository {
    @Volatile
    private var pages: List<List<QuranPageToken>> = emptyList()

    @Volatile
    private var verseTextByLocation: Map<Pair<Int, Int>, String> = emptyMap()

    @Volatile
    private var verseTokensByLocation: Map<Pair<Int, Int>, List<QuranPageToken>> = emptyMap()

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
        pages = (1..TOTAL_PAGES).map { pageNumber -> groupedPages.getValue(pageNumber) }
        val groupedVerseTokens = tokens
            .asSequence()
            .filterNot(QuranPageToken::isAyahMarker)
            .groupBy { it.surahNumber to it.ayahNumber }
        verseTokensByLocation = groupedVerseTokens
        verseTextByLocation = groupedVerseTokens.mapValues { (_, words) ->
            words.joinToString(" ", transform = QuranPageToken::arabic)
        }
    }

    fun page(pageNumber: Int): List<QuranPageToken> = pages.getOrNull(pageNumber - 1).orEmpty()

    fun firstPageForSurah(surahNumber: Int): Int = pages.indexOfFirst { page ->
        page.any { it.surahNumber == surahNumber }
    }.let { index -> if (index >= 0) index + 1 else 1 }

    fun verseText(surahNumber: Int, ayahNumber: Int): String =
        verseTextByLocation[surahNumber to ayahNumber].orEmpty()

    fun verseTokens(surahNumber: Int, ayahNumber: Int): List<QuranPageToken> =
        verseTokensByLocation[surahNumber to ayahNumber].orEmpty()

    val pageCount: Int get() = pages.size

    val tokenCount: Int get() = pages.sumOf(List<QuranPageToken>::size)

    const val TOTAL_PAGES = 604
}
