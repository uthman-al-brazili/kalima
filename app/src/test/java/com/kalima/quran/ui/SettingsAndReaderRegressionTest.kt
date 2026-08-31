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

        assertTrue(availabilityBlock.contains("preloadQuranFirstPage(context)"))
        assertTrue(availabilityBlock.contains("initializeQuranReader(context)"))
        assertTrue(availabilityBlock.contains("initialValue = QuranReaderRepository.isInitialized"))
        assertFalse(availabilityBlock.contains("WordRepository.prepareReaderIndex()"))
        assertTrue(reader.contains("WordRepository.prepareReaderIndex()"))
        assertTrue(reader.contains("if (readerIndexReady)"))
        assertTrue(reader.contains("key(pageIndex, readerIndexReady)"))
    }

    @Test
    fun `all main tabs keep surrounding controls compact`() {
        val app = source("ui/KalimaApp.kt")
        val reader = source("ui/QuranReaderScreen.kt")
        val study = source("ui/StudyScreen.kt")
        val mission = source("ui/StudyMissionScreen.kt")
        val learn = source("ui/LearnScreen.kt")
        val progress = source("ui/ProgressScreen.kt")
        val components = source("ui/Components.kt")

        assertTrue(app.contains("if (settingsVisible)"))
        assertFalse(app.contains("selected != AppTab.Quran"))
        assertFalse(app.contains("text = stringResource(R.string.app_name)"))
        assertTrue(app.contains("Modifier.height(64.dp)"))
        assertFalse(app.contains("quranReadingMode"))
        assertTrue(reader.contains("onOpenSettings"))
        assertTrue(study.contains("onOpenSettings"))
        assertTrue(mission.contains("TabSettingsButton("))
        assertTrue(learn.contains("TabSettingsButton("))
        assertTrue(progress.contains("TabSettingsButton("))
        assertTrue(components.contains("internal fun TabSettingsButton"))
        assertTrue(components.contains("internal fun Modifier.alignTabSettingsButton"))
        assertTrue(reader.contains("pageSelectionEnabled"))
        assertFalse(reader.contains("ic_fullscreen"))
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

    @Test
    fun `word explorer removes study buttons but keeps occurrence marking`() {
        val reader = source("ui/QuranReaderScreen.kt")
        val versePanel = source("ui/VerseExplorerPanel.kt")

        assertFalse(reader.contains("onOpenOccurrence"))
        assertFalse(versePanel.contains("open_word_for_study"))
        assertFalse(versePanel.contains("add_to_study"))
        assertTrue(versePanel.contains("onToggleCustomList"))
        assertTrue(versePanel.contains("VerseExcerptBuilder.findRange(occurrence.verseArabic, occurrence.arabic)"))
        assertTrue(versePanel.contains("background = highlightBackground"))
    }

    private fun source(relative: String): String = sequenceOf(
        File("src/main/java/com/kalima/quran/$relative"),
        File("app/src/main/java/com/kalima/quran/$relative"),
    ).firstOrNull(File::isFile)?.readText()
        ?: error("Android source not found: $relative")
}
