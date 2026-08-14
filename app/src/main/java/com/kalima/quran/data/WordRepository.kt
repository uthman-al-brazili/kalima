package com.kalima.quran.data

import com.kalima.quran.localization.AppLanguage
import java.io.InputStream
import java.security.MessageDigest
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

object WordRepository {
    private val ARABIC_MARKS = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")
    private val SEARCH_WHITESPACE = Regex("\\s+")
    private const val SEARCH_FIELD_SEPARATOR = "\u0000"

    private val curatedPortugueseWords: List<QuranWord> = listOf(
        QuranWord(
            id = "allah",
            arabic = "ٱللَّهُ",
            lemma = "الله",
            transliteration = "allāh",
            meaning = "Deus; o nome próprio divino",
            root = "أ ل ه",
            grammar = "nome próprio",
            category = "Fundamentos",
            reference = "Al-Fātiḥah 1:1",
            verseArabic = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
            verseMeaning = "Em nome de Deus, o Inteiramente Misericordioso, o Sempre Misericordioso.",
            insight = "No Alcorão, o nome Allah identifica o Deus único e não recebe forma plural.",
        ),
        QuranWord(
            id = "rabb",
            arabic = "رَبِّ",
            lemma = "رَبّ",
            transliteration = "rabb",
            meaning = "Senhor, cuidador e sustentador",
            root = "ر ب ب",
            grammar = "substantivo",
            category = "Fundamentos",
            reference = "Al-Fātiḥah 1:2",
            verseArabic = "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ",
            verseMeaning = "Todo louvor pertence a Deus, Senhor de todos os mundos.",
            insight = "Rabb reúne as ideias de autoridade, cuidado, educação e sustento.",
        ),
        QuranWord(
            id = "rahman",
            arabic = "ٱلرَّحْمَٰنِ",
            lemma = "رَحْمَٰن",
            transliteration = "ar-raḥmān",
            meaning = "o Inteiramente Misericordioso",
            root = "ر ح م",
            grammar = "nome intensivo",
            category = "Atributos",
            reference = "Al-Fātiḥah 1:3",
            verseArabic = "ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
            verseMeaning = "O Inteiramente Misericordioso, o Sempre Misericordioso.",
            insight = "A raiz r-ḥ-m está ligada à misericórdia, compaixão e cuidado.",
        ),
        QuranWord(
            id = "din",
            arabic = "ٱلدِّينِ",
            lemma = "دِين",
            transliteration = "ad-dīn",
            meaning = "retribuição, julgamento; também religião",
            root = "د ي ن",
            grammar = "substantivo",
            category = "Fundamentos",
            reference = "Al-Fātiḥah 1:4",
            verseArabic = "مَٰلِكِ يَوْمِ ٱلدِّينِ",
            verseMeaning = "Soberano do Dia da Retribuição.",
            insight = "O contexto define o sentido. Neste versículo, dīn indica prestação de contas e retribuição.",
        ),
        QuranWord(
            id = "nabudu",
            arabic = "نَعْبُدُ",
            lemma = "عَبَدَ",
            transliteration = "naʿbudu",
            meaning = "nós adoramos e servimos",
            root = "ع ب د",
            grammar = "verbo, 1ª pessoa do plural",
            category = "Adoração",
            reference = "Al-Fātiḥah 1:5",
            verseArabic = "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ",
            verseMeaning = "Somente a Ti adoramos e somente de Ti buscamos ajuda.",
            insight = "A mesma raiz forma ʿabd, servo. A adoração inclui submissão consciente e serviço.",
        ),
        QuranWord(
            id = "kitab",
            arabic = "ٱلْكِتَٰبُ",
            lemma = "كِتَاب",
            transliteration = "al-kitāb",
            meaning = "o Livro, a Escritura",
            root = "ك ت ب",
            grammar = "substantivo",
            category = "Revelação",
            reference = "Al-Baqarah 2:2",
            verseArabic = "ذَٰلِكَ ٱلْكِتَٰبُ لَا رَيْبَ فِيهِ",
            verseMeaning = "Este é o Livro no qual não há dúvida.",
            insight = "A raiz k-t-b envolve escrever, registrar e prescrever.",
        ),
        QuranWord(
            id = "huda",
            arabic = "هُدًى",
            lemma = "هُدًى",
            transliteration = "hudan",
            meaning = "orientação, guia",
            root = "ه د ي",
            grammar = "substantivo verbal",
            category = "Revelação",
            reference = "Al-Baqarah 2:2",
            verseArabic = "هُدًى لِّلْمُتَّقِينَ",
            verseMeaning = "Orientação para os conscientes de Deus.",
            insight = "A raiz h-d-y comunica conduzir alguém com gentileza ao caminho certo.",
        ),
        QuranWord(
            id = "muttaqin",
            arabic = "ٱلْمُتَّقِينَ",
            lemma = "مُتَّقٍ",
            transliteration = "al-muttaqīn",
            meaning = "os conscientes de Deus, os que se resguardam",
            root = "و ق ي",
            grammar = "particípio ativo, plural",
            category = "Caráter",
            reference = "Al-Baqarah 2:2",
            verseArabic = "هُدًى لِّلْمُتَّقِينَ",
            verseMeaning = "Orientação para os que cultivam consciência e proteção diante de Deus.",
            insight = "Taqwā vem da ideia de proteger-se: agir atento às consequências espirituais.",
        ),
        QuranWord(
            id = "ghayb",
            arabic = "ٱلْغَيْبِ",
            lemma = "غَيْب",
            transliteration = "al-ghayb",
            meaning = "o invisível, aquilo que está oculto",
            root = "غ ي ب",
            grammar = "substantivo",
            category = "Crença",
            reference = "Al-Baqarah 2:3",
            verseArabic = "ٱلَّذِينَ يُؤْمِنُونَ بِٱلْغَيْبِ",
            verseMeaning = "Aqueles que creem no que está além da percepção.",
            insight = "Ghayb designa realidades não alcançadas diretamente pelos sentidos.",
        ),
        QuranWord(
            id = "salah",
            arabic = "ٱلصَّلَوٰةَ",
            lemma = "صَلَاة",
            transliteration = "aṣ-ṣalāh",
            meaning = "a oração ritual",
            root = "ص ل و",
            grammar = "substantivo",
            category = "Adoração",
            reference = "Al-Baqarah 2:3",
            verseArabic = "وَيُقِيمُونَ ٱلصَّلَوٰةَ",
            verseMeaning = "E estabelecem a oração.",
            insight = "O verbo que acompanha ṣalāh é frequentemente aqāma: estabelecer e manter com constância.",
        ),
        QuranWord(
            id = "razaqna",
            arabic = "رَزَقْنَٰهُمْ",
            lemma = "رَزَقَ",
            transliteration = "razaqnāhum",
            meaning = "Nós lhes concedemos sustento",
            root = "ر ز ق",
            grammar = "verbo + pronome objeto",
            category = "Ações",
            reference = "Al-Baqarah 2:3",
            verseArabic = "وَمِمَّا رَزَقْنَٰهُمْ يُنفِقُونَ",
            verseMeaning = "E compartilham daquilo que lhes concedemos como sustento.",
            insight = "Rizq abrange provisão material e outros benefícios concedidos.",
        ),
        QuranWord(
            id = "yuminun",
            arabic = "يُؤْمِنُونَ",
            lemma = "آمَنَ",
            transliteration = "yuʾminūn",
            meaning = "eles creem, confiam",
            root = "أ م ن",
            grammar = "verbo, 3ª pessoa masc. plural",
            category = "Crença",
            reference = "Al-Baqarah 2:3",
            verseArabic = "ٱلَّذِينَ يُؤْمِنُونَ بِٱلْغَيْبِ",
            verseMeaning = "Aqueles que creem no que está além da percepção.",
            insight = "A raiz ʾ-m-n também carrega os sentidos de segurança, confiança e fidelidade.",
        ),
        QuranWord(
            id = "akhirah",
            arabic = "ٱلْءَاخِرَةِ",
            lemma = "آخِرَة",
            transliteration = "al-ākhirah",
            meaning = "a vida futura, o Além",
            root = "أ خ ر",
            grammar = "substantivo",
            category = "Crença",
            reference = "Al-Baqarah 2:4",
            verseArabic = "وَبِٱلْءَاخِرَةِ هُمْ يُوقِنُونَ",
            verseMeaning = "E têm certeza da vida futura.",
            insight = "A raiz indica aquilo que vem por último ou depois, em contraste com dunyā, a vida próxima.",
        ),
        QuranWord(
            id = "muflihun",
            arabic = "ٱلْمُفْلِحُونَ",
            lemma = "مُفْلِح",
            transliteration = "al-mufliḥūn",
            meaning = "os bem-sucedidos, os que prosperam",
            root = "ف ل ح",
            grammar = "particípio ativo, plural",
            category = "Caráter",
            reference = "Al-Baqarah 2:5",
            verseArabic = "وَأُو۟لَٰٓئِكَ هُمُ ٱلْمُفْلِحُونَ",
            verseMeaning = "E são esses os verdadeiramente bem-sucedidos.",
            insight = "Falāḥ é sucesso amplo e duradouro, não apenas ganho imediato.",
        ),
        QuranWord(
            id = "haqq",
            arabic = "ٱلْحَقُّ",
            lemma = "حَقّ",
            transliteration = "al-ḥaqq",
            meaning = "a verdade, o que é real e devido",
            root = "ح ق ق",
            grammar = "substantivo",
            category = "Fundamentos",
            reference = "Al-Baqarah 2:147",
            verseArabic = "ٱلْحَقُّ مِن رَّبِّكَ",
            verseMeaning = "A verdade vem do teu Senhor.",
            insight = "Ḥaqq pode indicar verdade, realidade estabelecida, direito ou dever.",
        ),
        QuranWord(
            id = "sabirin",
            arabic = "ٱلصَّابِرِينَ",
            lemma = "صَابِر",
            transliteration = "aṣ-ṣābirīn",
            meaning = "os pacientes e perseverantes",
            root = "ص ب ر",
            grammar = "particípio ativo, plural",
            category = "Caráter",
            reference = "Al-Baqarah 2:153",
            verseArabic = "إِنَّ ٱللَّهَ مَعَ ٱلصَّابِرِينَ",
            verseMeaning = "Deus está com os pacientes e perseverantes.",
            insight = "Ṣabr inclui paciência, firmeza e autocontrole diante da dificuldade.",
        ),
    )

