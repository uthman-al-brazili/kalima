package com.kalima.quran.data

import java.text.Normalizer
import java.util.Locale

data class QuranWordAudioLocation(
    val surah: Int,
    val ayah: Int,
    val word: Int,
) {
    init {
        require(surah in 1..114) { "Invalid surah: $surah" }
        require(ayah > 0) { "Invalid ayah: $ayah" }
        require(word > 0) { "Invalid word position: $word" }
    }

    val fileName: String
        get() = String.format(
            Locale.ROOT,
            "%03d_%03d_%03d.mp3",
            surah,
            ayah,
            word,
        )

    val quranComUrl: String
        get() = "https://audio.qurancdn.com/wbw/$fileName"
}

internal object QuranWordAudioLocationResolver {
    private val locationId = Regex("^s(\\d+)-v(\\d+)-w(\\d+)$")
    private val referenceLocation = Regex("(\\d+):(\\d+)$")

    fun resolve(
        id: String,
        arabic: String,
        reference: String,
        verseArabic: String,
    ): QuranWordAudioLocation? {
        val match = referenceLocation.find(reference.trim()) ?: return null
        return resolve(
            id = id,
            arabic = arabic,
            referenceSurah = match.groupValues[1].toInt(),
            referenceAyah = match.groupValues[2].toInt(),
            verseArabic = verseArabic,
        )
    }

    fun resolve(
        id: String,
        arabic: String,
        referenceSurah: Int,
        referenceAyah: Int,
        verseArabic: String,
    ): QuranWordAudioLocation? {
        locationId.matchEntire(id)?.let { match ->
            return runCatching {
                QuranWordAudioLocation(
                    surah = match.groupValues[1].toInt(),
                    ayah = match.groupValues[2].toInt(),
                    word = match.groupValues[3].toInt(),
                )
            }.getOrNull()
        }

        val target = normalizedLetters(arabic)
        if (target.isEmpty()) return null
        val verseWords = verseArabic
            .split(Regex("\\s+"))
            .map(::normalizedLetters)
            .filter(String::isNotEmpty)
        val position = verseWords.indexOfFirst { it == target }
            .takeIf { it >= 0 }
            ?.plus(1)
            ?: return null

        return runCatching {
            QuranWordAudioLocation(referenceSurah, referenceAyah, position)
        }.getOrNull()
    }

    private fun normalizedLetters(value: String): String = buildString(value.length) {
        Normalizer.normalize(value, Normalizer.Form.NFD).forEach { character ->
            val normalized = when (character) {
                '\u0622', '\u0623', '\u0625', '\u0671' -> '\u0627'
                '\u0649' -> '\u064A'
                else -> character
            }
            if (normalized != '\u0640' &&
                Character.isLetter(normalized) &&
                Character.UnicodeBlock.of(normalized) == Character.UnicodeBlock.ARABIC
            ) {
                append(normalized)
            }
        }
    }
}
