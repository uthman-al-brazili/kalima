package com.kalima.quran.data

import android.content.Context
import com.kalima.quran.localization.LanguageManager

fun WordRepository.initialize(context: Context) {
    val language = LanguageManager.selectedLanguage(context)
    initialize(context.assets.open(VocabularyAssetLoader.ASSET_NAME), language)
}
