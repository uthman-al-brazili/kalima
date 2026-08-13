package com.kalima.quran.quiz

import com.kalima.quran.data.QuranWord

enum class QuizQuestionType {
    ArabicToPortuguese,
    PortugueseToArabic,
    ContextualMeaning,
    ListeningToPortuguese,
    ClozeToArabic,
    RootToArabic,
}

enum class QuizMode {
    Mixed,
    Listening,
    Cloze,
    Roots,
}

data class QuizQuestion(
    val word: QuranWord,
    val type: QuizQuestionType,
    val options: List<String>,
    val correctOptionIndex: Int,
) {
    init {
        require(options.size == OPTION_COUNT)
        require(correctOptionIndex in options.indices)
    }

    val correctAnswer: String get() = options[correctOptionIndex]

    companion object {
        const val OPTION_COUNT = 4
    }
}

sealed interface LockScreenContent {
    data class WordCard(val word: QuranWord) : LockScreenContent
    data class QuizCard(val question: QuizQuestion) : LockScreenContent
}