    private data class EnglishCuratedText(
        val meaning: String,
        val grammar: String,
        val category: String,
        val verseMeaning: String,
        val insight: String,
    )

    private val englishCuratedText = mapOf(
        "allah" to EnglishCuratedText(
            "God; the divine proper name",
            "proper noun",
            "Foundations",
            "In the name of God, the Entirely Merciful, the Especially Merciful.",
            "In the Quran, the name Allah identifies the one God and has no plural form.",
        ),
        "rabb" to EnglishCuratedText(
            "Lord, caretaker, and sustainer",
            "noun",
            "Foundations",
            "All praise belongs to God, Lord of all worlds.",
            "Rabb brings together the ideas of authority, care, upbringing, and sustenance.",
        ),
        "rahman" to EnglishCuratedText(
            "the Entirely Merciful",
            "intensive noun",
            "Attributes",
            "The Entirely Merciful, the Especially Merciful.",
            "The root r-ḥ-m is connected to mercy, compassion, and care.",
        ),
        "din" to EnglishCuratedText(
            "recompense, judgment; also religion",
            "noun",
            "Foundations",
            "Sovereign of the Day of Recompense.",
            "Context determines the sense. In this verse, dīn indicates accountability and recompense.",
        ),
        "nabudu" to EnglishCuratedText(
            "we worship and serve",
            "verb, first-person plural",
            "Worship",
            "You alone we worship, and You alone we ask for help.",
            "The same root forms ʿabd, servant. Worship includes conscious submission and service.",
        ),
        "kitab" to EnglishCuratedText(
            "the Book, the Scripture",
            "noun",
            "Revelation",
            "This is the Book in which there is no doubt.",
            "The root k-t-b involves writing, recording, and prescribing.",
        ),
        "huda" to EnglishCuratedText(
            "guidance",
            "verbal noun",
            "Revelation",
            "Guidance for those mindful of God.",
            "The root h-d-y conveys gently leading someone to the right path.",
        ),
        "muttaqin" to EnglishCuratedText(
            "those mindful of God, those who guard themselves",
            "active participle, plural",
            "Character",
            "Guidance for those who cultivate mindfulness and guard themselves before God.",
            "Taqwā comes from the idea of protection: acting with awareness of spiritual consequences.",
        ),
        "ghayb" to EnglishCuratedText(
            "the unseen, that which is hidden",
            "noun",
            "Belief",
            "Those who believe in what lies beyond perception.",
            "Ghayb refers to realities that cannot be reached directly by the senses.",
        ),
        "salah" to EnglishCuratedText(
            "the ritual prayer",
            "noun",
            "Worship",
            "And they establish prayer.",
            "The verb often paired with ṣalāh is aqāma: to establish and maintain consistently.",
        ),
        "razaqna" to EnglishCuratedText(
            "We have provided for them",
            "verb + object pronoun",
            "Actions",
            "And they share from what We have provided for them.",
            "Rizq includes material provision and other benefits that are granted.",
        ),
        "yuminun" to EnglishCuratedText(
            "they believe, they trust",
            "verb, third-person masculine plural",
            "Belief",
            "Those who believe in what lies beyond perception.",
            "The root ʾ-m-n also carries the senses of safety, trust, and faithfulness.",
        ),
        "akhirah" to EnglishCuratedText(
            "the afterlife, the Hereafter",
            "noun",
            "Belief",
            "And they are certain of the Hereafter.",
            "The root indicates what comes last or afterward, in contrast with dunyā, the nearer life.",
        ),
        "muflihun" to EnglishCuratedText(
            "the successful, those who prosper",
            "active participle, plural",
            "Character",
            "And it is they who are truly successful.",
            "Falāḥ is broad and lasting success, not merely immediate gain.",
        ),
        "haqq" to EnglishCuratedText(
            "the truth, what is real and due",
            "noun",
            "Foundations",
            "The truth comes from your Lord.",
            "Ḥaqq can indicate truth, established reality, a right, or a duty.",
        ),
        "sabirin" to EnglishCuratedText(
            "the patient and steadfast",
            "active participle, plural",
            "Character",
            "God is with the patient and steadfast.",
            "Ṣabr includes patience, steadfastness, and self-control in difficulty.",
        ),
    )

