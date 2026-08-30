package com.kalima.quran.data

import java.time.Instant

/**
 * The learner's guided focus and supporting vocabulary sets, resolved into one
 * stable study source. Guided words come first so the global learning limit
 * fills path stages before introducing optional supporting vocabulary.
 */
data class StudyPlanSelection(
    val focus: UnderstandPathState?,
    val focusWords: List<QuranWord>,
    val supportingWords: List<QuranWord>,
    val combinedWords: List<QuranWord>,
)

object StudyPlan {
    fun calculate(
        progress: StudyProgress,
        corpusWords: List<QuranWord>,
        supportingWords: List<QuranWord> = WordRepository.wordsFor(
            progress.studyScopes,
            progress.selectedSurahs,
            progress.customStudyIds,
        ),
    ): StudyPlanSelection {
        val focus = progress.activeUnderstandPath?.let { pathId ->
            UnderstandPathProgress.calculate(progress, pathId, corpusWords)
        }
        if (focus == null) {
            return StudyPlanSelection(
                focus = null,
                focusWords = emptyList(),
                supportingWords = supportingWords,
                combinedWords = supportingWords,
            )
        }

        val establishedIds = progress.learnedIds + progress.reviewingIds +
            progress.alreadyKnownIds + progress.reviewSchedules.keys
        val wordsById = corpusWords.associateBy(QuranWord::id)
        val establishedConceptKeys = establishedIds.mapNotNullTo(mutableSetOf()) { id ->
            wordsById[id]?.let(::understandingConceptKey)
        }
        val unlockedConceptKeys = focus.unlockedWords
            .mapTo(mutableSetOf(), ::understandingConceptKey)
        val lockedFutureConceptKeys = focus.targetWords
            .asSequence()
            .map(::understandingConceptKey)
            .filterNot(unlockedConceptKeys::contains)
            .toSet()
        val focusWords = (
            focus.unlockedWords + focus.targetWords.filter { it.id in establishedIds }
            ).distinctBy(QuranWord::id)
        val visibleSupportingWords = supportingWords.filter { word ->
            val conceptKey = understandingConceptKey(word)
            conceptKey !in lockedFutureConceptKeys || conceptKey in establishedConceptKeys
        }.let { visibleWords ->
            val seenConceptKeys = focusWords.mapTo(mutableSetOf(), ::understandingConceptKey)
            visibleWords.filter { word ->
                val conceptKey = understandingConceptKey(word)
                if (word.id in establishedIds) {
                    seenConceptKeys += conceptKey
                    true
                } else {
                    seenConceptKeys.add(conceptKey)
                }
            }
        }

        return StudyPlanSelection(
            focus = focus,
            focusWords = focusWords,
            supportingWords = visibleSupportingWords,
            combinedWords = (focusWords + visibleSupportingWords).distinctBy(QuranWord::id),
        )
    }

    fun orderedQueue(
        words: List<QuranWord>,
        focusWordIds: Set<String>,
        schedules: Map<String, ReviewSchedule>,
        spacedRepetitionEnabled: Boolean,
        now: Instant,
        newStartIndex: Int,
    ): List<QuranWord> {
        val focusWords = words.filter { it.id in focusWordIds }
        val supportingWords = words.filterNot { it.id in focusWordIds }
        if (!spacedRepetitionEnabled) {
            return ReviewQueue.rotated(focusWords, newStartIndex) +
                ReviewQueue.rotated(supportingWords, newStartIndex)
        }

        val dueWords = ReviewQueue.dueWords(words, schedules, now)
        val newFocusWords = ReviewQueue.newWords(focusWords, schedules)
        val newSupportingWords = ReviewQueue.newWords(supportingWords, schedules)
        return dueWords + ReviewQueue.rotated(newFocusWords, newStartIndex) +
            ReviewQueue.rotated(newSupportingWords, newStartIndex)
    }
}
