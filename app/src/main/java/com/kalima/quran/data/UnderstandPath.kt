package com.kalima.quran.data

import com.kalima.quran.quiz.QuizMastery

enum class UnderstandPathId {
    AlFatihahSevenDays,
    LastTenSurahs,
}

data class UnderstandPathStage(
    val surahNumber: Int,
    val ayahNumber: Int? = null,
    val vocabularyAyahNumber: Int? = ayahNumber,
    val vocabularyWordNumbers: Set<Int> = emptySet(),
)

data class UnderstandPathDefinition(
    val id: UnderstandPathId,
    val stages: List<UnderstandPathStage>,
)

data class UnderstandPathMetric(
    val recognizedOccurrences: Int,
    val totalOccurrences: Int,
    val recalledConcepts: Int,
    val totalConcepts: Int,
) {
    val coveragePercent: Int
        get() = percent(recognizedOccurrences, totalOccurrences)

    val recallPercent: Int
        get() = percent(recalledConcepts, totalConcepts)

    val meetsGoal: Boolean
        get() = coveragePercent >= UnderstandPathProgress.COVERAGE_GOAL_PERCENT &&
            recallPercent >= UnderstandPathProgress.RECALL_GOAL_PERCENT
}

data class UnderstandPathState(
    val definition: UnderstandPathDefinition,
    val metric: UnderstandPathMetric,
    val stageMetrics: List<UnderstandPathMetric>,
    val currentStageIndex: Int,
    val targetWords: List<QuranWord>,
    val unlockedWords: List<QuranWord>,
    val currentStageReadyToAdvance: Boolean,
    val meetsCompletionGoal: Boolean,
)

object UnderstandPathCatalog {
    val definitions: List<UnderstandPathDefinition> = listOf(
        UnderstandPathDefinition(
            id = UnderstandPathId.AlFatihahSevenDays,
            stages = (1..7).map { ayah ->
                if (ayah == 3) {
                    // Ar-Rahman and Ar-Rahim first occur in the basmalah, so the
                    // surah vocabulary asset points those repeated concepts to 1:1.
                    UnderstandPathStage(
                        surahNumber = 1,
                        ayahNumber = ayah,
                        vocabularyAyahNumber = 1,
                        vocabularyWordNumbers = setOf(3, 4),
                    )
                } else {
                    UnderstandPathStage(surahNumber = 1, ayahNumber = ayah)
                }
            },
        ),
        UnderstandPathDefinition(
            id = UnderstandPathId.LastTenSurahs,
            stages = (105..114).map { surah -> UnderstandPathStage(surahNumber = surah) },
        ),
    )

    fun definition(id: UnderstandPathId): UnderstandPathDefinition =
        definitions.first { it.id == id }

    fun targetWords(
        definition: UnderstandPathDefinition,
        words: List<QuranWord>,
    ): List<QuranWord> {
        val surahs = definition.stages.mapTo(mutableSetOf(), UnderstandPathStage::surahNumber)
        return words.filter { it.surahNumber in surahs }
    }

    fun stageWords(
        stage: UnderstandPathStage,
        targetWords: List<QuranWord>,
    ): List<QuranWord> = targetWords.filter { word ->
        word.surahNumber == stage.surahNumber &&
            (stage.vocabularyAyahNumber == null || word.audioLocation?.let { location ->
                location.surah == stage.surahNumber &&
                    location.ayah == stage.vocabularyAyahNumber &&
                    (stage.vocabularyWordNumbers.isEmpty() || location.word in stage.vocabularyWordNumbers)
            } == true)
    }
}

object UnderstandPathProgress {
    const val COVERAGE_GOAL_PERCENT = 90
    const val RECALL_GOAL_PERCENT = 80

    fun calculate(
        progress: StudyProgress,
        pathId: UnderstandPathId,
        words: List<QuranWord>,
    ): UnderstandPathState {
        val definition = UnderstandPathCatalog.definition(pathId)
        val targetWords = UnderstandPathCatalog.targetWords(definition, words)
        val wordsById = words.associateBy(QuranWord::id)
        val recognizedKeys = (progress.learnedIds + progress.alreadyKnownIds)
            .mapNotNullTo(mutableSetOf()) { id -> wordsById[id]?.let(::understandingConceptKey) }
        val introducedKeys = (
            progress.reviewSchedules.keys + progress.learnedIds + progress.reviewingIds +
                progress.alreadyKnownIds
            ).mapNotNullTo(mutableSetOf()) { id -> wordsById[id]?.let(::understandingConceptKey) }
        val correctDaysByKey = mutableMapOf<String, MutableSet<String>>()
        progress.quizCorrectDays.forEach { (id, days) ->
            val key = wordsById[id]?.let(::understandingConceptKey) ?: return@forEach
            correctDaysByKey.getOrPut(key, ::mutableSetOf).addAll(days)
        }
        val recalledKeys = correctDaysByKey
            .filterValues(QuizMastery::isMastered)
            .keys
        val stageWords = definition.stages.map { stage ->
            UnderstandPathCatalog.stageWords(stage, targetWords)
        }
        val stageMetrics = stageWords.map { stageTarget ->
            metricFor(stageTarget, recognizedKeys, recalledKeys)
        }
        val derivedStageIndex = stageWords.indexOfFirst { stageTarget ->
            stageTarget.map(::understandingConceptKey).distinct().any { it !in introducedKeys }
        }.let { firstUnintroduced ->
            if (firstUnintroduced < 0) definition.stages.lastIndex else firstUnintroduced
        }
        val currentStageIndex = if (progress.activeUnderstandPath == pathId) {
            progress.activeUnderstandPathStage.coerceIn(0, definition.stages.lastIndex)
        } else {
            derivedStageIndex
        }
        val currentStageReadyToAdvance = stageWords[currentStageIndex]
            .map(::understandingConceptKey)
            .distinct()
            .all { it in introducedKeys }
        val unlockedIds = stageWords
            .take(currentStageIndex + 1)
            .flatten()
            .mapTo(mutableSetOf(), QuranWord::id)

        return UnderstandPathState(
            definition = definition,
            metric = metricFor(targetWords, recognizedKeys, recalledKeys),
            stageMetrics = stageMetrics,
            currentStageIndex = currentStageIndex,
            targetWords = targetWords,
            unlockedWords = targetWords.filter { it.id in unlockedIds },
            currentStageReadyToAdvance = currentStageReadyToAdvance,
            meetsCompletionGoal = stageMetrics.isNotEmpty() && stageMetrics.all(UnderstandPathMetric::meetsGoal),
        )
    }

    private fun metricFor(
        targetWords: List<QuranWord>,
        recognizedKeys: Set<String>,
        recalledKeys: Set<String>,
    ): UnderstandPathMetric {
        val concepts = targetWords.mapTo(mutableSetOf(), ::understandingConceptKey)
        return UnderstandPathMetric(
            recognizedOccurrences = targetWords.sumOf { word ->
                if (understandingConceptKey(word) in recognizedKeys) word.frequency else 0
            },
            totalOccurrences = targetWords.sumOf(QuranWord::frequency),
            recalledConcepts = concepts.count { it in recalledKeys },
            totalConcepts = concepts.size,
        )
    }

}

internal fun understandingConceptKey(word: QuranWord): String = buildString {
    append(VerseExplorer.normalizeArabic(word.arabic))
    append('\u0000')
    append(VerseExplorer.normalizeArabic(word.lemma.ifBlank { word.arabic }))
}

private fun percent(numerator: Int, denominator: Int): Int =
    if (denominator == 0) 0 else (numerator * 100) / denominator
