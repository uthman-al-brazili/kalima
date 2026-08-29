package com.kalima.quran.data

data class SurahVocabularyCoverage(
    val surahNumber: Int,
    val recognizedOccurrences: Int,
    val totalOccurrences: Int,
) {
    val percent: Int
        get() = percentage(recognizedOccurrences, totalOccurrences)

    val nextMilestonePercent: Int?
        get() = VOCABULARY_COVERAGE_MILESTONES.firstOrNull { percent < it }
}

data class QuranVocabularyCoverage(
    val recognizedOccurrences: Int,
    val totalOccurrences: Int,
    val surahs: List<SurahVocabularyCoverage>,
) {
    val percent: Int
        get() = percentage(recognizedOccurrences, totalOccurrences)

    val nextMilestonePercent: Int?
        get() = VOCABULARY_COVERAGE_MILESTONES.firstOrNull { percent < it }
}

object VocabularyCoverage {
    @Volatile
    private var indexedCorpus: IndexedCorpus? = null

    /**
     * Measures Quran word occurrences covered by forms the learner has mastered.
     * Matching both the normalized surface form and lemma avoids treating unrelated
     * Arabic homographs as the same learned vocabulary item.
     */
    fun calculate(
        words: List<QuranWord>,
        recognizedWordIds: Set<String>,
    ): QuranVocabularyCoverage {
        val index = indexFor(words)
        val recognizedKeys = recognizedWordIds.mapNotNullTo(mutableSetOf()) {
            index.recognitionKeyByWordId[it]
        }
        val recognizedBySurah = mutableMapOf<Int, Int>()
        recognizedKeys.forEach { key ->
            index.occurrencesByKeyAndSurah[key]?.forEach { (surahNumber, occurrences) ->
                recognizedBySurah[surahNumber] =
                    recognizedBySurah.getOrDefault(surahNumber, 0) + occurrences
            }
        }
        val surahs = index.totalOccurrencesBySurah.map { (surahNumber, total) ->
            SurahVocabularyCoverage(
                surahNumber = surahNumber,
                recognizedOccurrences = recognizedBySurah.getOrDefault(surahNumber, 0),
                totalOccurrences = total,
            )
        }

        return QuranVocabularyCoverage(
            recognizedOccurrences = surahs.sumOf(SurahVocabularyCoverage::recognizedOccurrences),
            totalOccurrences = surahs.sumOf(SurahVocabularyCoverage::totalOccurrences),
            surahs = surahs,
        )
    }

    private fun indexFor(words: List<QuranWord>): IndexedCorpus {
        indexedCorpus?.takeIf { it.words === words }?.let { return it }
        return synchronized(this) {
            indexedCorpus?.takeIf { it.words === words } ?: buildIndex(words).also {
                indexedCorpus = it
            }
        }
    }

    private fun buildIndex(words: List<QuranWord>): IndexedCorpus {
        val recognitionKeyByWordId = HashMap<String, RecognitionKey>(words.size)
        val totalOccurrencesBySurah = sortedMapOf<Int, Int>()
        val occurrencesByKeyAndSurah = HashMap<RecognitionKey, MutableMap<Int, Int>>()
        words.forEach { word ->
            val key = recognitionKey(word)
            recognitionKeyByWordId[word.id] = key
            val surahNumber = word.surahNumber ?: return@forEach
            totalOccurrencesBySurah[surahNumber] =
                totalOccurrencesBySurah.getOrDefault(surahNumber, 0) + word.frequency
            val occurrencesBySurah = occurrencesByKeyAndSurah.getOrPut(key, ::mutableMapOf)
            occurrencesBySurah[surahNumber] =
                occurrencesBySurah.getOrDefault(surahNumber, 0) + word.frequency
        }
        return IndexedCorpus(
            words = words,
            recognitionKeyByWordId = recognitionKeyByWordId,
            totalOccurrencesBySurah = totalOccurrencesBySurah,
            occurrencesByKeyAndSurah = occurrencesByKeyAndSurah,
        )
    }

    private fun recognitionKey(word: QuranWord): RecognitionKey = RecognitionKey(
        form = VerseExplorer.normalizeArabic(word.arabic),
        lemma = VerseExplorer.normalizeArabic(word.lemma.ifBlank { word.arabic }),
    )

    private data class RecognitionKey(
        val form: String,
        val lemma: String,
    )

    private data class IndexedCorpus(
        val words: List<QuranWord>,
        val recognitionKeyByWordId: Map<String, RecognitionKey>,
        val totalOccurrencesBySurah: Map<Int, Int>,
        val occurrencesByKeyAndSurah: Map<RecognitionKey, Map<Int, Int>>,
    )
}

private fun percentage(numerator: Int, denominator: Int): Int =
    if (denominator == 0) 0 else (numerator * 100) / denominator

private val VOCABULARY_COVERAGE_MILESTONES = listOf(50, 75, 90)
