package com.kalima.quran.widget

import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.StudyPlan
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.WordRepository
import com.kalima.quran.data.limitNewWords

/** Selects only from the learner's configured study corpus when possible. */
internal object DailyQuranWordSelector {
    fun select(progress: StudyProgress, sequence: Int): QuranWord {
        val activeWords = progress.limitNewWords(
            StudyPlan.calculate(progress, WordRepository.words).combinedWords,
        )
        val source = activeWords.ifEmpty { WordRepository.words }
        val currentWord = WordRepository.wordById(progress.currentStudyWordId)
        return if (sequence == 0 && currentWord != null && currentWord in source) {
            currentWord
        } else {
            WordRepository.wordAtSequence(sequence, source)
        }
    }
}
