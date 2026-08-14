package com.kalima.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuranWordAudioLocationTest {
    @Test
    fun `builds the exact Quran com word audio URL`() {
        val location = QuranWordAudioLocation(surah = 1, ayah = 1, word = 1)

        assertEquals(
            "https://audio.qurancdn.com/wbw/001_001_001.mp3",
            location.quranComUrl,
        )
    }

    @Test
    fun `reads a word location from a corpus card id`() {
        assertEquals(
            QuranWordAudioLocation(surah = 114, ayah = 6, word = 3),
            QuranWordAudioLocationResolver.resolve(
                id = "s114-v006-w003",
                arabic = "ٱلنَّاسِ",
                referenceSurah = 114,
                referenceAyah = 6,
                verseArabic = "مِنَ ٱلْجِنَّةِ وَٱلنَّاسِ",
            ),
        )
    }

    @Test
    fun `finds the representative location for a frequent form`() {
        assertEquals(
            QuranWordAudioLocation(surah = 1, ayah = 1, word = 2),
            QuranWordAudioLocationResolver.resolve(
                id = "freq-aa7099e278",
                arabic = "ٱللَّهِ",
                referenceSurah = 1,
                referenceAyah = 1,
                verseArabic = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
            ),
        )
    }

    @Test
    fun `does not count standalone pause marks as words`() {
        assertEquals(
            QuranWordAudioLocation(surah = 2, ayah = 10, word = 4),
            QuranWordAudioLocationResolver.resolve(
                id = "frequent-form",
                arabic = "وَلَهُمْ",
                referenceSurah = 2,
                referenceAyah = 10,
                verseArabic = "فِى قُلُوبِهِم مَّرَضٌ ۖ وَلَهُمْ عَذَابٌ أَلِيمٌۢ",
            ),
        )
    }

    @Test
    fun `rejects a card whose word is absent from its verse`() {
        assertNull(
            QuranWordAudioLocationResolver.resolve(
                id = "frequent-form",
                arabic = "غَيْرُ",
                referenceSurah = 1,
                referenceAyah = 1,
                verseArabic = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
            ),
        )
    }

    @Test
    fun `every startup fallback card has Quran com word audio`() {
        val missing = WordRepository.words.filter { it.audioLocation == null }.map(QuranWord::id)

        assertEquals(emptyList<String>(), missing)
    }
}
