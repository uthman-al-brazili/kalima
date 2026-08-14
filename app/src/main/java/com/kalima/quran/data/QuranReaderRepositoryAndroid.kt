package com.kalima.quran.data

import android.content.Context

fun QuranReaderRepository.initialize(context: Context) {
    initialize(context.assets.open(QuranTextAssetLoader.ASSET_NAME))
}
