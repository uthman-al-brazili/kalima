package com.kalima.quran.data

import java.io.BufferedInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream

data class QuranPageToken(
    val pageNumber: Int,
    val lineNumber: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val wordNumber: Int,
    val arabic: String,
    val isAyahMarker: Boolean,
)

internal object QuranTextAssetLoader {
    // Android expands .gz assets and exposes them without the compression suffix.
    const val ASSET_NAME = "quran_arabic.tsv"
    private const val FORMAT_HEADER = "#kalima-quran-pages-v2"
    private const val FIELD_COUNT = 7

    fun load(input: InputStream): List<QuranPageToken> {
        val tokens = ArrayList<QuranPageToken>(84_000)
        decoded(input).bufferedReader(Charsets.UTF_8).use { reader ->
            require(reader.readLine() == FORMAT_HEADER) { "Unrecognized Quran text format" }
            reader.lineSequence().forEachIndexed { index, line ->
                val fields = line.split('\t', limit = FIELD_COUNT)
                require(fields.size == FIELD_COUNT) {
                    "Invalid Quran text record on line ${index + 2}: ${fields.size} fields"
                }
                tokens += QuranPageToken(
                    pageNumber = fields[0].toInt(),
                    lineNumber = fields[1].toInt(),
                    surahNumber = fields[2].toInt(),
                    ayahNumber = fields[3].toInt(),
                    wordNumber = fields[4].toInt(),
                    isAyahMarker = fields[5] == "end",
                    arabic = fields[6],
                )
            }
        }
        return tokens
    }

    private fun decoded(input: InputStream): InputStream {
        val buffered = BufferedInputStream(input)
        buffered.mark(2)
        val first = buffered.read()
        val second = buffered.read()
        buffered.reset()
        return if (first == GZIP_MAGIC_FIRST && second == GZIP_MAGIC_SECOND) {
            GZIPInputStream(buffered)
        } else {
            buffered
        }
    }

    private const val GZIP_MAGIC_FIRST = 0x1f
    private const val GZIP_MAGIC_SECOND = 0x8b
}
