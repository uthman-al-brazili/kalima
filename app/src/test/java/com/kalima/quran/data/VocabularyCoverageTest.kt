package com.kalima.quran.data

import com.kalima.quran.localization.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyCoverageTest {
    @Test
    fun `recognized global form counts matching occurrences across surahs`() {
        val learned = word(id = "known", arabic = "ٱلْكِتَابُ", lemma = "كِتَاب")
        val words = listOf(
            learned,
            word(id = "s1-known", arabic = "ٱلْكِتَابُ", lemma = "كِتَاب", surah = 1, frequency = 3),
            word(id = "s1-other", arabic = "رَبّ", lemma = "رَبّ", surah = 1, frequency = 2),
            word(id = "s2-known", arabic = "الْكِتَابُ", lemma = "كِتَاب", surah = 2, frequency = 1),
            word(id = "s2-homograph", arabic = "الْكِتَابُ", lemma = "كَتَبَ", surah = 2, frequency = 4),
        )

        val coverage = VocabularyCoverage.calculate(words, setOf(learned.id))

        assertEquals(4, coverage.recognizedOccurrences)
        assertEquals(10, coverage.totalOccurrences)
        assertEquals(40, coverage.percent)
        assertEquals(50, coverage.nextMilestonePercent)
        assertEquals(
            SurahVocabularyCoverage(1, recognizedOccurrences = 3, totalOccurrences = 5),
            coverage.surahs[0],
        )
        assertEquals(60, coverage.surahs[0].percent)
        assertEquals(75, coverage.surahs[0].nextMilestonePercent)
        assertEquals(
            SurahVocabularyCoverage(2, recognizedOccurrences = 1, totalOccurrences = 5),
            coverage.surahs[1],
        )
    }

    @Test
    fun `coverage milestones stop after ninety percent`() {
        val learned = word(id = "known", arabic = "رَبّ", lemma = "رَبّ")
        val coverage = VocabularyCoverage.calculate(
            words = listOf(
                learned,
                word(id = "s1-known", arabic = "رَبّ", lemma = "رَبّ", surah = 1, frequency = 9),
                word(id = "s1-other", arabic = "دِين", lemma = "دِين", surah = 1, frequency = 1),
            ),
            recognizedWordIds = setOf(learned.id),
        )

        assertEquals(90, coverage.percent)
        assertNull(coverage.nextMilestonePercent)
    }

    @Test
    fun `real corpus covers all surahs and reaches one hundred percent`() {
        val corpusAsset = sequenceOf(
            java.io.File("src/main/assets/${VocabularyAssetLoader.ASSET_NAME}.gz"),
            java.io.File("app/src/main/assets/${VocabularyAssetLoader.ASSET_NAME}.gz"),
        ).first(java.io.File::isFile)
        val corpus = VocabularyAssetLoader.load(corpusAsset.inputStream(), AppLanguage.English)
        val perSurahWordIds = corpus
            .asSequence()
            .filter { it.surahNumber != null }
            .map(QuranWord::id)
            .toSet()

        val complete = VocabularyCoverage.calculate(corpus, perSurahWordIds)
        val oneFrequentWord = corpus.first(QuranWord::isFrequent)
        val partial = VocabularyCoverage.calculate(corpus, setOf(oneFrequentWord.id))

        assertEquals(114, complete.surahs.size)
        assertEquals(complete.totalOccurrences, complete.recognizedOccurrences)
        assertEquals(100, complete.percent)
        assertTrue(partial.recognizedOccurrences > 0)
    }

    private fun word(
        id: String,
        arabic: String,
        lemma: String,
        surah: Int? = null,
        frequency: Int = 1,
    ): QuranWord = QuranWord(
        id = id,
        arabic = arabic,
        lemma = lemma,
        transliteration = "",
        meaning = "",
        root = "—",
        grammar = "",
        category = "",
        reference = "Test 1:1",
        verseArabic = arabic,
        verseMeaning = "",
        insight = "",
        frequency = frequency,
        surahNumber = surah,
    )
}
