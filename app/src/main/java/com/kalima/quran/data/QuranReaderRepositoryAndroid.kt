package com.kalima.quran.data

import android.content.Context
import android.util.Log

fun QuranReaderRepository.initialize(context: Context) {
    try {
        initialize(context.assets.open(QuranTextAssetLoader.ASSET_NAME))
    } catch (error: Exception) {
        Log.e(TAG, "Could not load the bundled Quran text", error)
    }
}

private const val TAG = "QuranReaderRepository"
