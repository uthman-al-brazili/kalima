package com.kalima.quran.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlphabetReferenceRegressionTest {
    @Test
    fun `Arabic alphabet answers leave room for full glyph metrics`() {
        val study = source("ui/AlphabetScreen.kt")
        val options = study
            .substringAfter("question.options.forEachIndexed")
            .substringBefore("if (selectedOptionIndex != null)")

        assertTrue(options.contains("height(if (option.isArabic) 64.dp else 48.dp)"))
    }

    @Test
    fun `alphabet answer is final after the first choice`() {
        val study = source("ui/AlphabetScreen.kt")
        val options = study
            .substringAfter("question.options.forEachIndexed")
            .substringBefore("TextButton(\n                        onClick = {\n                            recalling = false")

        assertTrue(options.contains("if (!answered)"))
        assertTrue(options.contains("enabled = !answered"))
        assertTrue(options.contains("val correct = answered && optionIndex == question.correctOptionIndex"))
        assertTrue(options.contains("if (answered)"))
        assertFalse(options.contains("if (!answeredCorrectly)"))
    }

    @Test
    fun `vowelled alphabet prompt centers kasra independently of font anchor`() {
        val study = source("ui/AlphabetMilestone.kt")
        val prompt = study
            .substringAfter("internal fun AlphabetPromptArabicText")
            .substringBefore("internal fun AlphabetDecodingMilestone")

        assertTrue(prompt.contains("text.endsWith(ARABIC_KASRA)"))
        assertTrue(prompt.contains("val centerX = this.size.width / 2f"))
        assertTrue(prompt.contains("val centerY = this.size.height * 0.92f"))
        assertTrue(prompt.contains("drawLine("))
    }

    @Test
    fun `alphabet reference has no search and renders rows right to left`() {
        val study = source("ui/AlphabetOverviewComponents.kt")
        val table = study
            .substringAfter("internal fun AlphabetReferenceTable")
            .substringBefore("private fun NumberFoundationCard")

        assertFalse(table.contains("OutlinedTextField"))
        assertFalse(table.contains("alphabetReferenceMatching"))
        assertFalse(table.contains("FoundationPronunciationButton"))
        assertFalse(table.contains("pronouncer"))
        assertFalse(resource("values-en/strings.xml").contains("Tap each form’s speaker"))
        assertFalse(resource("values/strings.xml").contains("Toque no alto-falante de cada forma"))
        assertTrue(
            table.contains(
                "CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)",
            ),
        )
    }

    @Test
    fun `letter lesson keeps study controls in the first viewport`() {
        val study = source("ui/AlphabetScreen.kt")
        val lesson = study
            .substringAfter("private fun AlphabetFoundationScreen")
            .substringBefore("private fun AlphabetPromptArabicText")

        assertTrue(lesson.contains("R.string.open_alphabet_table_short"))
        assertTrue(lesson.contains("LaunchedEffect(recalling, decodingMilestone, symbolIndex)"))
        assertTrue(lesson.contains("size = 72"))
        assertFalse(lesson.contains("stringResource(R.string.alphabet_letters_note)"))
        assertFalse(lesson.contains("stringResource(R.string.letter_audio_matches_name)"))
    }

    @Test
    fun `number lessons are not shown inside the alphabet course`() {
        val study = source("ui/AlphabetScreen.kt")
        val lesson = study
            .substringAfter("private fun AlphabetFoundationScreen")
            .substringBefore("private fun AlphabetPromptArabicText")

        assertFalse(lesson.contains("NumberFoundationCard"))
        assertFalse(lesson.contains("onCompleteNumberLesson"))
    }

    @Test
    fun `alphabet and number tabs keep their study content separate`() {
        val alphabet = source("ui/AlphabetScreen.kt")
        val numbers = source("ui/NumberScreen.kt")
        val table = source("ui/AlphabetOverviewComponents.kt")
            .substringAfter("private fun AlphabetReferenceTable")

        assertTrue(alphabet.contains("AlphabetReferenceTable"))
        assertFalse(alphabet.contains("NumberFoundationCard"))
        assertTrue(numbers.contains("NumberFoundationCard"))
        assertFalse(numbers.contains("AlphabetReferenceTable"))
        assertTrue(table.contains("R.string.alphabet_reference_compact_hint"))
        assertTrue(table.contains("size = 28"))
        assertTrue(table.contains("size = 24"))
    }

    @Test
    fun `alphabet overview does not repeat the selected tab label`() {
        val alphabet = source("ui/AlphabetScreen.kt")
        val accessCard = source("ui/AlphabetOverviewComponents.kt")
            .substringAfter("internal fun AlphabetAccessCard")

        assertFalse(alphabet.contains("R.string.alphabet_shortcut_title"))
        assertTrue(accessCard.contains("R.string.foundation_course_title"))
        assertFalse(accessCard.contains("R.string.alphabet_shortcut_title"))
    }

    @Test
    fun `numbers overview does not repeat the selected tab label`() {
        val numbers = source("ui/NumberScreen.kt")
            .substringAfter("fun NumberScreen")
        val lessonCard = source("ui/NumberScreen.kt")
            .substringAfter("private fun NumberFoundationCard")
            .substringBefore("private fun NumberAccessCard")
        val accessCard = source("ui/NumberScreen.kt")
            .substringAfter("private fun NumberAccessCard")

        assertFalse(numbers.contains("R.string.numbers_shortcut_title"))
        assertFalse(lessonCard.contains("R.string.number_course_title"))
        assertFalse(lessonCard.contains("R.string.hear_number"))
        assertFalse(lessonCard.contains("R.string.next_number"))
        assertFalse(lessonCard.contains("R.string.finish_number_course"))
        assertTrue(lessonCard.contains("R.string.listen_pronunciation"))
        assertTrue(lessonCard.contains("R.string.continue_action"))
        assertTrue(accessCard.contains("R.string.foundation_course_title"))
        assertFalse(accessCard.contains("R.string.numbers_shortcut_title"))
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
