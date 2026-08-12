package com.kalima.quran.desktop

import com.kalima.quran.data.AppThemeMode
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.WordStatus
import com.kalima.quran.localization.AppLanguage
import com.kalima.quran.quiz.QuizMode

fun AppLanguage.t(portuguese: String, english: String): String =
    if (this == AppLanguage.Portuguese) portuguese else english

fun StudyScope.label(language: AppLanguage): String = when (this) {
    StudyScope.All -> language.t("Todo o corpus", "Full corpus")
    StudyScope.Frequent50 -> language.t("Primeiras 50", "First 50")
    StudyScope.Frequent -> language.t("Top 100", "Top 100")
    StudyScope.Frequent300 -> language.t("Top 300", "Top 300")
    StudyScope.Frequent500 -> language.t("Top 500", "Top 500")
    StudyScope.Prayer -> language.t("Orações", "Prayer")
    StudyScope.ShortSurahs -> language.t("Suras curtas", "Short surahs")
    StudyScope.Favorites -> language.t("Favoritas", "Favorites")
    StudyScope.Custom -> language.t("Minha lista", "My list")
    StudyScope.Surahs -> language.t("Por sura", "By surah")
}

fun WordStatus.label(language: AppLanguage): String = when (this) {
    WordStatus.New -> language.t("Nova", "New")
    WordStatus.Reviewing -> language.t("Em revisão", "Reviewing")
    WordStatus.Learned -> language.t("Aprendida", "Learned")
}

fun QuizMode.label(language: AppLanguage): String = when (this) {
    QuizMode.Mixed -> language.t("Misto", "Mixed")
    QuizMode.Listening -> language.t("Escuta", "Listening")
    QuizMode.Cloze -> language.t("Lacuna", "Cloze")
    QuizMode.Roots -> language.t("Raízes", "Roots")
    QuizMode.ReviewsOnly -> language.t("Revisões", "Reviews")
    QuizMode.Difficult -> language.t("Difíceis", "Difficult")
}

fun AppThemeMode.label(language: AppLanguage): String = when (this) {
    AppThemeMode.Auto -> language.t("Do sistema", "System")
    AppThemeMode.Light -> language.t("Claro", "Light")
    AppThemeMode.Dark -> language.t("Escuro", "Dark")
}
