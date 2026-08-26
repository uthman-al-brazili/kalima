package com.kalima.quran.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CitationActionsRegressionTest {
    @Test
    fun `share action uses an accessible icon without a visible text label`() {
        val source = source("ui/CitationActions.kt")

        assertTrue(source.contains("IconButton("))
        assertTrue(source.contains("painterResource(R.drawable.ic_share)"))
        assertTrue(
            source.contains("contentDescription = stringResource(R.string.share_citation)"),
        )
        assertFalse(source.contains("Text(stringResource(R.string.share_citation))"))
    }

    private fun source(relative: String): String = sequenceOf(
        File("src/main/java/com/kalima/quran/$relative"),
        File("app/src/main/java/com/kalima/quran/$relative"),
    ).firstOrNull(File::isFile)?.readText()
        ?: error("Android source not found: $relative")
}
