package com.kalima.quran.data

import android.content.Context
import java.time.LocalDate

object WordRepository {
    private val curatedWords: List<QuranWord> = listOf(
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

    val selectableSurahs: List<QuranSurah> = GeneratedQuranSurahs.all

    private val fallbackWords = curatedWords + GeneratedQuranVocabulary.words

    @Volatile
    private var corpusWords: List<QuranWord> = fallbackWords

    @Volatile
    private var frequentIndex: List<QuranWord> = fallbackWords.filter(QuranWord::isFrequent)

    @Volatile
    private var surahIndex: Map<Int, List<QuranWord>> =
        fallbackWords.filter { it.surahNumber != null }.groupBy { requireNotNull(it.surahNumber) }

    @Volatile
    private var initialized = false

    val words: List<QuranWord> get() = corpusWords

    val frequentWords: List<QuranWord> get() = frequentIndex

    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return
        val imported = context.assets.open(VocabularyAssetLoader.ASSET_NAME).use(VocabularyAssetLoader::load)
        corpusWords = curatedWords + imported
        frequentIndex = imported.filter(QuranWord::isFrequent)
        surahIndex = imported
            .filter { it.surahNumber != null }
            .groupBy { requireNotNull(it.surahNumber) }
        initialized = true
    }

    fun wordsFor(scope: StudyScope, selectedSurahs: Set<Int>): List<QuranWord> =
        when (scope) {
            StudyScope.All -> words
            StudyScope.Frequent -> frequentWords
            StudyScope.Surahs -> selectedSurahs.sorted().flatMap { surahIndex[it].orEmpty() }
        }.ifEmpty { words }

    fun wordFor(
        date: LocalDate = LocalDate.now(),
        source: List<QuranWord> = words,
    ): QuranWord = source[Math.floorMod(date.toEpochDay().toInt(), source.size)]

    fun wordAtSequence(sequence: Int, source: List<QuranWord> = words): QuranWord =
        source[Math.floorMod(sequence, source.size)]

    fun search(query: String, source: List<QuranWord> = words): List<QuranWord> {
        val term = query.trim().lowercase()
        if (term.isEmpty()) return source
        return source.filter { word ->
            listOf(
                word.arabic,
                word.lemma,
                word.transliteration,
                word.meaning,
                word.root,
                word.category,
                word.reference,
            ).any { it.lowercase().contains(term) }
        }
    }
}

data class QuranSurah(
    val number: Int,
    val arabicName: String,
    val transliteratedName: String,
)
