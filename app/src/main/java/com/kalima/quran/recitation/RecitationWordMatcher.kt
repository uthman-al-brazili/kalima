package com.kalima.quran.recitation

import java.text.Normalizer

/**
 * Aligns a speech recognizer transcript with the known words of one ayah.
 *
 * This deliberately reports word progress only. It is not a pronunciation or Tajweed score.
 */
object RecitationWordMatcher {
    private val arabicMarks = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")
    private val nonLetters = Regex("[^\\p{L}]")

    fun bestMatch(expectedWords: List<String>, transcripts: List<String>): Set<Int> =
        transcripts
            .map { transcript -> match(expectedWords, transcript) }
            .maxByOrNull(Set<Int>::size)
            .orEmpty()

    fun match(expectedWords: List<String>, transcript: String): Set<Int> =
        evaluate(expectedWords, transcript).matchedWordIndexes

    fun evaluate(expectedWords: List<String>, transcript: String): RecitationMatch {
        val expected = expectedWords.map(::normalize)
        val heard = transcript
            .trim()
            .split(Regex("\\s+"))
            .map(::normalize)
            .filter(String::isNotEmpty)
        if (expected.isEmpty() || heard.isEmpty()) return RecitationMatch.Empty

        // Match the target ayah against the end of the transcript. This lets the recognizer
        // discard earlier speech or stale preview text, while requiring the target itself to be
        // contiguous, ordered, and the final thing spoken.
        val matchedPrefixSize = (minOf(expected.size, heard.size) downTo 1)
            .firstOrNull { prefixSize ->
                val heardStart = heard.size - prefixSize
                (0 until prefixSize).all { index ->
                    wordsMatch(expected[index], heard[heardStart + index])
                }
            }
            ?: 0
        val isComplete = matchedPrefixSize == expected.size
        return RecitationMatch(
            matchedWordIndexes = (0 until matchedPrefixSize).toSet(),
            isComplete = isComplete,
        )
    }

    internal fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(arabicMarks, "")
            .replace('ٱ', 'ا')
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            .replace("ـ", "")
            .replace(nonLetters, "")
            .lowercase()

    private fun wordsMatch(expected: String, heard: String): Boolean {
        if (expected == heard) return true
        if (expected.length < 5 || heard.length < 5) return false
        if (kotlin.math.abs(expected.length - heard.length) > 1) return false
        return editDistanceAtMostOne(expected, heard)
    }

    private fun editDistanceAtMostOne(first: String, second: String): Boolean {
        var firstIndex = 0
        var secondIndex = 0
        var edits = 0
        while (firstIndex < first.length && secondIndex < second.length) {
            if (first[firstIndex] == second[secondIndex]) {
                firstIndex += 1
                secondIndex += 1
                continue
            }
            edits += 1
            if (edits > 1) return false
            when {
                first.length > second.length -> firstIndex += 1
                second.length > first.length -> secondIndex += 1
                else -> {
                    firstIndex += 1
                    secondIndex += 1
                }
            }
        }
        if (firstIndex < first.length || secondIndex < second.length) edits += 1
        return edits <= 1
    }
}

data class RecitationMatch(
    val matchedWordIndexes: Set<Int>,
    val isComplete: Boolean,
) {
    companion object {
        val Empty = RecitationMatch(emptySet(), isComplete = false)
    }
}
