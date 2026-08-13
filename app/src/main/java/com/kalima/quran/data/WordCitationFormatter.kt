package com.kalima.quran.data

object WordCitationFormatter {
    fun format(word: QuranWord, corpusIdentity: String): String = buildString {
        appendLine(word.arabic)
        if (word.transliteration.isNotBlank()) appendLine(word.transliteration)
        appendLine(word.meaning)
        appendLine()
        appendLine(word.verseArabic)
        if (word.verseMeaning.isNotBlank()) appendLine(word.verseMeaning)
        appendLine(word.reference)
        appendLine()
        appendLine("Kalima card: ${word.id}")
        append("Corpus: $corpusIdentity")
    }
}
