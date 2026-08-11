package com.kalima.quran.data

data class QuranWord(
    val id: String,
    val arabic: String,
    val lemma: String,
    val transliteration: String,
    val meaning: String,
    val root: String,
    val grammar: String,
    val category: String,
    val reference: String,
    val verseArabic: String,
    val verseMeaning: String,
    val insight: String,
    val frequency: Int = 1,
    val surahNumber: Int? = null,
    val isFrequent: Boolean = false,
)

enum class StudyScope {
    All,
    Frequent,
    Surahs,
}

enum class WordStatus {
    New,
    Reviewing,
    Learned,
}