    val selectableSurahs: List<QuranSurah> = GeneratedQuranSurahs.all

    private val curatedAudioLocations = mapOf(
        "allah" to QuranWordAudioLocation(1, 1, 2),
        "rabb" to QuranWordAudioLocation(1, 2, 3),
        "rahman" to QuranWordAudioLocation(1, 3, 1),
        "din" to QuranWordAudioLocation(1, 4, 3),
        "nabudu" to QuranWordAudioLocation(1, 5, 2),
        "kitab" to QuranWordAudioLocation(2, 2, 2),
        "huda" to QuranWordAudioLocation(2, 2, 6),
        "muttaqin" to QuranWordAudioLocation(2, 2, 7),
        "ghayb" to QuranWordAudioLocation(2, 3, 3),
        "salah" to QuranWordAudioLocation(2, 3, 5),
        "razaqna" to QuranWordAudioLocation(2, 3, 7),
        "yuminun" to QuranWordAudioLocation(2, 3, 2),
        "akhirah" to QuranWordAudioLocation(2, 4, 10),
        "muflihun" to QuranWordAudioLocation(2, 5, 8),
        "haqq" to QuranWordAudioLocation(2, 147, 1),
        "sabirin" to QuranWordAudioLocation(2, 153, 10),
    )

    private val fallbackWords = withAudioLocations(
        curatedPortugueseWords + GeneratedQuranVocabulary.words,
    )

