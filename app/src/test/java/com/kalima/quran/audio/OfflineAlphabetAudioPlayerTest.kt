package com.kalima.quran.audio

import com.kalima.quran.data.AlphabetAudio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OfflineAlphabetAudioPlayerTest {
    @Test
    fun `short letter recordings use a preloaded audible sequence`() {
        val source = sequenceOf(
            File("src/main/java/com/kalima/quran/audio/OfflineAlphabetAudioPlayer.kt"),
            File("app/src/main/java/com/kalima/quran/audio/OfflineAlphabetAudioPlayer.kt"),
        ).first(File::isFile).readText()

        assertTrue(source.contains("SoundPool.Builder()"))
        assertTrue(source.contains("soundPool.load(applicationContext"))
        assertTrue(source.contains("requestAudioFocus(audioFocusRequest)"))
        assertTrue(source.contains("soundPool.play(soundId, 1f, 1f"))
        assertTrue(source.contains("if (sequenceActive)"))
        assertTrue(source.contains("LETTER_NAME_PLAY_COUNT = 3"))
        assertTrue(source.contains("LETTER_NAME_REPLAY_INTERVAL_MS = 1_100L"))
    }

    @Test
    fun `all letter recordings map to distinct bundled resources`() {
        val resources = AlphabetAudio.entries.map(::alphabetAudioResource)

        assertEquals(28, resources.size)
        assertEquals(28, resources.toSet().size)
        assertTrue(resources.all { it != 0 })
    }

    @Test
    fun `approved Arabic names select every offline recording`() {
        AlphabetAudio.entries.forEach { audio ->
            assertEquals(audio, AlphabetAudio.fromSpokenArabic(audio.spokenArabic))
        }
        assertEquals(null, AlphabetAudio.fromSpokenArabic("بَ"))
    }
}
