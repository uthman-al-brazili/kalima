package com.kalima.quran.data

import java.io.InputStream

object QuranReaderRepository {
    @Volatile
    private var versesBySurah: Map<Int, List<QuranVerse>> = emptyMap()

    @Synchronized
    fun initialize(input: InputStream) {
        if (versesBySurah.isNotEmpty()) {
            input.close()
            return
        }
        versesBySurah = QuranTextAssetLoader.load(input).groupBy(QuranVerse::surahNumber)
    }

    fun versesFor(surahNumber: Int): List<QuranVerse> = versesBySurah[surahNumber].orEmpty()

    val verseCount: Int get() = versesBySurah.values.sumOf(List<QuranVerse>::size)
}
