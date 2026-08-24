package com.kalima.quran.widget

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyQuranWordWidgetLayoutTest {
    @Test
    fun `widget centers its learning content and keeps next as a separate action`() {
        val layout = source("res/layout/widget_daily_quran_word.xml")

        assertTrue(layout.contains("<FrameLayout"))
        assertTrue(layout.contains("android:id=\"@+id/widget_open_area\""))
        assertTrue(layout.contains("android:gravity=\"center\""))
        assertTrue(layout.contains("android:textAlignment=\"center\""))
        assertTrue(layout.contains("android:id=\"@+id/widget_next\""))
        assertTrue(layout.contains("android:layout_gravity=\"end|bottom\""))
    }

    @Test
    fun `widget background has gradient depth and subtle decorative rings`() {
        val background = source("res/drawable/widget_daily_word_background.xml")

        assertTrue(background.contains("<layer-list"))
        assertTrue(background.contains("<gradient"))
        assertTrue(background.contains("widget_daily_word_pattern_gold"))
        assertTrue(background.contains("widget_daily_word_pattern_mint"))
    }

    @Test
    fun `widget click bindings preserve exact word routing and independent next action`() {
        val provider = source("java/com/kalima/quran/widget/DailyQuranWordWidgetProvider.kt")

        assertTrue(provider.contains("setOnClickPendingIntent(R.id.widget_open_area"))
        assertTrue(provider.contains("openWordIntent(localized, appWidgetId, word.id)"))
        assertTrue(provider.contains("setOnClickPendingIntent(R.id.widget_next"))
        assertTrue(provider.contains("nextIntent(localized, appWidgetId)"))
    }

    private fun source(relative: String): String = sequenceOf(
        File("app/src/main/$relative"),
        File("src/main/$relative"),
    ).firstOrNull(File::isFile)?.readText()
        ?: error("Android source not found: $relative")
}
