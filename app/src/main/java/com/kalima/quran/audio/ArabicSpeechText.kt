package com.kalima.quran.audio

import java.text.Normalizer

object ArabicSpeechText {
    fun prepare(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFC)
        .filterNot(::isQuranicAnnotation)
        .replace("\u0640", "")
        .trim()

    private fun isQuranicAnnotation(character: Char): Boolean =
        character.code in 0x0610..0x061A ||
            character.code in 0x06D6..0x06ED ||
            character.code in 0x08D3..0x08E1
}
