package com.kalima.quran.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicVoiceInstallerTest {
    @Test
    fun identifiesWhetherGoogleIsTheDefaultEngine() {
        assertTrue(ArabicVoiceInstaller.isGoogleEngine("com.google.android.tts"))
        assertFalse(ArabicVoiceInstaller.isGoogleEngine("com.samsung.SMT"))
        assertFalse(ArabicVoiceInstaller.isGoogleEngine(null))
    }
}
