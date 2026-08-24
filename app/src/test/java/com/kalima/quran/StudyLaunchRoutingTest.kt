package com.kalima.quran

import com.kalima.quran.data.WordRepository
import com.kalima.quran.ui.StudyLaunchTarget
import com.kalima.quran.ui.displayedStudyWordId
import com.kalima.quran.ui.studyQueueSourceWords
import org.junit.Assert.assertEquals
import org.junit.Test

class StudyLaunchRoutingTest {
    @Test
    fun `every delivered widget click receives a fresh request id`() {
        assertEquals(101L, nextStudyLaunchRequestId(previousId = 100L, nowNanos = 100L))
        assertEquals(250L, nextStudyLaunchRequestId(previousId = 101L, nowNanos = 250L))
    }

    @Test
    fun `widget word overrides an older restored study word`() {
        val target = StudyLaunchTarget(wordId = "widget-word", requestId = 42L)

        assertEquals(
            "widget-word",
            displayedStudyWordId(savedWordId = "restored-word", launchTarget = target),
        )
    }

    @Test
    fun `saved word remains when there is no widget request`() {
        assertEquals(
            "restored-word",
            displayedStudyWordId(savedWordId = "restored-word", launchTarget = null),
        )
    }

    @Test
    fun `widget word remains available when the normal study queue is empty`() {
        val requested = WordRepository.words.first()

        assertEquals(
            listOf(requested),
            studyQueueSourceWords(availableWords = emptyList(), requestedWord = requested),
        )
    }
}
