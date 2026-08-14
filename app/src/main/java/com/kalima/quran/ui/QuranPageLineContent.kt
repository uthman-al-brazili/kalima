package com.kalima.quran.ui

import com.kalima.quran.data.QuranPageToken

internal data class QuranPageLineSegment(
    val token: QuranPageToken,
    val start: Int,
    val endExclusive: Int,
)

internal data class QuranPageLineContent(
    val text: String,
    val segments: List<QuranPageLineSegment>,
) {
    val words: List<QuranPageToken> = segments
        .asSequence()
        .map(QuranPageLineSegment::token)
        .filterNot(QuranPageToken::isAyahMarker)
        .toList()
}

internal data class QuranPageSection(
    val startingSurahNumber: Int?,
    val tokens: List<QuranPageToken>,
)

internal fun quranPageSections(tokens: List<QuranPageToken>): List<QuranPageSection> = buildList {
    var startingSurahNumber: Int? = null
    var sectionStart = 0

    tokens.forEachIndexed { index, token ->
        val startsSurah = token.ayahNumber == 1 &&
            token.wordNumber == 1 &&
            !token.isAyahMarker
        if (startsSurah) {
            if (index > sectionStart) {
                add(QuranPageSection(startingSurahNumber, tokens.subList(sectionStart, index)))
            }
            startingSurahNumber = token.surahNumber
            sectionStart = index
        }
    }

    if (sectionStart < tokens.size) {
        add(QuranPageSection(startingSurahNumber, tokens.subList(sectionStart, tokens.size)))
    }
}

internal fun quranPageLineContent(tokens: List<QuranPageToken>): QuranPageLineContent {
    val segments = ArrayList<QuranPageLineSegment>(tokens.size)
    val text = buildString {
        tokens.forEachIndexed { index, token ->
            if (index > 0) append(' ')
            val start = length
            if (token.isAyahMarker) append(QURAN_AYAH_END_SYMBOL)
            append(token.arabic)
            segments += QuranPageLineSegment(
                token = token,
                start = start,
                endExclusive = length,
            )
        }
    }
    return QuranPageLineContent(text = text, segments = segments)
}

private const val QURAN_AYAH_END_SYMBOL = "۝"
