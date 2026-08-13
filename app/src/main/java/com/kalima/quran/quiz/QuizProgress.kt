package com.kalima.quran.quiz

import com.kalima.quran.data.StudyProgress
import java.time.LocalDate

/** Records quiz-only mastery without changing study or spaced-repetition state. */
object QuizProgress {
    fun record(
        progress: StudyProgress,
        wordId: String,
        correct: Boolean,
        date: LocalDate,
    ): StudyProgress = progress.copy(
        quizCorrectAnswers = progress.quizCorrectAnswers + if (correct) 1 else 0,
        quizTotalAnswers = progress.quizTotalAnswers + 1,
        quizCorrectDays = if (correct) {
            progress.quizCorrectDays + (
                wordId to (progress.quizCorrectDays[wordId].orEmpty() + date.toString())
            )
        } else {
            progress.quizCorrectDays
        },
    )
}
