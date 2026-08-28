package com.kalima.quran.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class CitationActionsRegressionTest {
    @Test
    fun `app exposes no share controls`() {
        val uiSource = sourceDirectory().walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n", transform = File::readText)

        assertFalse(uiSource.contains("Intent.ACTION_SEND"))
        assertFalse(uiSource.contains("CitationActions("))
        assertFalse(uiSource.contains("R.drawable.ic_share"))
    }

    private fun sourceDirectory(): File = sequenceOf(
        File("src/main/java/com/kalima/quran/ui"),
        File("app/src/main/java/com/kalima/quran/ui"),
    ).firstOrNull(File::isDirectory)
        ?: error("Android UI source directory not found")
}
