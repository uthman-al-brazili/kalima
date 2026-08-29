package com.kalima.quran.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StandaloneTransliterationTest {
    @Test
    fun restoresTheArticleAndDropsTheContextualCaseEnding() {
        assertEquals(
            "al-ʿālamīn",
            standaloneTransliteration("ٱلْعَـٰلَمِينَ", "l-ʿālamīna"),
        )
        assertEquals(
            "al-mus'taqīm",
            standaloneTransliteration("ٱلْمُسْتَقِيمَ", "l-mus'taqīma"),
        )
    }

    @Test
    fun assimilatesTheStandaloneArticleBeforeSunLetters() {
        assertEquals(
            "ar-raḥmān",
            standaloneTransliteration("ٱلرَّحْمَـٰنِ", "l-raḥmāni"),
        )
        assertEquals(
            "ad-dīn",
            standaloneTransliteration("ٱلدِّينِ", "l-dīni"),
        )
    }

    @Test
    fun appliesPausalTanwinAndTaMarbuta() {
        assertEquals("hudā", standaloneTransliteration("هُدًى", "hudan"))
        assertEquals("raḥmah", standaloneTransliteration("رَحْمَةٌ", "raḥmatun"))
    }

    @Test
    fun preservesLongVowelsAndWordsAlreadyEndingInSukun() {
        assertEquals("qālū", standaloneTransliteration("قَالُوا", "qālū"))
        assertEquals("min", standaloneTransliteration("مِنْ", "min"))
    }

    @Test
    fun usesTheStandaloneFormOfAllah() {
        assertEquals("allāh", standaloneTransliteration("ٱللَّهِ", "l-lahi"))
    }

    @Test
    fun normalizesTheCuratedStartupCardsToo() {
        assertEquals("hudā", WordRepository.words.first { it.id == "huda" }.transliteration)
    }
}
