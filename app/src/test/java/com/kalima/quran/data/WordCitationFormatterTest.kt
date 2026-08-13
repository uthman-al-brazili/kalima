package com.kalima.quran.data

import org.junit.Assert.assertTrue
import org.junit.Test

class WordCitationFormatterTest {
    @Test
    fun sharedTextAlwaysCarriesReferenceAndContentIdentity() {
        val word = QuranWord(
            "id-1", "نُور", "نور", "nūr", "light", "ن و ر", "noun", "test",
            "Quran 24:35", "ٱللَّهُ نُورُ ٱلسَّمَاوَاتِ", "God is the light...", "test",
        )
        val text = WordCitationFormatter.format(word, "corpus-v1")
        assertTrue(text.contains(word.reference))
        assertTrue(text.contains("Kalima card: ${word.id}"))
        assertTrue(text.contains("Corpus: corpus-v1"))
    }
}
