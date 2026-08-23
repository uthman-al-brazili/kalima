package com.kalima.quran.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsAndReaderRegressionTest {
    @Test
    fun `settings expose on demand information and no donation action`() {
        val settings = source("ui/SettingsScreen.kt")
        val activity = source("MainActivity.kt")

        assertTrue(settings.contains("SettingInfoButton"))
        assertTrue(settings.contains("R.string.open_website"))
        assertFalse(settings.contains("onDonate"))
        assertFalse(activity.contains("DONATION_URL"))
        assertTrue(activity.contains("https://kalima-h1f.pages.dev/"))
    }

    @Test
    fun `reader shows pages before preparing optional word lookup index`() {
        val reader = source("ui/QuranReaderScreen.kt")
        val availabilityBlock = reader.substringBefore("// Word lookup is useful")

        assertTrue(availabilityBlock.contains("initializeQuranReader(context)"))
        assertFalse(availabilityBlock.contains("WordRepository.prepareReaderIndex()"))
        assertTrue(reader.contains("WordRepository.prepareReaderIndex()"))
        assertTrue(reader.contains("if (readerIndexReady)"))
    }

    private fun source(relative: String): String = sequenceOf(
        File("src/main/java/com/kalima/quran/$relative"),
        File("app/src/main/java/com/kalima/quran/$relative"),
    ).firstOrNull(File::isFile)?.readText()
        ?: error("Android source not found: $relative")
}
