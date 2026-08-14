package com.kalima.quran.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranTextAssetLoaderTest {
    @Test
    fun containsTheCompleteQuranInCanonicalOrder() {
        val verses = QuranTextAssetLoader.load(findAsset().inputStream())

        assertEquals(6_236, verses.size)
        assertEquals((1..114).toList(), verses.map(QuranVerse::surahNumber).distinct())
        assertEquals(7, verses.count { it.surahNumber == 1 })
        assertEquals(286, verses.count { it.surahNumber == 2 })
        assertEquals(6, verses.count { it.surahNumber == 114 })
        assertTrue(verses.all { it.arabic.isNotBlank() })
        verses.groupBy(QuranVerse::surahNumber).values.forEach { surah ->
            assertEquals((1..surah.size).toList(), surah.map(QuranVerse::ayahNumber))
        }
    }

    private fun findAsset(): File = sequenceOf(
        File("src/main/assets/${QuranTextAssetLoader.ASSET_NAME}.gz"),
        File("app/src/main/assets/${QuranTextAssetLoader.ASSET_NAME}.gz"),
    ).firstOrNull(File::isFile)
        ?: error("Quran reader asset not found from ${File(".").absolutePath}")
}
