package com.kalima.quran.quiz

import com.kalima.quran.data.QuranWord

data class VerseExcerpt(
    val text: String,
    val highlightStart: Int,
    val highlightEnd: Int,
) {
    val hasHighlight: Boolean get() = highlightStart >= 0 && highlightEnd > highlightStart
}

object VerseExcerptBuilder {
    private const val DEFAULT_MAX_CHARS = 180

    fun build(word: QuranWord, maxChars: Int = DEFAULT_MAX_CHARS): VerseExcerpt {
        val verse = word.verseArabic
        val target = findRange(verse, word.arabic) ?: findRange(verse, word.lemma)
        if (verse.length <= maxChars) {
            return VerseExcerpt(verse, target?.first ?: -1, target?.last?.plus(1) ?: -1)
        }

        val targetStart = target?.first ?: 0
        val targetEnd = target?.last?.plus(1) ?: 0
        var start = (targetStart - maxChars / 2).coerceIn(0, verse.length - maxChars)
        var end = (start + maxChars).coerceAtMost(verse.length)
        if (start > 0) {
            start = verse.lastIndexOf(' ', start).takeIf { it >= 0 }?.plus(1) ?: start
        }
        if (end < verse.length) {
            end = verse.indexOf(' ', end).takeIf { it >= 0 } ?: end
        }
        if (targetEnd > end) end = targetEnd.coerceAtMost(verse.length)

        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < verse.length) "…" else ""
        val excerpt = prefix + verse.substring(start, end) + suffix
        val highlightStart = target?.let { prefix.length + targetStart - start } ?: -1
        val highlightEnd = target?.let { prefix.length + targetEnd - start } ?: -1
        return VerseExcerpt(excerpt, highlightStart, highlightEnd)
    }

    fun buildCloze(word: QuranWord, maxChars: Int = DEFAULT_MAX_CHARS): String {
        val excerpt = build(word, maxChars)
        if (!excerpt.hasHighlight) return excerpt.text
        return excerpt.text.replaceRange(excerpt.highlightStart, excerpt.highlightEnd, "____")
    }

    internal fun findRange(text: String, target: String): IntRange? {
        val normalizedText = normalizeWithIndexes(text)
        val normalizedTarget = normalizeWithIndexes(target).value
        if (normalizedTarget.isEmpty()) return null
        val normalizedStart = normalizedText.value.indexOf(normalizedTarget)
        if (normalizedStart < 0) return null
        val normalizedEnd = normalizedStart + normalizedTarget.length - 1
        return normalizedText.originalIndexes[normalizedStart]..normalizedText.originalIndexes[normalizedEnd]
    }

    private fun normalizeWithIndexes(value: String): NormalizedText {
        val normalized = StringBuilder(value.length)
        val indexes = ArrayList<Int>(value.length)
        value.forEachIndexed { index, character ->
            val mapped = when {
                character == 'ـ' || character.isArabicMark() -> null
                character in "أإآٱ" -> 'ا'
                character == 'ى' -> 'ي'
                else -> character
            }
            if (mapped != null) {
                normalized.append(mapped)
                indexes += index
            }
        }
        return NormalizedText(normalized.toString(), indexes)
    }

    private fun Char.isArabicMark(): Boolean = when (Character.getType(this)) {
        Character.NON_SPACING_MARK.toInt(),
        Character.COMBINING_SPACING_MARK.toInt(),
        Character.ENCLOSING_MARK.toInt(),
        -> true

        else -> false
    }

    private data class NormalizedText(
        val value: String,
        val originalIndexes: List<Int>,
    )
}