    @Volatile
    private var corpusWords: List<QuranWord> = fallbackWords

    @Volatile
    private var searchIndex: Map<String, SearchEntry>? = buildSearchIndex(fallbackWords)

    @Volatile
    private var frequentIndex: List<QuranWord> = fallbackWords.filter(QuranWord::isFrequent)

    @Volatile
    private var rankedFrequencyIndex: List<QuranWord> = buildRankedFrequencyIndex(fallbackWords)

    @Volatile
    private var surahIndex: Map<Int, List<QuranWord>> =
        fallbackWords.filter { it.surahNumber != null }.groupBy { requireNotNull(it.surahNumber) }

    @Volatile
    private var referenceIndex: Map<String, List<QuranWord>>? = fallbackWords.groupBy(QuranWord::reference)

    @Volatile
    private var lemmaIndex: Map<String, List<QuranWord>>? = fallbackWords.groupBy(::lemmaKey)

    @Volatile
    private var initializedLanguage: AppLanguage? = null

    val words: List<QuranWord> get() = corpusWords

    val frequentWords: List<QuranWord> get() = frequentIndex

    @Synchronized
    fun initialize(input: InputStream, language: AppLanguage) {
        if (initializedLanguage == language) {
            input.close()
            return
        }
        val imported = input.use { VocabularyAssetLoader.load(it, language) }
        val loadedWords = curatedWords(language) + imported
        searchIndex = null
        referenceIndex = null
        lemmaIndex = null
        corpusWords = loadedWords
        frequentIndex = imported.filter(QuranWord::isFrequent)
        rankedFrequencyIndex = buildRankedFrequencyIndex(imported)
        surahIndex = imported
            .filter { it.surahNumber != null }
            .groupBy { requireNotNull(it.surahNumber) }
        initializedLanguage = language
    }

