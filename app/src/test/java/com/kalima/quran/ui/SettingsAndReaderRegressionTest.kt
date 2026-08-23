package com.kalima.quran.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsAndReaderRegressionTest {
    @Test
    fun `settings keep descriptions inline and no donation action`() {
        val settings = source("ui/SettingsScreen.kt")
        val activity = source("MainActivity.kt")

        assertFalse(settings.contains("SettingInfoButton"))
        assertFalse(settings.contains("visibleSettingInfo"))
        assertFalse(settings.contains("showSettingInfo"))
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

    @Test
    fun `hussary recitation keeps static verse styling without proportional timing`() {
        val player = source("audio/HussaryVerseAudioPlayer.kt")
        val versePanel = source("ui/VerseExplorerPanel.kt")

        assertFalse(player.contains("onProgress"))
        assertFalse(player.contains("PROGRESS_INTERVAL_MS"))
        assertFalse(versePanel.contains("verseWordIndexAt"))
        assertFalse(versePanel.contains("highlightedTokenIndex"))
        assertFalse(versePanel.contains("RecitableVerseExplorer"))
    }

    private fun source(relative: String): String = sequenceOf(
        File("src/main/java/com/kalima/quran/$relative"),
        File("app/src/main/java/com/kalima/quran/$relative"),
    ).firstOrNull(File::isFile)?.readText()
        ?: error("Android source not found: $relative")
}
