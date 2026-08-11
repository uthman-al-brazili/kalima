package com.kalima.quran.localization

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import java.util.Locale

enum class AppLanguage(val languageTag: String) {
    Portuguese("pt"),
    English("en"),
}

object LanguageManager {
    fun selectedLanguage(context: Context): AppLanguage {
        val storedTag = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null)
        return AppLanguage.entries.firstOrNull { it.languageTag == storedTag }
            ?: AppLanguage.Portuguese
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit {
            putString(KEY_LANGUAGE, language.languageTag)
        }
    }

    fun localizedContext(context: Context): Context {
        val locale = Locale.forLanguageTag(selectedLanguage(context).languageTag)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(configuration)
    }

    private const val PREFERENCES = "kalima_language"
    private const val KEY_LANGUAGE = "language"
}
