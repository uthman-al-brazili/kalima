package com.kalima.quran.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranVerseAudioCoverageTest {
    @Test
    fun `every bundled ayah has a unique Al Hussary recording location`() {
        val verseLocations = QuranTextAssetLoader.load(findAsset().inputStream())
            .map { QuranVerseAudioLocation(it.surahNumber, it.ayahNumber) }
            .distinct()

        assertEquals(6_236, verseLocations.size)
        assertEquals(6_236, verseLocations.map(QuranVerseAudioLocation::fileName).distinct().size)
        assertTrue(verseLocations.all { it.fileName.matches(Regex("\\d{6}\\.mp3")) })
        assertTrue(verseLocations.all { it.hussaryUrl.startsWith(HUSSARY_BASE_URL) })
    }

    private fun findAsset(): File = sequenceOf(
        File("src/main/assets/${QuranTextAssetLoader.ASSET_NAME}.gz"),
        File("app/src/main/assets/${QuranTextAssetLoader.ASSET_NAME}.gz"),
    ).firstOrNull(File::isFile)
        ?: error("Quran reader asset not found from ${File(".").absolutePath}")

    private companion object {
        const val HUSSARY_BASE_URL = "https://everyayah.com/data/Husary_128kbps/"
    }
}
