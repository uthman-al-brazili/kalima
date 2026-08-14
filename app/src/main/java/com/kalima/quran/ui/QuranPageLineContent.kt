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
