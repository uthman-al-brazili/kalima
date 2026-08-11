package com.kalima.quran.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class ArabicSpeechTextTest {
    @Test
    fun `keeps Arabic vowel marks while removing Quranic annotations`() {
        assertEquals("بِهِ", ArabicSpeechText.prepare(" بِهِۦ "))
        assertEquals("عَلَىٰ", ArabicSpeechText.prepare("عَلَىٰۚ"))
    }

    @Test
    fun `removes decorative elongation`() {
        assertEquals("كتاب", ArabicSpeechText.prepare("كـتاب"))
    }
}
