package com.kalima.quran.ui

import com.kalima.quran.data.QuranPageToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun `reflows adjacent Mushaf source rows as one readable page section`() {
        val firstRow = token(arabic = "وَإِذْ", wordNumber = 1, lineNumber = 4)
        val secondRow = token(arabic = "قَالَ", wordNumber = 2, lineNumber = 5)

        val sections = quranPageSections(listOf(firstRow, secondRow))

        assertEquals(1, sections.size)
        assertNull(sections.single().startingSurahNumber)
        assertEquals(listOf(firstRow, secondRow), sections.single().tokens)
        assertEquals(
            "وَإِذْ قَالَ",
            quranPageLineContent(sections.single().tokens).text,
        )
    }

    @Test
    fun `starts a new page section where a surah header belongs`() {
        val previousSurah = token(arabic = "قَالَ", wordNumber = 2, lineNumber = 8)
        val newSurahStart = token(
            arabic = "قُلْ",
            wordNumber = 1,
            lineNumber = 10,
            surahNumber = 112,
            ayahNumber = 1,
        )
        val newSurahSecondWord = token(
            arabic = "هُوَ",
            wordNumber = 2,
            lineNumber = 10,
            surahNumber = 112,
            ayahNumber = 1,
        )

        val sections = quranPageSections(
            listOf(previousSurah, newSurahStart, newSurahSecondWord),
        )

        assertEquals(2, sections.size)
        assertNull(sections[0].startingSurahNumber)
        assertEquals(listOf(previousSurah), sections[0].tokens)
        assertEquals(112, sections[1].startingSurahNumber)
        assertEquals(listOf(newSurahStart, newSurahSecondWord), sections[1].tokens)
    }

    private fun QuranPageLineContent.segmentText(index: Int): String =
        segments[index].let { text.substring(it.start, it.endExclusive) }

    private fun token(
        arabic: String,
        wordNumber: Int,
        isAyahMarker: Boolean = false,
        lineNumber: Int = 1,
        surahNumber: Int = 2,
        ayahNumber: Int = 30,
    ) = QuranPageToken(
        pageNumber = 6,
        lineNumber = lineNumber,
        surahNumber = surahNumber,
        ayahNumber = ayahNumber,
        wordNumber = wordNumber,
        arabic = arabic,
        isAyahMarker = isAyahMarker,
    )
}
