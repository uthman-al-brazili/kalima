package com.kalima.quran.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlphabetReferenceRegressionTest {
    @Test
    fun `alphabet reference has no search and renders rows right to left`() {
        val study = source("ui/StudyScreen.kt")
        val table = study
            .substringAfter("private fun AlphabetReferenceTable")
            .substringBefore("private fun NumberFoundationCard")

        assertFalse(table.contains("OutlinedTextField"))
        assertFalse(table.contains("alphabetReferenceMatching"))
        assertTrue(table.contains("FoundationPronunciationButton"))
        assertTrue(table.contains("pronouncer: ArabicPronouncer"))
        assertTrue(table.contains("R.string.hear_letter_named"))
        assertFalse(resource("values-en/strings.xml").contains("Tap each form’s speaker"))
        assertFalse(resource("values/strings.xml").contains("Toque no alto-falante de cada forma"))
        assertTrue(
            table.contains(
                "CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)",
            ),
        )
    }

    @Test
    fun `foundation voice reapplies Arabic locale before speaking`() {
        val voice = source("audio/ArabicFoundationVoice.kt")
        val play = voice.substringAfter("private fun play")

        assertTrue(play.contains("engine.setLanguage(locale)"))
        assertTrue(play.contains("engine.speak("))
        assertTrue(play.indexOf("engine.setLanguage(locale)") < play.indexOf("engine.speak("))
    }

    private fun source(relative: String): String = sequenceOf(
        File("src/main/java/com/kalima/quran/$relative"),
        File("app/src/main/java/com/kalima/quran/$relative"),
    ).firstOrNull(File::isFile)?.readText()
        ?: error("Android source not found: $relative")

    private fun resource(relative: String): String = sequenceOf(
        File("src/main/res/$relative"),
        File("app/src/main/res/$relative"),
    ).firstOrNull(File::isFile)?.readText()
        ?: error("Android resource not found: $relative")
}
