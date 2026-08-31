package com.kalima.quran.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kalima.quran.R
import com.kalima.quran.audio.ArabicPronouncer
import com.kalima.quran.data.QuranWord
import com.kalima.quran.quiz.QuizQuestion
import com.kalima.quran.quiz.QuizQuestionType
import com.kalima.quran.quiz.VerseExcerpt
import com.kalima.quran.ui.theme.KalimaTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContextCheckpointScreenUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val pronouncer by lazy { ArabicPronouncer(context) }

    @After
    fun shutDownPronouncer() {
        pronouncer.shutdown()
    }

    @Test
    fun correctAnswerShowsFeedbackAndContinues() {
        val answers = mutableListOf<Pair<String, Boolean>>()
        var continues = 0
        val question = checkpointQuestion(correctOptionIndex = 1)
        composeRule.setContent {
            KalimaTheme {
                ContextCheckpointScreen(
                    question = question,
                    pronouncer = pronouncer,
                    onAnswer = { id, correct -> answers += id to correct },
                    onContinue = { continues += 1 },
                    onOpenSettings = {},
                )
            }
        }

        clickOption(question, 1)

        composeRule.onNodeWithText(context.getString(R.string.checkpoint_correct)).assertExists()
        assertOptionsAreGone(question)
        composeRule.onNodeWithText(context.getString(R.string.continue_action))
            .performScrollTo()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(question.word.id to true), answers)
            assertEquals(1, continues)
        }
    }

    @Test
    fun incorrectAnswerShowsCorrectionAndReportsFailure() {
        val answers = mutableListOf<Pair<String, Boolean>>()
        val question = checkpointQuestion(correctOptionIndex = 2)
        composeRule.setContent {
            KalimaTheme {
                ContextCheckpointScreen(
                    question = question,
                    pronouncer = pronouncer,
                    onAnswer = { id, correct -> answers += id to correct },
                    onContinue = {},
                    onOpenSettings = {},
                )
            }
        }

        clickOption(question, 0)

        composeRule.onNodeWithText(context.getString(R.string.checkpoint_incorrect)).assertExists()
        assertOptionsAreGone(question)
        composeRule.runOnIdle {
            assertEquals(listOf(question.word.id to false), answers)
        }
    }

    private fun clickOption(question: ContextCheckpointQuestion, index: Int) {
        composeRule.onNodeWithContentDescription(optionDescription(question, index))
            .performScrollTo()
            .performClick()
    }

    private fun assertOptionsAreGone(question: ContextCheckpointQuestion) {
        question.options.indices.forEach { index ->
            composeRule.onNodeWithContentDescription(optionDescription(question, index))
                .assertDoesNotExist()
        }
    }

    private fun optionDescription(question: ContextCheckpointQuestion, index: Int): String =
        context.getString(
            R.string.checkpoint_choice_description,
            index + 1,
            question.options[index],
        )

    private fun checkpointQuestion(correctOptionIndex: Int): ContextCheckpointQuestion {
        val word = QuranWord(
            id = "mercy",
            arabic = "رَحْمَة",
            lemma = "رَحْمَة",
            transliteration = "rahmah",
            meaning = "mercy",
            root = "ر ح م",
            grammar = "noun",
            category = "test",
            reference = "Test 1:1",
            verseArabic = "رَحْمَة فِي آيَة",
            verseMeaning = "Mercy in a test ayah.",
            insight = "",
        )
        val options = listOf("guidance", "mercy", "light", "truth")
        return ContextCheckpointQuestion(
            quizQuestion = QuizQuestion(
                word = word,
                type = QuizQuestionType.ContextualMeaning,
                options = options,
                correctOptionIndex = correctOptionIndex,
            ),
            ayah = VerseExcerpt(
                text = word.verseArabic,
                highlightStart = 0,
                highlightEnd = word.arabic.length,
            ),
        )
    }
}
