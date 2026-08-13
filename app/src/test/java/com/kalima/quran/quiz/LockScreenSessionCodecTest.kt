package com.kalima.quran.quiz

import com.kalima.quran.data.QuranWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LockScreenSessionCodecTest {
    private val word = QuranWord(
        id = "word-1",
        arabic = "كِتَاب",
        lemma = "كتاب",
        transliteration = "kitāb",
        meaning = "book",
        root = "ك ت ب",
        grammar = "noun",
        category = "test",
        reference = "Quran 2:2",
        verseArabic = "ذَٰلِكَ ٱلْكِتَٰبُ",
        verseMeaning = "That is the Book",
        insight = "test",
    )

    @Test
    fun quizRoundTripsWithoutLosingArabicOptions() {
        val question = QuizQuestion(
            word,
            QuizQuestionType.PortugueseToArabic,
            listOf("كِتَاب", "قَلَم", "عِلْم", "نُور"),
            0,
        )
        val session = LockScreenSession("session-1", LockScreenContent.QuizCard(question), true)
        assertEquals(
            session,
            LockScreenSessionCodec.decode(LockScreenSessionCodec.encode(session)) {
                if (it == word.id) word else null
            },
        )
    }

    @Test
    fun corruptOrUnknownSessionsAreRejected() {
        assertNull(LockScreenSessionCodec.decode("broken") { word })
        assertNull(
            LockScreenSessionCodec.decode(
                LockScreenSessionCodec.encode(
                    LockScreenSession("session-2", LockScreenContent.WordCard(word)),
                ),
            ) { null },
        )
    }
}
