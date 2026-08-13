package com.kalima.quran.data

object LearningWordLimiter {
    const val UNLIMITED = 0
    const val MINIMUM_LIMIT = 1
    const val DEFAULT_LIMIT = 100

    /**
     * Keeps every established word available for review and fills the remaining
     * global learning-limit slots with new words in the source's stable order.
     */
    fun apply(
        words: List<QuranWord>,
        learnedIds: Set<String>,
        reviewingIds: Set<String>,
        alreadyKnownIds: Set<String> = emptySet(),
        maximumWords: Int,
    ): List<QuranWord> {
        val practiceWords = words.filterNot { it.id in alreadyKnownIds }
        if (maximumWords == UNLIMITED) return practiceWords

        val establishedIds = learnedIds + reviewingIds
        var newSlots = (maximumWords - establishedIds.size).coerceAtLeast(0)
        return practiceWords.filter { word ->
            if (word.id in establishedIds) {
                true
            } else if (newSlots > 0) {
                newSlots -= 1
                true
            } else {
                false
            }
        }
    }
}

fun StudyProgress.limitNewWords(words: List<QuranWord>): List<QuranWord> =
    LearningWordLimiter.apply(
        words = words,
        learnedIds = learnedIds,
        reviewingIds = reviewingIds,
        alreadyKnownIds = alreadyKnownIds,
        maximumWords = maximumWords,
    )
