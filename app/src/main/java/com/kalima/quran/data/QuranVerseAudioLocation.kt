package com.kalima.quran.data

import java.util.Locale

data class QuranVerseAudioLocation(
    val surah: Int,
    val ayah: Int,
) {
    init {
        require(surah in 1..114) { "Invalid surah: $surah" }
        require(ayah > 0) { "Invalid ayah: $ayah" }
    }

    val fileName: String
        get() = String.format(Locale.ROOT, "%03d%03d.mp3", surah, ayah)

    val hussaryUrl: String
        get() = "https://everyayah.com/data/Husary_128kbps/$fileName"

    companion object {
        fun fromWord(location: QuranWordAudioLocation): QuranVerseAudioLocation =
            QuranVerseAudioLocation(location.surah, location.ayah)
    }
}
