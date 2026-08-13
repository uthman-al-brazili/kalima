package com.kalima.quran.quiz

import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.WordStatus
import kotlin.random.Random

object QuizEngine {
    const val SESSION_SIZE = 5

    private val sessionTypes = listOf(
        QuizQuestionType.ArabicToPortuguese,
        QuizQuestionType.PortugueseToArabic,
        QuizQuestionType.ContextualMeaning,
        QuizQuestionType.ListeningToPortuguese,
        QuizQuestionType.ClozeToArabic,
    )

    private val lockScreenTypes = listOf(
        QuizQuestionType.ArabicToPortuguese,
        QuizQuestionType.ContextualMeaning,
        QuizQuestionType.ArabicToPortuguese,
        QuizQuestionType.PortugueseToArabic,
        QuizQuestionType.ContextualMeaning,
    )

    fun createSession(
        words: List<QuranWord>,
        random: Random = Random.Default,
        optionWords: List<QuranWord> = words,
        mode: QuizMode = QuizMode.Mixed,
    ): List<QuizQuestion> {
        require(words.isNotEmpty()) { "O quiz precisa de palavras" }
        require(optionWords.isNotEmpty()) { "O quiz precisa de alternativas" }
        val targets = words.shuffled(random).take(SESSION_SIZE)
        val types = when (mode) {
            QuizMode.Mixed -> sessionTypes.shuffled(random).take(targets.size)
            QuizMode.Listening -> List(targets.size) { QuizQuestionType.ListeningToPortuguese }
            QuizMode.Cloze -> List(targets.size) { QuizQuestionType.ClozeToArabic }
            QuizMode.Roots -> List(targets.size) { QuizQuestionType.RootToArabic }
        }
        return targets.mapIndexed { index, word ->
            createQuestion(word, types[index], optionWords, random)
        }
    }

    fun createLockScreenQuestion(
        words: List<QuranWord>,
        statusFor: (String) -> WordStatus,
        sequence: Int,
        optionWords: List<QuranWord> = words,
    ): QuizQuestion {
        require(words.isNotEmpty()) { "O quiz precisa de palavras" }
        require(optionWords.isNotEmpty()) { "O quiz precisa de alternativas" }
        val random = Random(sequence * 7_919 + 41)
        val ordered = prioritized(words, statusFor, random)
        val word = ordered[Math.floorMod(sequence, ordered.size)]
        val type = lockScreenTypes[Math.floorMod(sequence, lockScreenTypes.size)]
        return createQuestion(word, type, optionWords, random)
    }

    internal fun createQuestion(
        word: QuranWord,
        type: QuizQuestionType,
        source: List<QuranWord>,
        random: Random,
    ): QuizQuestion {
        val correctAnswer = answerFor(word, type)
        val distractors = linkedSetOf<String>()
        var attempts = 0
        while (distractors.size < QuizQuestion.OPTION_COUNT - 1 && attempts < 100) {
            val candidate = answerFor(source[random.nextInt(source.size)], type)
            if (candidate.isNotBlank() && candidate != correctAnswer) distractors += candidate
            attempts += 1
        }
        if (distractors.size < QuizQuestion.OPTION_COUNT - 1) {
            source.asSequence()
                .map { answerFor(it, type) }
                .filter { it.isNotBlank() && it != correctAnswer }
                .forEach { candidate ->
                    if (distractors.size < QuizQuestion.OPTION_COUNT - 1) distractors += candidate
                }
        }
        require(distractors.size == QuizQuestion.OPTION_COUNT - 1) {
            "Não há alternativas distintas suficientes para este conjunto"
        }
        val options = (distractors.toList() + correctAnswer).shuffled(random)
        return QuizQuestion(
            word = word,
            type = type,
            options = options,
            correctOptionIndex = options.indexOf(correctAnswer),
        )
    }

    private fun answerFor(word: QuranWord, type: QuizQuestionType): String =
        when (type) {
            QuizQuestionType.ArabicToPortuguese,
            QuizQuestionType.ContextualMeaning,
            QuizQuestionType.ListeningToPortuguese,
            -> word.meaning

            QuizQuestionType.PortugueseToArabic,
            QuizQuestionType.ClozeToArabic,
            QuizQuestionType.RootToArabic,
            -> word.arabic
        }

    private fun prioritized(
        words: List<QuranWord>,
        statusFor: (String) -> WordStatus,
        random: Random,
    ): List<QuranWord> = buildList(words.size) {
        addAll(words.filter { statusFor(it.id) == WordStatus.Reviewing }.shuffled(random))
        addAll(words.filter { statusFor(it.id) == WordStatus.New }.shuffled(random))
        addAll(words.filter { statusFor(it.id) == WordStatus.Learned }.shuffled(random))
        addAll(words.filter { statusFor(it.id) == WordStatus.AlreadyKnown }.shuffled(random))
    }
}
