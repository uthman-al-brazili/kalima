package com.kalima.quran.data

import com.kalima.quran.localization.AppLanguage
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream

internal object VocabularyAssetLoader {
    const val ASSET_NAME = "quran_vocabulary.tsv"
    private const val FORMAT_HEADER = "#kalima-quran-v2"
    private const val FIELD_COUNT = 14

    fun load(
        input: InputStream,
        language: AppLanguage = AppLanguage.Portuguese,
    ): List<QuranWord> {
        val stringPool = HashMap<String, String>(65_536)
        val surahNames = GeneratedQuranSurahs.all.associate { it.number to it.transliteratedName }
        val result = ArrayList<QuranWord>(42_101)
        val english = language == AppLanguage.English

        decoded(input).bufferedReader(Charsets.UTF_8).use { reader ->
            require(reader.readLine() == FORMAT_HEADER) { "Unrecognized corpus format" }
            reader.lineSequence().forEachIndexed { index, line ->
                val fields = line.split('\t', limit = FIELD_COUNT)
                require(fields.size == FIELD_COUNT) {
                    "Invalid record on line ${index + 2}: ${fields.size} fields"
                }

                val referenceSurah = fields[8].toInt()
                val referenceVerse = fields[9].toInt()
                val frequency = fields[10].toInt()
                val studySurah = fields[11].toInt()
                val isFrequent = fields[12].toBooleanStrict()
                val meaning = fields[if (english) 5 else 4].pooled(stringPool)
                val lemma = fields[2].pooled(stringPool)
                val surahName = surahNames[referenceSurah] ?: "Surah $referenceSurah"
                val grammar = localizedGrammar(fields[7], language).pooled(stringPool)
                val audioLocation = requireNotNull(
                    QuranWordAudioLocationResolver.resolve(
                        id = fields[0],
                        arabic = fields[1],
                        referenceSurah = referenceSurah,
                        referenceAyah = referenceVerse,
                        verseArabic = fields[13],
                    ),
                ) { "No Quran.com word-audio location for ${fields[0]}" }

                result += QuranWord(
                    id = fields[0],
                    arabic = fields[1].pooled(stringPool),
                    lemma = lemma,
                    transliteration = fields[3].pooled(stringPool),
                    meaning = meaning,
                    root = fields[6].pooled(stringPool),
                    grammar = grammar,
                    category = when {
                        isFrequent && english -> "Most frequent"
                        isFrequent -> "Mais frequentes"
                        english -> "Surah $studySurah"
                        else -> "Sura $studySurah"
                    },
                    reference = "$surahName $referenceSurah:$referenceVerse",
                    verseArabic = fields[13].pooled(stringPool),
                    verseMeaning = if (english) {
                        "In this context, the supporting meaning is: $meaning."
                    } else {
                        "Neste contexto, o sentido de apoio é: $meaning."
                    },
                    insight = when {
                        isFrequent && english ->
                            "This form appears $frequency times in the Quran."
                        isFrequent ->
                            "Esta forma aparece $frequency vezes no Alcorão."
                        english -> {
                            val occurrence = if (frequency == 1) "time" else "times"
                            "It appears $frequency $occurrence in this surah."
                        }
                        else -> {
                            val occurrence = if (frequency == 1) "vez" else "vezes"
                            "Aparece $frequency $occurrence nesta sura."
                        }
                    },
                    frequency = frequency,
                    surahNumber = studySurah.takeIf { it > 0 },
                    isFrequent = isFrequent,
                    audioLocation = audioLocation,
                )
            }
        }
        return result
    }

    private fun localizedGrammar(value: String, language: AppLanguage): String {
        if (language == AppLanguage.Portuguese) return value
        return when (value) {
            "substantivo" -> "noun"
            "verbo" -> "verb"
            "partícula" -> "particle"
            "adjetivo" -> "adjective"
            "nome próprio" -> "proper noun"
            else -> value
        }
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
