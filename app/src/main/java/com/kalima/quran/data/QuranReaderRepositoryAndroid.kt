package com.kalima.quran.data

import android.content.Context
import android.util.Log

fun initializeQuranReader(context: Context) {
    try {
        QuranReaderRepository.initialize(context.assets.open(QuranTextAssetLoader.ASSET_NAME))
    } catch (error: Exception) {
        Log.e(TAG, "Could not load the bundled Quran text", error)
    }
}

private const val TAG = "QuranReaderRepository"
