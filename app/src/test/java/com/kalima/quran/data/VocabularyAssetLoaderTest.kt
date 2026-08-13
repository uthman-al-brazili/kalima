package com.kalima.quran.data

import com.kalima.quran.localization.AppLanguage
import com.kalima.quran.quiz.VerseExcerptBuilder
import com.kalima.quran.quiz.QuizEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.GZIPInputStream
import kotlin.random.Random

class VocabularyAssetLoaderTest {
    @Test
    fun fullCorpusContainsEverySurahAndExpectedCardCount() {
        val corpus = VocabularyAssetLoader.load(findCorpusAsset().inputStream())

        assertEquals(42_101, corpus.size)
        assertEquals(100, corpus.count(QuranWord::isFrequent))
        assertEquals((1..114).toSet(), corpus.mapNotNull(QuranWord::surahNumber).toSet())
        assertEquals(corpus.size, corpus.map(QuranWord::id).toSet().size)
        (1..114).forEach { surah ->
            assertTrue("A sura $surah deve conter vocabulário", corpus.any { it.surahNumber == surah })
        }
    }

    @Test
    fun fullCorpusRequiredFieldsArePresent() {
        val corpus = VocabularyAssetLoader.load(findCorpusAsset().inputStream())

        assertTrue(corpus.all { it.arabic.isNotBlank() })
        assertTrue(corpus.all { it.meaning.isNotBlank() })
        assertTrue(corpus.all { it.verseArabic.isNotBlank() })
        assertTrue(corpus.all { it.reference.matches(Regex(".+ \\d+:\\d+")) })
    }

    @Test
    fun corpusLoadsPortugueseAndEnglishGlossesWithoutChangingCardIds() {
        val portuguese = VocabularyAssetLoader.load(
            findCorpusAsset().inputStream(),
            AppLanguage.Portuguese,
        )
        val english = VocabularyAssetLoader.load(
            findCorpusAsset().inputStream(),
            AppLanguage.English,
        )

        assertEquals(portuguese.map(QuranWord::id), english.map(QuranWord::id))
        assertEquals("de", portuguese.first().meaning)
        assertEquals("from", english.first().meaning)
        assertEquals("partícula", portuguese.first().grammar)
        assertEquals("particle", english.first().grammar)
        assertTrue(english.all { it.meaning.isNotBlank() })
        assertTrue(english.all { it.verseMeaning.startsWith("In this context") })
    }

    @Test
    fun loaderAcceptsTheExpandedAssetFormatUsedInsideTheApk() {
        val corpus = GZIPInputStream(findCorpusAsset().inputStream()).use(VocabularyAssetLoader::load)

        assertEquals(42_101, corpus.size)
        assertEquals((1..114).toSet(), corpus.mapNotNull(QuranWord::surahNumber).toSet())
    }

    @Test
    fun everyCorpusCardCanHighlightItsWordInTheVerse() {
        val corpus = VocabularyAssetLoader.load(findCorpusAsset().inputStream())
        val missingHighlights = corpus
            .filterNot { VerseExcerptBuilder.build(it).hasHighlight }
            .map(QuranWord::id)

        assertTrue(missingHighlights.take(20).toString(), missingHighlights.isEmpty())
    }

    @Test
    fun everySurahCanGenerateACompleteQuizSession() {
        val corpus = VocabularyAssetLoader.load(findCorpusAsset().inputStream())

        (1..114).forEach { surah ->
            val words = corpus.filter { it.surahNumber == surah }
            val session = QuizEngine.createSession(
                words = words,
                random = Random(surah),
            )
            assertEquals("Sura $surah", QuizEngine.SESSION_SIZE, session.size)
        }
    }

    private fun findCorpusAsset(): File = sequenceOf(
        File("src/main/assets/${VocabularyAssetLoader.ASSET_NAME}.gz"),
        File("app/src/main/assets/${VocabularyAssetLoader.ASSET_NAME}.gz"),
    ).firstOrNull(File::isFile)
        ?: error("Corpus asset not found from ${File(".").absolutePath}")
}
