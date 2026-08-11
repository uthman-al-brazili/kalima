package com.kalima.quran.data

import java.io.BufferedInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream

internal object VocabularyAssetLoader {
    const val ASSET_NAME = "quran_vocabulary.tsv"
    private const val FORMAT_HEADER = "#kalima-quran-v1"
    private const val FIELD_COUNT = 13

    fun load(input: InputStream): List<QuranWord> {
        val stringPool = HashMap<String, String>(65_536)
        val surahNames = GeneratedQuranSurahs.all.associate { it.number to it.transliteratedName }
        val result = ArrayList<QuranWord>(42_101)

        decoded(input).bufferedReader(Charsets.UTF_8).use { reader ->
            require(reader.readLine() == FORMAT_HEADER) { "Formato de corpus não reconhecido" }
            reader.lineSequence().forEachIndexed { index, line ->
                val fields = line.split('\t', limit = FIELD_COUNT)
                require(fields.size == FIELD_COUNT) {
                    "Registro inválido na linha ${index + 2}: ${fields.size} campos"
                }

                val referenceSurah = fields[7].toInt()
                val referenceVerse = fields[8].toInt()
                val frequency = fields[9].toInt()
                val studySurah = fields[10].toInt()
                val isFrequent = fields[11].toBooleanStrict()
                val meaning = fields[4].pooled(stringPool)
                val lemma = fields[2].pooled(stringPool)
                val surahName = surahNames[referenceSurah] ?: "Sura $referenceSurah"
                val occurrence = if (frequency == 1) "vez" else "vezes"

                result += QuranWord(
                    id = fields[0],
                    arabic = fields[1].pooled(stringPool),
                    lemma = lemma,
                    transliteration = fields[3].pooled(stringPool),
                    meaning = meaning,
                    root = fields[5].pooled(stringPool),
                    grammar = fields[6].pooled(stringPool),
                    category = if (isFrequent) "Mais frequentes" else "Sura $studySurah",
                    reference = "$surahName $referenceSurah:$referenceVerse",
                    verseArabic = fields[12].pooled(stringPool),
                    verseMeaning = "Neste contexto, o sentido de apoio é: $meaning.",
                    insight = if (isFrequent) {
                        "Esta forma aparece $frequency vezes no Alcorão. Lema registrado: $lemma."
                    } else {
                        "Aparece $frequency $occurrence nesta sura. Lema registrado: $lemma."
                    },
                    frequency = frequency,
                    surahNumber = studySurah.takeIf { it > 0 },
                    isFrequent = isFrequent,
                )
            }
        }
        return result
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

    private fun String.pooled(pool: MutableMap<String, String>): String =
        pool.getOrPut(this) { this }

    private const val GZIP_MAGIC_FIRST = 0x1f
    private const val GZIP_MAGIC_SECOND = 0x8b
}
