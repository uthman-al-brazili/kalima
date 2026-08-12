package com.kalima.quran.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopPronouncerTest {
    @Test
    fun `successful speech requires the started marker`() {
        assertEquals(
            DesktopSpeechResult.Started,
            DesktopPronouncer.classifySpeechResult(0, "KALIMA_SPEECH_STARTED"),
        )
        assertEquals(
            DesktopSpeechResult.Failed,
            DesktopPronouncer.classifySpeechResult(0, ""),
        )
    }

    @Test
    fun `missing Arabic voice is reported separately`() {
        assertEquals(
            DesktopSpeechResult.Unavailable,
            DesktopPronouncer.classifySpeechResult(2, "KALIMA_ARABIC_VOICE_UNAVAILABLE"),
        )
    }

    @Test
    fun `unexpected process failures are reported`() {
        assertEquals(
            DesktopSpeechResult.Failed,
            DesktopPronouncer.classifySpeechResult(3, "KALIMA_SPEECH_FAILED"),
        )
    }

    @Test
    fun `Windows speech bridge starts or identifies a missing Arabic voice`() {
        if (!System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) return

        val result = DesktopPronouncer.executeSpeech(text = "", slow = true)

        assertTrue(
            result == DesktopSpeechResult.Started || result == DesktopSpeechResult.Unavailable,
            "Expected a working speech bridge, but received $result",
        )
    }
}
