package com.kalima.quran.quiz

import com.kalima.quran.data.WordRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuizEngineTest {
    private val words = WordRepository.words.take(80)

    @Test
    fun sessionUsesApprovedQuestionDistribution() {
        val session = QuizEngine.createSession(words, random = Random(17))

        assertEquals(5, session.size)
        assertEquals(1, session.count { it.type == QuizQuestionType.ArabicToPortuguese })
        assertEquals(1, session.count { it.type == QuizQuestionType.PortugueseToArabic })
        assertEquals(1, session.count { it.type == QuizQuestionType.ContextualMeaning })
        assertEquals(1, session.count { it.type == QuizQuestionType.ListeningToPortuguese })
        assertEquals(1, session.count { it.type == QuizQuestionType.ClozeToArabic })
    }

    @Test
    fun everyQuestionHasFourDistinctOptionsAndOneCorrectAnswer() {
        val session = QuizEngine.createSession(words, random = Random(29))

        session.forEach { question ->
            assertEquals(4, question.options.size)
            assertEquals(4, question.options.toSet().size)
            assertTrue(question.correctOptionIndex in question.options.indices)
            assertEquals(question.correctAnswer, question.options[question.correctOptionIndex])
        }
    }

    @Test
    fun sessionSelectionIsStableForTheSameRandomSeed() {
        val first = QuizEngine.createSession(words = words, random = Random(41))
        val second = QuizEngine.createSession(words = words, random = Random(41))

        assertEquals(first.map { it.word.id }, second.map { it.word.id })
    }

    @Test
    fun optionPoolDoesNotIntroduceAdditionalQuizTargets() {
        val allowedTarget = words.first()

        val session = QuizEngine.createSession(
            words = listOf(allowedTarget),
            random = Random(53),
            optionWords = words,
        )

        assertTrue(session.all { it.word.id == allowedTarget.id })
        assertTrue(session.all { it.options.size == QuizQuestion.OPTION_COUNT })
    }

    @Test
    fun sessionDoesNotRepeatTargetsWhenFewerThanFiveAreDue() {
        val targets = words.take(2)

        val session = QuizEngine.createSession(
            words = targets,
            random = Random(61),
            optionWords = words,
        )

        assertEquals(2, session.size)
        assertEquals(targets.map { it.id }.toSet(), session.map { it.word.id }.toSet())
    }

    @Test
    fun focusedModesUseOnlyTheirRequestedQuestionType() {
        val modes = mapOf(
            QuizMode.Listening to QuizQuestionType.ListeningToPortuguese,
            QuizMode.Cloze to QuizQuestionType.ClozeToArabic,
            QuizMode.Roots to QuizQuestionType.RootToArabic,
        )

        modes.forEach { (mode, expectedType) ->
            val session = QuizEngine.createSession(
                words = words,
                random = Random(71),
                mode = mode,
            )
            assertTrue(session.all { it.type == expectedType })
        }
    }
}
