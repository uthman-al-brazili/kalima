package com.kalima.quran.quiz

import com.kalima.quran.data.QuranWord

data class VerseExcerpt(
    val text: String,
    val highlightStart: Int,
    val highlightEnd: Int,
) {
    val hasHighlight: Boolean get() = highlightStart >= 0 && highlightEnd > highlightStart
}

data class FullVerseCloze(
    val text: String,
    val completeAyah: String,
    val removedOccurrences: Int,
) {
    fun restore(): String = completeAyah
}

object VerseExcerptBuilder {
    private const val DEFAULT_MAX_CHARS = 180
    const val CLOZE_BLANK = "________"

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
        return excerpt.text.replaceRange(excerpt.highlightStart, excerpt.highlightEnd, CLOZE_BLANK)
    }

    /** Builds a checkpoint cloze from the complete ayah, removing every matching occurrence. */
    fun buildFullCloze(word: QuranWord): FullVerseCloze? {
        val verse = word.verseArabic
        val ranges = findRanges(verse, word.arabic).ifEmpty {
            findRanges(verse, word.lemma)
        }
        if (ranges.isEmpty()) return null

        val cloze = ranges.asReversed().fold(verse) { text, range ->
            text.replaceRange(range.first, range.last + 1, CLOZE_BLANK)
        }
        return FullVerseCloze(
            text = cloze,
            completeAyah = verse,
            removedOccurrences = ranges.size,
        )
    }

    internal fun findRange(text: String, target: String): IntRange? {
        return findRanges(text, target).firstOrNull()
    }

    private fun findRanges(text: String, target: String): List<IntRange> {
        val normalizedText = normalizeWithIndexes(text)
        val normalizedTarget = normalizeWithIndexes(target).value
        if (normalizedTarget.isEmpty()) return emptyList()

        return buildList {
            var searchStart = 0
            while (searchStart <= normalizedText.value.length - normalizedTarget.length) {
                val normalizedStart = normalizedText.value.indexOf(normalizedTarget, searchStart)
                if (normalizedStart < 0) break
                val normalizedEnd = normalizedStart + normalizedTarget.length - 1
                val originalStart = normalizedText.originalIndexes[normalizedStart]
                var originalEnd = normalizedText.originalIndexes[normalizedEnd]
                while (
                    originalEnd + 1 < text.length &&
                    (text[originalEnd + 1] == 'ـ' || text[originalEnd + 1].isArabicMark())
                ) {
                    originalEnd += 1
                }
                add(originalStart..originalEnd)
                searchStart = normalizedEnd + 1
            }
        }
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
