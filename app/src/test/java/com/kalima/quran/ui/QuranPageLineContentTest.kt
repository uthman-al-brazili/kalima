package com.kalima.quran.ui

import com.kalima.quran.data.QuranPageToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class QuranPageLineContentTest {
    @Test
    fun `keeps a Mushaf source line in one continuous text run`() {
        val first = token(arabic = "وَإِذْ", wordNumber = 1)
        val second = token(arabic = "قَالَ", wordNumber = 2)
        val marker = token(arabic = "٣٠", wordNumber = 3, isAyahMarker = true)

        val content = quranPageLineContent(listOf(first, second, marker))

        assertEquals("وَإِذْ قَالَ ۝٣٠", content.text)
        assertEquals(listOf(first, second), content.words)
        assertEquals("وَإِذْ", content.segmentText(0))
        assertEquals("قَالَ", content.segmentText(1))
        assertEquals("۝٣٠", content.segmentText(2))
        assertFalse(content.text.contains('\n'))
    }

    private fun QuranPageLineContent.segmentText(index: Int): String =
        segments[index].let { text.substring(it.start, it.endExclusive) }

    private fun token(
        arabic: String,
        wordNumber: Int,
        isAyahMarker: Boolean = false,
    ) = QuranPageToken(
        pageNumber = 6,
        lineNumber = 1,
        surahNumber = 2,
        ayahNumber = 30,
        wordNumber = wordNumber,
        arabic = arabic,
        isAyahMarker = isAyahMarker,
    )
}
