package com.kalima.quran.widget

import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.WordRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyQuranWordSelectorTest {
    @Test
    fun `first widget card keeps the current study word when it is in scope`() {
        val current = WordRepository.words.first()
        val selected = DailyQuranWordSelector.select(
            StudyProgress(currentStudyWordId = current.id),
            sequence = 0,
        )

        assertEquals(current.id, selected.id)
    }

    @Test
    fun `next card is selected from the local corpus without changing progress`() {
        val progress = StudyProgress()
        val selected = DailyQuranWordSelector.select(progress, sequence = 4)

        assertEquals(WordRepository.wordAtSequence(4).id, selected.id)
        assertEquals(emptySet<String>(), progress.reviewingIds)
        assertEquals(emptySet<String>(), progress.learnedIds)
    }
}
