package com.kalima.quran.ui

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kalima.quran.R
import com.kalima.quran.data.SessionLevel
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.UnderstandPathId
import com.kalima.quran.ui.theme.KalimaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun defaultPlanIsVisibleAndStartsAStudySession() {
        var result: OnboardingResult? = null
        composeRule.setContent {
            KalimaTheme {
                OnboardingScreen { scope, path, level ->
                    result = OnboardingResult(scope, path, level)
                }
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.onboarding_path_title)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.path_top_100_desc)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.session_level_quick)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.onboarding_start))
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(StudyScope.Frequent, result?.scope)
            assertNull(result?.path)
            assertEquals(SessionLevel.Steady, result?.level)
        }
    }

    @Test
    fun selectedGuidedPlanAndDepthAreReturned() {
        var result: OnboardingResult? = null
        composeRule.setContent {
            KalimaTheme {
                OnboardingScreen { scope, path, level ->
                    result = OnboardingResult(scope, path, level)
                }
            }
        }

        composeRule.onNode(
            hasText(context.getString(R.string.understand_path_fatihah_title)) and hasClickAction(),
        ).performClick()
        composeRule.onNode(
            hasText(context.getString(R.string.session_level_deep)) and hasClickAction(),
        ).performScrollTo().performClick()
        composeRule.onNodeWithText(context.getString(R.string.onboarding_start))
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(
                OnboardingResult(
                    scope = StudyScope.Prayer,
                    path = UnderstandPathId.AlFatihahSevenDays,
                    level = SessionLevel.Deep,
                ),
                result,
            )
        }
    }

    @Test
    fun lockScreenLearningIsExplainedOnScreen() {
        composeRule.setContent {
            KalimaTheme { OnboardingScreen { _, _, _ -> } }
        }

        composeRule.onNodeWithText(
            context.getString(R.string.onboarding_lock_screen_title),
        ).assertExists()
        composeRule.onNodeWithText(
            context.getString(R.string.onboarding_lock_screen_description),
        ).assertExists()
    }

    private data class OnboardingResult(
        val scope: StudyScope,
        val path: UnderstandPathId?,
        val level: SessionLevel,
    )
}