    fun prepareDeferredIndexes() {
        val source = words
        if (searchIndex != null && referenceIndex != null && lemmaIndex != null) return

        val preparedSearchIndex = buildSearchIndex(source)
        val preparedReferenceIndex = source.groupBy(QuranWord::reference)
        val preparedLemmaIndex = source.groupBy(::lemmaKey)
        synchronized(this) {
            if (corpusWords !== source) return
            searchIndex = preparedSearchIndex
            referenceIndex = preparedReferenceIndex
            lemmaIndex = preparedLemmaIndex
        }
    }

    private fun curatedWords(language: AppLanguage): List<QuranWord> {
        val localized = if (language == AppLanguage.Portuguese) {
            curatedPortugueseWords
        } else {
            curatedPortugueseWords.map { word ->
                val text = requireNotNull(englishCuratedText[word.id])
                word.copy(
                    meaning = text.meaning,
                    grammar = text.grammar,
                    category = text.category,
                    verseMeaning = text.verseMeaning,
                    insight = text.insight,
                )
            }
        }
        return withAudioLocations(localized)
    }

    private fun withAudioLocations(words: List<QuranWord>): List<QuranWord> = words.map { word ->
        if (word.audioLocation != null) {
            word
        } else {
            word.copy(
                audioLocation = requireNotNull(
                    curatedAudioLocations[word.id]
                        ?: QuranWordAudioLocationResolver.resolve(
                            id = word.id,
                            arabic = word.arabic,
                            reference = word.reference,
                            verseArabic = word.verseArabic,
                        ),
                ) { "No Quran.com word-audio location for fallback card ${word.id}" },
            )
        }
    }

    fun wordsFor(
        scope: StudyScope,
        selectedSurahs: Set<Int>,
        customStudyIds: Set<String> = emptySet(),
    ): List<QuranWord> {
        val selected = when (scope) {
            StudyScope.All -> words
            StudyScope.Frequent50 -> rankedFrequencyIndex.take(50)
            StudyScope.Frequent -> rankedFrequencyIndex.take(100)
            StudyScope.Frequent300 -> rankedFrequencyIndex.take(300)
            StudyScope.Frequent500 -> rankedFrequencyIndex.take(500)
            StudyScope.Prayer -> distinctByLemma(
                curatedWords(initializedLanguage ?: AppLanguage.Portuguese) +
                    listOf(1, 112, 113, 114).flatMap { surahIndex[it].orEmpty() },
            )
            StudyScope.ShortSurahs -> distinctByLemma(
                (101..114).flatMap { surahIndex[it].orEmpty() },
            )
            StudyScope.Custom -> words.filter { it.id in customStudyIds }
            StudyScope.Surahs -> selectedSurahs.sorted().flatMap { surahIndex[it].orEmpty() }
        }
        return when (scope) {
            StudyScope.Custom -> selected
            else -> selected.ifEmpty { words }
        }
    }

    private fun buildRankedFrequencyIndex(source: List<QuranWord>): List<QuranWord> {
        val frequent = source.filter(QuranWord::isFrequent)
        val frequentKeys = frequent.mapTo(mutableSetOf(), ::lemmaKey)
        val additional = source
            .asSequence()
            .filter { it.surahNumber != null && lemmaKey(it) !in frequentKeys }
            .groupBy(::lemmaKey)
            .mapNotNull { (_, forms) ->
                forms.maxByOrNull(QuranWord::frequency)?.copy(
                    frequency = forms.sumOf(QuranWord::frequency),
                )
            }
            .sortedByDescending(QuranWord::frequency)
        return distinctByLemma(frequent + additional)
    }

