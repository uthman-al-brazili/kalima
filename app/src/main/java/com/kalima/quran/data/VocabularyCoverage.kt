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
    /**
     * Measures Quran word occurrences covered by forms the learner has mastered.
     * Matching both the normalized surface form and lemma avoids treating unrelated
     * Arabic homographs as the same learned vocabulary item.
     */
    fun calculate(
        words: List<QuranWord>,
        recognizedWordIds: Set<String>,
    ): QuranVocabularyCoverage {
        val recognizedKeys = words
            .asSequence()
            .filter { it.id in recognizedWordIds }
            .map(::recognitionKey)
            .toSet()
        val surahs = words
            .asSequence()
            .filter { it.surahNumber != null }
            .groupBy { requireNotNull(it.surahNumber) }
            .map { (surahNumber, surahWords) ->
                val total = surahWords.sumOf(QuranWord::frequency)
                val recognized = surahWords
                    .asSequence()
                    .filter { recognitionKey(it) in recognizedKeys }
                    .sumOf(QuranWord::frequency)
                SurahVocabularyCoverage(
                    surahNumber = surahNumber,
                    recognizedOccurrences = recognized,
                    totalOccurrences = total,
                )
            }
            .sortedBy(SurahVocabularyCoverage::surahNumber)

        return QuranVocabularyCoverage(
            recognizedOccurrences = surahs.sumOf(SurahVocabularyCoverage::recognizedOccurrences),
            totalOccurrences = surahs.sumOf(SurahVocabularyCoverage::totalOccurrences),
            surahs = surahs,
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
}

private fun percentage(numerator: Int, denominator: Int): Int =
    if (denominator == 0) 0 else (numerator * 100) / denominator

private val VOCABULARY_COVERAGE_MILESTONES = listOf(50, 75, 90)
