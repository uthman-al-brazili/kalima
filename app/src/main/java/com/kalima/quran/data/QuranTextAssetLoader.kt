package com.kalima.quran.data

import java.io.BufferedInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream

data class QuranVerse(
    val surahNumber: Int,
    val ayahNumber: Int,
    val arabic: String,
)

internal object QuranTextAssetLoader {
    const val ASSET_NAME = "quran_arabic.tsv.gz"
    private const val FORMAT_HEADER = "#kalima-quran-text-v1"

    fun load(input: InputStream): List<QuranVerse> {
        val verses = ArrayList<QuranVerse>(6_236)
        decoded(input).bufferedReader(Charsets.UTF_8).use { reader ->
            require(reader.readLine() == FORMAT_HEADER) { "Unrecognized Quran text format" }
            reader.lineSequence().forEachIndexed { index, line ->
                val fields = line.split('\t', limit = 3)
                require(fields.size == 3) {
                    "Invalid Quran text record on line ${index + 2}: ${fields.size} fields"
                }
                verses += QuranVerse(
                    surahNumber = fields[0].toInt(),
                    ayahNumber = fields[1].toInt(),
                    arabic = fields[2],
                )
            }
        }
        return verses
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
