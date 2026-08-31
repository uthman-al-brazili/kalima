package com.kalima.quran.audio

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

internal fun openPinnedAudioConnection(
    url: URL,
    expectedHost: String,
    userAgent: String,
    connectTimeoutMs: Int,
    readTimeoutMs: Int,
): HttpURLConnection {
    check(url.protocol == "https" && url.host.equals(expectedHost, ignoreCase = true) && url.port == -1) {
        "Unexpected offline audio URL"
    }
    return (url.openConnection() as HttpURLConnection).apply {
        instanceFollowRedirects = false
        connectTimeout = connectTimeoutMs
        readTimeout = readTimeoutMs
        setRequestProperty("User-Agent", userAgent)
    }
}

internal fun validateAudioResponse(connection: HttpURLConnection, maxBytes: Long) {
    check(connection.responseCode == HttpURLConnection.HTTP_OK) {
        "Audio server returned HTTP ${connection.responseCode}"
    }
    val contentType = connection.contentType
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase(Locale.ROOT)
    check(contentType in SUPPORTED_MP3_CONTENT_TYPES) {
        "Audio server returned unsupported content type"
    }
    val declaredBytes = connection.contentLengthLong
    check(declaredBytes < 0L || declaredBytes <= maxBytes) {
        "Audio response exceeds the $maxBytes-byte limit"
    }
}

internal fun copyWithLimit(
    input: InputStream,
    output: OutputStream,
    maxBytes: Long,
): Long {
    require(maxBytes > 0L) { "Copy byte limit must be positive" }
    val buffer = ByteArray(COPY_BUFFER_BYTES)
    var totalBytes = 0L
    while (true) {
        val remaining = maxBytes - totalBytes
        val requestedBytes = if (remaining >= buffer.size) {
            buffer.size
        } else {
            (remaining + 1L).toInt()
        }
        val bytesRead = input.read(
            buffer,
            0,
            requestedBytes,
        )
        if (bytesRead < 0) return totalBytes
        if (bytesRead == 0) continue
        if (bytesRead > remaining) throw IOException("Audio response exceeds the $maxBytes-byte limit")
        output.write(buffer, 0, bytesRead)
        totalBytes += bytesRead
    }
}

internal fun hasMp3Signature(file: File): Boolean {
    if (!file.isFile) return false
    val header = ByteArray(3)
    val bytesRead = file.inputStream().use { it.read(header) }
    if (bytesRead < header.size) return false
    val hasId3Tag = header[0] == 'I'.code.toByte() &&
        header[1] == 'D'.code.toByte() &&
        header[2] == '3'.code.toByte()
    val hasMpegFrame = (header[0].toInt() and 0xff) == 0xff &&
        (header[1].toInt() and 0xe0) == 0xe0
    return hasId3Tag || hasMpegFrame
}

private val SUPPORTED_MP3_CONTENT_TYPES = setOf("audio/mpeg", "audio/mp3")
private const val COPY_BUFFER_BYTES = 8 * 1024
