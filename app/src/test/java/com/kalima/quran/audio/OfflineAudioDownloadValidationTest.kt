package com.kalima.quran.audio

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineAudioDownloadValidationTest {
    @Test
    fun `audio connection rejects other hosts and disables redirects`() {
        assertThrows(IllegalStateException::class.java) {
            openPinnedAudioConnection(
                url = URL("https://example.com/audio.mp3"),
                expectedHost = "audio.qurancdn.com",
                userAgent = "test",
                connectTimeoutMs = 1,
                readTimeoutMs = 1,
            )
        }

        val connection = openPinnedAudioConnection(
            url = URL("https://audio.qurancdn.com/audio.mp3"),
            expectedHost = "audio.qurancdn.com",
            userAgent = "test",
            connectTimeoutMs = 1,
            readTimeoutMs = 1,
        )
        try {
            assertFalse(connection.instanceFollowRedirects)
        } finally {
            connection.disconnect()
        }
    }

    @Test
    fun `bounded copy accepts input at limit`() {
        val input = ByteArray(8) { it.toByte() }
        val output = ByteArrayOutputStream()

        copyWithLimit(ByteArrayInputStream(input), output, maxBytes = 8)

        assertArrayEquals(input, output.toByteArray())
    }

    @Test
    fun `bounded copy stops writing at limit`() {
        val output = ByteArrayOutputStream()

        assertThrows(IOException::class.java) {
            copyWithLimit(ByteArrayInputStream(ByteArray(9)), output, maxBytes = 8)
        }
        assertTrue(output.size() <= 8)
    }

    @Test
    fun `response validation rejects oversized and non-audio bodies`() {
        assertThrows(IllegalStateException::class.java) {
            validateAudioResponse(
                FakeConnection(contentType = "audio/mpeg", contentLength = 9),
                maxBytes = 8,
            )
        }
        assertThrows(IllegalStateException::class.java) {
            validateAudioResponse(
                FakeConnection(contentType = "text/html", contentLength = 8),
                maxBytes = 8,
            )
        }
    }

    @Test
    fun `response validation accepts bounded MP3 with content type parameters`() {
        validateAudioResponse(
            FakeConnection(contentType = "audio/mpeg; charset=binary", contentLength = 8),
            maxBytes = 8,
        )
    }

    @Test
    fun `recognizes MP3 signatures and rejects arbitrary files`() {
        val id3File = temporaryFile(
            byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte()),
        )
        val frameFile = temporaryFile(byteArrayOf(0xff.toByte(), 0xfb.toByte(), 0x90.toByte()))
        val textFile = temporaryFile("not audio".toByteArray())
        try {
            assertTrue(hasMp3Signature(id3File))
            assertTrue(hasMp3Signature(frameFile))
            assertFalse(hasMp3Signature(textFile))
        } finally {
            id3File.delete()
            frameFile.delete()
            textFile.delete()
        }
    }

    private fun temporaryFile(content: ByteArray): File =
        File.createTempFile("kalima-audio-", ".tmp").apply { writeBytes(content) }

    private class FakeConnection(
        private val contentType: String,
        private val contentLength: Long,
    ) : HttpURLConnection(URL("https://audio.qurancdn.com/audio.mp3")) {
        override fun getResponseCode(): Int = HTTP_OK
        override fun getContentType(): String = contentType
        override fun getContentLengthLong(): Long = contentLength
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy(): Boolean = false
    }
}
