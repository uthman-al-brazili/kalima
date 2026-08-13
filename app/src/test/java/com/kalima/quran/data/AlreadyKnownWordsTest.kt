package com.kalima.quran.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AlreadyKnownWordsTest {
    @Test
    fun alreadyKnownStatusTakesPriorityWithoutErasingPreviousProgress() {
        val progress = StudyProgress(
            learnedIds = setOf("word-1"),
            reviewingIds = setOf("word-2"),
            alreadyKnownIds = setOf("word-1", "word-2", "word-3"),
        )

        assertEquals(WordStatus.AlreadyKnown, progress.statusFor("word-1"))
        assertEquals(WordStatus.AlreadyKnown, progress.statusFor("word-2"))
        assertEquals(WordStatus.AlreadyKnown, progress.statusFor("word-3"))
        assertEquals(
            WordStatus.Learned,
            progress.copy(alreadyKnownIds = progress.alreadyKnownIds - "word-1")
                .statusFor("word-1"),
        )
    }
}
