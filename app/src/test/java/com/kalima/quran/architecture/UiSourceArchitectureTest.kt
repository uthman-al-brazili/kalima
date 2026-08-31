package com.kalima.quran.architecture

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source-layout checks belong here; user-visible behavior belongs in androidTest. */
class UiSourceArchitectureTest {
    @Test
    fun `Kalima app boundary stays grouped by state and actions`() {
        val source = uiFile("KalimaApp.kt").readText()
        val parameters = source
            .substringAfter("fun KalimaApp(")
            .substringBefore(") {")
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()

        assertEquals(listOf("state: KalimaUiState,", "actions: KalimaAppActions,"), parameters)
    }

    @Test
    fun `Android UI source files stay below the extraction threshold`() {
        val oversized = requireNotNull(uiDirectory().listFiles())
            .filter { it.extension == "kt" }
            .map { it.name to it.readLines().size }
            .filter { (_, lines) -> lines > MAX_UI_FILE_LINES }

        assertTrue("UI files over $MAX_UI_FILE_LINES lines: $oversized", oversized.isEmpty())
    }

    private fun uiFile(name: String): File = File(uiDirectory(), name)

    private fun uiDirectory(): File = sequenceOf(
        File("src/main/java/com/kalima/quran/ui"),
        File("app/src/main/java/com/kalima/quran/ui"),
    ).firstOrNull(File::isDirectory)
        ?: error("Android UI source directory not found")

    private companion object {
        const val MAX_UI_FILE_LINES = 900
    }
}