    private fun distinctByLemma(source: List<QuranWord>): List<QuranWord> =
        source.distinctBy(::lemmaKey)

    private fun lemmaKey(word: QuranWord): String =
        Normalizer.normalize(word.lemma.ifBlank { word.arabic }, Normalizer.Form.NFD)
            .replace(ARABIC_MARKS, "")
            .replace('ٱ', 'ا')
            .trim()

    fun wordFor(
        date: LocalDate = LocalDate.now(),
        source: List<QuranWord> = words,
    ): QuranWord = source[Math.floorMod(date.toEpochDay().toInt(), source.size)]

    fun wordAtSequence(sequence: Int, source: List<QuranWord> = words): QuranWord =
        source[Math.floorMod(sequence, source.size)]

    fun search(query: String, source: List<QuranWord> = words): List<QuranWord> {
        val term = normalizeSearchText(query)
        if (term.isEmpty()) return source
        val compactArabicTerm = term
            .takeIf(::containsArabicLetter)
            ?.filterNot(Char::isWhitespace)
        return source.filter { word ->
            val entry = searchIndex?.get(word.id) ?: searchEntry(word)
            entry.text.contains(term) ||
                (compactArabicTerm != null && entry.compactRoot.contains(compactArabicTerm))
        }
    }

    fun verseTokens(word: QuranWord): List<VerseToken> = VerseExplorer.buildTokens(
        word.verseArabic,
        (referenceIndex?.get(word.reference) ?: words.filter { it.reference == word.reference })
            .filter { it.verseArabic == word.verseArabic },
    )

    fun concordance(word: QuranWord, limit: Int = 8): List<QuranWord> =
        (lemmaIndex?.get(lemmaKey(word)) ?: words.filter { lemmaKey(it) == lemmaKey(word) })
            .asSequence()
            .filterNot { it.id == word.id }
            .distinctBy(QuranWord::reference)
            .take(limit.coerceIn(1, 20))
            .toList()

    fun corpusIdentity(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        words.forEach { word ->
            digest.update(word.id.toByteArray(Charsets.UTF_8))
            digest.update('\n'.code.toByte())
        }
        val shortHash = digest.digest().take(8).joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }
        return "kalima-quran-v2-${words.size}-$shortHash"
    }

    private fun buildSearchIndex(source: List<QuranWord>): Map<String, SearchEntry> =
        source.associate { word -> word.id to searchEntry(word) }

    private fun searchEntry(word: QuranWord): SearchEntry {
        val fields = listOf(
            word.arabic,
            word.lemma,
            word.transliteration,
            word.meaning,
            word.root,
            word.category,
            word.reference,
        )
        return SearchEntry(
            text = fields.joinToString(SEARCH_FIELD_SEPARATOR, transform = ::normalizeSearchText),
            compactRoot = normalizeSearchText(word.root).filterNot(Char::isWhitespace),
        )
    }

    private fun normalizeSearchText(value: String): String = buildString(value.length) {
        Normalizer.normalize(value, Normalizer.Form.NFD).forEach { character ->
            val normalized = when {
                character == 'ـ' || character.isCombiningMark() -> null
                character in "أإآٱ" -> 'ا'
                character == 'ى' -> 'ي'
                else -> character
            }
            if (normalized != null) append(normalized)
        }
    }.lowercase(Locale.ROOT).trim().replace(SEARCH_WHITESPACE, " ")

    private fun containsArabicLetter(value: String): Boolean = value.any { character ->
        Character.UnicodeScript.of(character.code) == Character.UnicodeScript.ARABIC &&
            Character.isLetter(character)
    }

    private fun Char.isCombiningMark(): Boolean = when (Character.getType(this)) {
        Character.NON_SPACING_MARK.toInt(),
        Character.COMBINING_SPACING_MARK.toInt(),
        Character.ENCLOSING_MARK.toInt(),
        -> true

        else -> false
    }

    private data class SearchEntry(
        val text: String,
        val compactRoot: String,
    )

}

data class QuranSurah(
    val number: Int,
    val arabicName: String,
    val transliteratedName: String,
)
