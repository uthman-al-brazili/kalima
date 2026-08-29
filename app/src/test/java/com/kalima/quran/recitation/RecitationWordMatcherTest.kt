package com.kalima.quran.recitation

import java.nio.FloatBuffer
import org.junit.Assert.assertEquals
import org.junit.Test

class RecitationWordMatcherTest {
    @Test
    fun `matches speech text without Quran marks or alif variants`() {
        val expected = listOf("إِيَّاكَ", "نَعْبُدُ", "وَإِيَّاكَ", "نَسْتَعِينُ")

        assertEquals(
            setOf(0, 1, 2, 3),
            RecitationWordMatcher.match(expected, "اياك نعبد واياك نستعين"),
        )
    }

    @Test
    fun `rejects extra speech instead of skipping over it`() {
        val expected = listOf("الْحَمْدُ", "لِلَّهِ", "رَبِّ", "الْعَالَمِينَ")

        assertEquals(
            emptySet<Int>(),
            RecitationWordMatcher.match(expected, "بسم الله الحمد لله العالمين"),
        )
    }

    @Test
    fun `accepts a complete ordered ayah at the end of earlier speech`() {
        val expected = listOf("بِسْمِ", "اللَّهِ", "الرَّحْمَنِ", "الرَّحِيمِ")
        val result = RecitationWordMatcher.evaluate(
            expected,
            "الحمد لله رب العالمين بسم الله الرحمن الرحيم",
        )

        assertEquals(true, result.isComplete)
        assertEquals(setOf(0, 1, 2, 3), result.matchedWordIndexes)
    }

    @Test
    fun `highlights an ordered ayah prefix at the end of earlier speech`() {
        val expected = listOf("بِسْمِ", "اللَّهِ", "الرَّحْمَنِ", "الرَّحِيمِ")
        val result = RecitationWordMatcher.evaluate(
            expected,
            "الحمد لله رب العالمين بسم الله",
        )

        assertEquals(false, result.isComplete)
        assertEquals(setOf(0, 1), result.matchedWordIndexes)
    }

    @Test
    fun `rejects a word inserted inside the target ayah`() {
        val expected = listOf("بِسْمِ", "اللَّهِ", "الرَّحْمَنِ", "الرَّحِيمِ")
        val result = RecitationWordMatcher.evaluate(
            expected,
            "بسم الله الحمد الرحمن الرحيم",
        )

        assertEquals(false, result.isComplete)
    }

    @Test
    fun `rejects a complete ayah followed by extra words`() {
        val expected = listOf("بِسْمِ", "اللَّهِ", "الرَّحْمَنِ", "الرَّحِيمِ")
        val result = RecitationWordMatcher.evaluate(
            expected,
            "بسم الله الرحمن الرحيم الحمد لله",
        )

        assertEquals(false, result.isComplete)
        assertEquals(emptySet<Int>(), result.matchedWordIndexes)
    }

    @Test
    fun `rejects words recited out of ayah order`() {
        val expected = listOf("بِسْمِ", "اللَّهِ", "الرَّحْمَنِ", "الرَّحِيمِ")
        val result = RecitationWordMatcher.evaluate(
            expected,
            "الرحيم الرحمن الله بسم",
        )

        assertEquals(false, result.isComplete)
        assertEquals(setOf(0), result.matchedWordIndexes)
    }

    @Test
    fun `does not treat a different short word as recognized`() {
        assertEquals(
            emptySet<Int>(),
            RecitationWordMatcher.match(listOf("مِنْ"), "منه"),
        )
    }

    @Test
    fun `uses Tilawa Arabic normalization for final ta marbuta`() {
        assertEquals(
            setOf(0),
            RecitationWordMatcher.match(listOf("رَحْمَةٌ"), "رحمه"),
        )
    }

    @Test
    fun `selects the recognizer candidate with the most expected words`() {
        val expected = listOf("قُلْ", "هُوَ", "اللَّهُ", "أَحَدٌ")

        assertEquals(
            setOf(0, 1, 2, 3),
            RecitationWordMatcher.bestMatch(
                expected,
                listOf("كل هو", "قل هو الله احد"),
            ),
        )
    }

    @Test
    fun `Tilawa decoder collapses repeated CTC tokens and replaces word boundaries`() {
        val decoder = TilawaCtcDecoder(
            vocab = mapOf(0 to "<unk>", 1 to "▁قل", 2 to "▁هو", 3 to "<blank>"),
            blankTokenId = 3,
        )
        val frames = floatArrayOf(
            0f, 5f, 0f, 0f,
            0f, 5f, 0f, 0f,
            0f, 0f, 0f, 5f,
            0f, 0f, 5f, 0f,
        )

        assertEquals("قل هو", decoder.decode(FloatBuffer.wrap(frames), 4, 4))
    }
}
