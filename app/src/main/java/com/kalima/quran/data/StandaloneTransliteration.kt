package com.kalima.quran.data

/**
 * Converts the corpus's contextual transliteration into the form heard when the
 * displayed Quranic token is pronounced on its own (waqf).
 */
internal fun standaloneTransliteration(arabic: String, contextual: String): String {
    if (contextual.isBlank()) return contextual

    val letters = arabic.filter(Char::isLetter)
    var result = when {
        letters == "ٱلله" -> "allāh"
        letters.startsWith(ARABIC_DEFINITE_ARTICLE) ->
            restoreDefiniteArticle(letters.drop(2).firstOrNull(), contextual)
        else -> contextual
    }

    val finalLetter = letters.lastOrNull()
    val finalVowel = arabic.lastOrNull { it in TERMINAL_VOWEL_MARKS }
    result = when {
        finalLetter == TA_MARBUTA -> result.replace(TA_MARBUTA_ENDING, "h")
        finalVowel == FATHATAN && result.endsWith("an") -> result.dropLast(2) + "ā"
        finalVowel == DAMMATAN && result.endsWith("un") -> result.dropLast(2)
        finalVowel == KASRATAN && result.endsWith("in") -> result.dropLast(2)
        finalVowel == FATHA && result.endsWith("a") -> result.dropLast(1)
        finalVowel == DAMMA && result.endsWith("u") -> result.dropLast(1)
        finalVowel == KASRA && result.endsWith("i") -> result.dropLast(1)
        else -> result
    }
    return result
}

private fun restoreDefiniteArticle(firstNounLetter: Char?, contextual: String): String {
    val stem = when {
        contextual.startsWith("al-") -> contextual.drop(3)
        contextual.startsWith("l-") -> contextual.drop(2)
        else -> return contextual
    }
    val sunLetter = SUN_LETTER_PREFIXES[firstNounLetter]
    return if (sunLetter == null) "al-$stem" else "a$sunLetter-$stem"
}

private const val ARABIC_DEFINITE_ARTICLE = "ٱل"
private const val TA_MARBUTA = 'ة'
private const val FATHATAN = 'ً'
private const val DAMMATAN = 'ٌ'
private const val KASRATAN = 'ٍ'
private const val FATHA = 'َ'
private const val DAMMA = 'ُ'
private const val KASRA = 'ِ'
private const val SUKUN = 'ْ'

private val TERMINAL_VOWEL_MARKS = setOf(
    FATHATAN,
    DAMMATAN,
    KASRATAN,
    FATHA,
    DAMMA,
    KASRA,
    SUKUN,
)

private val TA_MARBUTA_ENDING = Regex("t(?:an|in|un|a|i|u)$")

private val SUN_LETTER_PREFIXES = mapOf(
    'ت' to "t",
    'ث' to "th",
    'د' to "d",
    'ذ' to "dh",
    'ر' to "r",
    'ز' to "z",
    'س' to "s",
    'ش' to "sh",
    'ص' to "ṣ",
    'ض' to "ḍ",
    'ط' to "ṭ",
    'ظ' to "ẓ",
    'ل' to "l",
    'ن' to "n",
)
