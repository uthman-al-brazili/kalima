package com.kalima.quran.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranTextAssetLoaderTest {
    @Test
    fun containsTheCompleteQuranInStandardPageAndWordOrder() {
        val tokens = QuranTextAssetLoader.load(findAsset().inputStream())
        val verses = tokens.groupBy { it.surahNumber to it.ayahNumber }

        assertEquals(6_236, verses.size)
        assertEquals((1..114).toList(), tokens.map(QuranPageToken::surahNumber).distinct())
        assertEquals((1..604).toList(), tokens.map(QuranPageToken::pageNumber).distinct())
        assertEquals(7, verses.keys.count { it.first == 1 })
        assertEquals(286, verses.keys.count { it.first == 2 })
        assertEquals(6, verses.keys.count { it.first == 114 })
        assertEquals(6_236, tokens.count(QuranPageToken::isAyahMarker))
        assertTrue(tokens.all { it.arabic.isNotBlank() })
        assertTrue(tokens.all { it.lineNumber in 1..15 })
        assertTrue(tokens.first().surahNumber == 1 && tokens.first().pageNumber == 1)
        assertTrue(tokens.last().surahNumber == 114 && tokens.last().pageNumber == 604)

        verses.values.forEach { verseTokens ->
            assertEquals(
                (1..verseTokens.size).toList(),
                verseTokens.map(QuranPageToken::wordNumber),
            )
            assertTrue(verseTokens.last().isAyahMarker)
            assertFalse(verseTokens.dropLast(1).any(QuranPageToken::isAyahMarker))
        }
    }

    private fun findAsset(): File = sequenceOf(
        File("src/main/assets/${QuranTextAssetLoader.ASSET_NAME}.gz"),
        File("app/src/main/assets/${QuranTextAssetLoader.ASSET_NAME}.gz"),
    ).firstOrNull(File::isFile)
        ?: error("Quran reader asset not found from ${File(".").absolutePath}")
}
