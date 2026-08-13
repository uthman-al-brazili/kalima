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
    Frequent50,
    Frequent,
    Frequent300,
    Frequent500,
    Prayer,
    ShortSurahs,
    Custom,
    Surahs;

    companion object {
        fun fromPersistedName(name: String?): StudyScope? = when (name) {
            LEGACY_FAVORITES_SCOPE -> Custom
            else -> entries.firstOrNull { it.name == name }
        }

        private const val LEGACY_FAVORITES_SCOPE = "Favorites"
    }
}

internal fun mergePersonalCollections(
    legacyFavoriteIds: Set<String>,
    customStudyIds: Set<String>,
): Set<String> = legacyFavoriteIds + customStudyIds

enum class WordStatus {
    New,
    Reviewing,
    Learned,
}
