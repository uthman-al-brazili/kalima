package com.kalima.quran.data

import java.text.Normalizer

data class VerseToken(
    val index: Int,
    val text: String,
    val word: QuranWord?,
)

object VerseExplorer {
    private val arabicMarks = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")

    fun buildTokens(verseArabic: String, candidates: List<QuranWord>): List<VerseToken> {
        val unused = candidates.toMutableList()
        return verseArabic.trim().split(Regex("\\s+")).mapIndexed { index, token ->
            val normalized = normalizeArabic(token)
            val matchIndex = unused.indexOfFirst { candidate ->
                normalizeArabic(candidate.arabic) == normalized
            }
            val match = if (matchIndex >= 0) unused.removeAt(matchIndex) else null
            VerseToken(index, token, match)
        }
    }

    fun normalizeArabic(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(arabicMarks, "")
        .replace('ٱ', 'ا')
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .trim()
}
