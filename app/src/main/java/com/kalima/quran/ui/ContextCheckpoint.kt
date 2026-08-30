package com.kalima.quran.ui

import com.kalima.quran.data.QuranWord
import com.kalima.quran.quiz.FullVerseCloze
import com.kalima.quran.quiz.QuizEngine
import com.kalima.quran.quiz.QuizQuestion
import com.kalima.quran.quiz.QuizQuestionType
import com.kalima.quran.quiz.VerseExcerptBuilder
import kotlin.random.Random

internal data class ContextCheckpointQuestion(
    val quizQuestion: QuizQuestion,
    val ayah: FullVerseCloze,
) {
    val word: QuranWord get() = quizQuestion.word
    val options: List<String> get() = quizQuestion.options
    val correctOptionIndex: Int get() = quizQuestion.correctOptionIndex
}

internal enum class ContextCheckpointFeedbackState {
    Unanswered,
    Correct,
    Incorrect,
}

/**
 * Builds one stable checkpoint from words practiced in this mission and distractors from the
 * active collection. Sorting makes the seed independent of repository or set iteration order.
 */
internal fun buildContextCheckpointQuestion(
    practicedWords: List<QuranWord>,
    activeCollection: List<QuranWord>,
    seed: Int,
    preferredWordIds: Set<String> = emptySet(),
): ContextCheckpointQuestion? {
    val candidates = practicedWords
        .distinctBy(QuranWord::id)
        .filter { it.arabic.isNotBlank() }
        .sortedBy(QuranWord::id)
    val optionWords = activeCollection
        .distinctBy(QuranWord::id)
        .sortedBy(QuranWord::id)
    if (candidates.isEmpty() || optionWords.isEmpty()) return null

    val random = Random(seed)
    val orderedCandidates = candidates.filter { it.id in preferredWordIds }.shuffled(random) +
        candidates.filterNot { it.id in preferredWordIds }.shuffled(random)
    return orderedCandidates.firstNotNullOfOrNull { word ->
        val ayah = VerseExcerptBuilder.buildFullCloze(word) ?: return@firstNotNullOfOrNull null
        val quizQuestion = QuizEngine.createQuestionOrNull(
            word = word,
            type = QuizQuestionType.ClozeToArabic,
            source = optionWords,
            random = random,
        ) ?: return@firstNotNullOfOrNull null
        ContextCheckpointQuestion(quizQuestion = quizQuestion, ayah = ayah)
    }
}

internal fun contextCheckpointFeedbackState(
    selectedOptionIndex: Int?,
    correctOptionIndex: Int,
): ContextCheckpointFeedbackState = when (selectedOptionIndex) {
    null -> ContextCheckpointFeedbackState.Unanswered
    correctOptionIndex -> ContextCheckpointFeedbackState.Correct
    else -> ContextCheckpointFeedbackState.Incorrect
}
