package com.kalima.quran.ui

import com.kalima.quran.data.WordRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadAndUnderstandSheetTest {
    @Test
    fun `revealed ayah meaning includes every word in recitation order`() {
        val template = WordRepository.words.first()
        val verseWords = listOf(
            template.copy(id = "one", meaning = "In the name"),
            template.copy(id = "two", meaning = "of Allah"),
            null,
            template.copy(id = "three", meaning = "the Most Merciful"),
        )

        assertEquals(
            "In the name of Allah the Most Merciful",
            buildAyahMeaning(verseWords),
        )
    }
}
