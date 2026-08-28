package com.kalima.quran.data

import java.text.Normalizer

data class VerseToken(
    val index: Int,
    val text: String,
    val word: QuranWord?,
)

object VerseExplorer {
    private val arabicMarks = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")

    fun buildTokens(verseArabic: String, candidates: List<QuranWord>): List<VerseToken> {
        val unused = candidates.toMutableList()
        return verseArabic.trim().split(Regex("\\s+")).mapIndexed { index, token ->
            val normalized = normalizeArabic(token)
            val matchIndex = unused.indexOfFirst { candidate ->
                normalizeArabic(candidate.arabic) == normalized
            }
            val match = if (matchIndex >= 0) unused.removeAt(matchIndex) else null
            VerseToken(index, token, match)
        }
    }

    internal fun buildIndexedTokens(
        tokens: List<QuranPageToken>,
        selectedWord: QuranWord,
        resolve: (QuranPageToken) -> QuranWord?,
    ): List<VerseToken> = tokens
        .asSequence()
        .filterNot(QuranPageToken::isAyahMarker)
        .mapIndexed { index, token ->
            val linkedWord = if (
                selectedWord.audioLocation.matches(token) &&
                normalizeArabic(selectedWord.arabic) == normalizeArabic(token.arabic)
            ) {
                selectedWord
            } else {
                resolve(token)
            }
            VerseToken(index, token.arabic, linkedWord)
        }
        .toList()

    internal fun buildIndexedTextTokens(
        verseArabic: String,
        surahNumber: Int,
        ayahNumber: Int,
        selectedWord: QuranWord,
        resolve: (QuranPageToken) -> QuranWord?,
    ): List<VerseToken> {
        val rawTokens = verseArabic
            .trim()
            .split(Regex("\\s+"))
            .filter { token -> token.any(Char::isLetter) }
        val tokens = buildList {
            var rawIndex = 0
            var wordNumber = 1
            while (rawIndex < rawTokens.size) {
                val probe = textToken(
                    arabic = rawTokens[rawIndex],
                    surahNumber = surahNumber,
                    ayahNumber = ayahNumber,
                    wordNumber = wordNumber,
                )
                val exactWord = resolve(probe)?.takeIf { word ->
                    word.audioLocation?.let { location ->
                        location.surah == surahNumber &&
                            location.ayah == ayahNumber &&
                            location.word == wordNumber
                    } == true
                }
                val exactParts = exactWord?.arabic
                    ?.trim()
                    ?.split(Regex("\\s+"))
                    ?.filter { part -> part.any(Char::isLetter) }
                    .orEmpty()
                val partCount = exactParts.size.takeIf { count ->
                    count > 1 &&
                        rawIndex + count <= rawTokens.size &&
                        exactParts.indices.all { offset ->
                            normalizeArabic(exactParts[offset]) ==
                                normalizeArabic(rawTokens[rawIndex + offset])
                        }
                } ?: 1
                add(
                    probe.copy(
                        arabic = rawTokens
                            .subList(rawIndex, rawIndex + partCount)
                            .joinToString(" "),
                    ),
                )
                rawIndex += partCount
                wordNumber += 1
            }
        }
        return buildIndexedTokens(tokens, selectedWord, resolve)
    }

    private fun textToken(
        arabic: String,
        surahNumber: Int,
        ayahNumber: Int,
        wordNumber: Int,
    ) = QuranPageToken(
        pageNumber = 0,
        lineNumber = 0,
        surahNumber = surahNumber,
        ayahNumber = ayahNumber,
        wordNumber = wordNumber,
        arabic = arabic,
        isAyahMarker = false,
    )

    fun normalizeArabic(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(arabicMarks, "")
        .replace('ٱ', 'ا')
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace("ـ", "")
        .trim()

    private fun QuranWordAudioLocation?.matches(token: QuranPageToken): Boolean =
        this != null &&
            surah == token.surahNumber &&
            ayah == token.ayahNumber &&
            word == token.wordNumber
}
