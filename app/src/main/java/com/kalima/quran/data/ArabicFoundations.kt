package com.kalima.quran.data

import java.time.Instant

data class FoundationSymbol(
    val arabic: String,
    /** The letter name, used only while introducing the letter. */
    val transliteration: String,
    val spokenArabic: String = arabic,
    /** The usable consonant sound rather than the letter name. */
    val sound: String,
    val audioSlug: String,
    val connectsToFollowing: Boolean = true,
) {
    val isolatedForm: String get() = arabic
    val initialForm: String? get() = if (connectsToFollowing) "${arabic}ـ" else null
    val medialForm: String? get() = if (connectsToFollowing) "ـ${arabic}ـ" else null
    val finalForm: String get() = "ـ$arabic"
    val soundAudioResourceName: String get() = "arabic_sound_$audioSlug"
    val masteryId: String get() = arabic.codePoints().toArray().joinToString("-")
}

data class QuranicDecodingMilestone(
    val word: String,
    val transliteration: String,
    val meaningEnglish: String,
    val meaningPortuguese: String,
    val segments: List<String>,
    /** Drop a matching file in res/raw; TTS is used until then. */
    val audioResourceName: String,
)

data class AlphabetLesson(
    val symbols: List<FoundationSymbol>,
    val milestone: QuranicDecodingMilestone,
)

enum class AlphabetMasteryDimension {
    IsolatedForm,
    ConnectedForm,
    Sound,
    VowelledReading,
}

enum class AlphabetQuestionType {
    GlyphToSound,
    AudioToGlyph,
    ConnectedToGlyph,
    VowelledToSound,
}

data class AlphabetPracticeOption(
    val text: String,
    val isArabic: Boolean,
)

data class AlphabetPracticeQuestion(
    val symbol: FoundationSymbol,
    val type: AlphabetQuestionType,
    val promptArabic: String?,
    val options: List<AlphabetPracticeOption>,
    val correctOptionIndex: Int,
    val spokenArabic: String? = null,
    val audioResourceName: String? = null,
) {
    val masteryKey: String
        get() = ArabicFoundations.masteryKey(symbol, dimension)

    val dimension: AlphabetMasteryDimension
        get() = when (type) {
            AlphabetQuestionType.GlyphToSound -> AlphabetMasteryDimension.Sound
            AlphabetQuestionType.AudioToGlyph -> AlphabetMasteryDimension.IsolatedForm
            AlphabetQuestionType.ConnectedToGlyph -> AlphabetMasteryDimension.ConnectedForm
            AlphabetQuestionType.VowelledToSound -> AlphabetMasteryDimension.VowelledReading
        }
}

data class NumberLesson(
    val westernDigit: Int,
    val arabicDigit: String,
    val arabicName: String,
    val transliteration: String,
    val audioResourceName: String,
)

/** A permanent, quick-reference row for one of the 28 Arabic letters. */
data class ArabicLetterReference(
    val letter: FoundationSymbol,
    val vowelVariants: List<FoundationSymbol>,
)

object ArabicFoundations {
    const val alphabetReferencePageSize = 4

    private fun letter(
        arabic: String,
        name: String,
        spokenName: String,
        sound: String,
        slug: String,
        connectsToFollowing: Boolean = true,
    ) = FoundationSymbol(
        arabic = arabic,
        transliteration = name,
        spokenArabic = spokenName,
        sound = sound,
        audioSlug = slug,
        connectsToFollowing = connectsToFollowing,
    )

    private val alif = letter("ا", "alif", "أَلِف", "ʾ", "alif", false)
    private val baa = letter("ب", "bāʾ", "بَاء", "b", "baa")
    private val taa = letter("ت", "tāʾ", "تَاء", "t", "taa")
    private val thaa = letter("ث", "thāʾ", "ثَاء", "th", "thaa")
    private val jiim = letter("ج", "jīm", "جِيم", "j", "jiim")
    private val haaPharyngeal = letter("ح", "ḥāʾ", "حَاء", "ḥ", "haa_pharyngeal")
    private val khaa = letter("خ", "khāʾ", "خَاء", "kh", "khaa")
    private val daal = letter("د", "dāl", "دَال", "d", "daal", false)
    private val dhaal = letter("ذ", "dhāl", "ذَال", "dh", "dhaal", false)
    private val raa = letter("ر", "rāʾ", "رَاء", "r", "raa", false)
    private val zaay = letter("ز", "zāy", "زَاي", "z", "zaay", false)
    private val siin = letter("س", "sīn", "سِين", "s", "siin")
    private val shiin = letter("ش", "shīn", "شِين", "sh", "shiin")
    private val saad = letter("ص", "ṣād", "صَاد", "ṣ", "saad")
    private val daad = letter("ض", "ḍād", "ضَاد", "ḍ", "daad")
    private val taaEmphatic = letter("ط", "ṭāʾ", "طَاء", "ṭ", "taa_emphatic")
    private val zaaEmphatic = letter("ظ", "ẓāʾ", "ظَاء", "ẓ", "zaa_emphatic")
    private val ayn = letter("ع", "ʿayn", "عَيْن", "ʿ", "ayn")
    private val ghayn = letter("غ", "ghayn", "غَيْن", "gh", "ghayn")
    private val faa = letter("ف", "fāʾ", "فَاء", "f", "faa")
    private val qaaf = letter("ق", "qāf", "قَاف", "q", "qaaf")
    private val kaaf = letter("ك", "kāf", "كَاف", "k", "kaaf")
    private val laam = letter("ل", "lām", "لَام", "l", "laam")
    private val miim = letter("م", "mīm", "مِيم", "m", "miim")
    private val nuun = letter("ن", "nūn", "نُون", "n", "nuun")
    private val haa = letter("ه", "hāʾ", "هَاء", "h", "haa")
    private val waaw = letter("و", "wāw", "وَاو", "w", "waaw", false)
    private val yaa = letter("ي", "yāʾ", "يَاء", "y", "yaa")

    /**
     * Seven variable-sized groups keep commonly confused shapes together. This retains the
     * existing seven-step progress contract while changing practice from alphabetic chunks to
     * direct visual comparison.
     */
    val alphabetLessons: List<AlphabetLesson> = listOf(
        AlphabetLesson(
            symbols = listOf(baa, taa, thaa, nuun),
            milestone = QuranicDecodingMilestone(
                "تَبَّتْ", "tabbat", "perished", "pereceu",
                listOf("تَ", "بَّتْ"), "arabic_word_tabbat",
            ),
        ),
        AlphabetLesson(
            symbols = listOf(jiim, haaPharyngeal, khaa, ayn, ghayn),
            milestone = QuranicDecodingMilestone(
                "نَحْنُ", "naḥnu", "we", "nós",
                listOf("نَحْ", "نُ"), "arabic_word_nahnu",
            ),
        ),
        AlphabetLesson(
            symbols = listOf(daal, dhaal, raa, zaay),
            milestone = QuranicDecodingMilestone(
                "رَبّ", "rabb", "Lord", "Senhor",
                listOf("رَ", "بّ"), "arabic_word_rabb",
            ),
        ),
        AlphabetLesson(
            symbols = listOf(siin, shiin, saad, daad),
            milestone = QuranicDecodingMilestone(
                "شَرّ", "sharr", "evil", "mal",
                listOf("شَ", "رّ"), "arabic_word_sharr",
            ),
        ),
        AlphabetLesson(
            symbols = listOf(taaEmphatic, zaaEmphatic, faa, qaaf),
            milestone = QuranicDecodingMilestone(
                "صِرَاط", "ṣirāṭ", "path", "caminho",
                listOf("صِ", "رَا", "ط"), "arabic_word_siraat",
            ),
        ),
        AlphabetLesson(
            symbols = listOf(kaaf, laam, miim, haa),
            milestone = QuranicDecodingMilestone(
                "قُلْ", "qul", "say", "dize",
                listOf("قُ", "لْ"), "arabic_word_qul",
            ),
        ),
        AlphabetLesson(
            symbols = listOf(alif, waaw, yaa),
            milestone = QuranicDecodingMilestone(
                "هُوَ", "huwa", "he", "ele",
                listOf("هُ", "وَ"), "arabic_word_huwa",
            ),
        ),
    )

    private val vowelMarks = listOf("َ" to "a", "ِ" to "i", "ُ" to "u")

    fun practiceQuestions(lessonIndex: Int): List<AlphabetPracticeQuestion> {
        val introduced = alphabetLessons.take(lessonIndex + 1).flatMap(AlphabetLesson::symbols)
        return alphabetLessons[lessonIndex].symbols.flatMap { symbol ->
            questionsFor(symbol, introduced)
        }
    }

    fun cumulativePracticeQuestions(
        lessonIndex: Int,
        schedules: Map<String, ReviewSchedule>,
        now: Instant,
    ): List<AlphabetPracticeQuestion> {
        val introduced = alphabetLessons.take(lessonIndex + 1).flatMap(AlphabetLesson::symbols)
        val current = alphabetLessons[lessonIndex].symbols.toSet()
        return introduced.flatMap { symbol -> questionsFor(symbol, introduced) }
            .filter { question ->
                question.symbol in current || schedules[question.masteryKey]?.isDue(now) != false
            }
    }

    private fun questionsFor(
        symbol: FoundationSymbol,
        candidates: List<FoundationSymbol>,
    ): List<AlphabetPracticeQuestion> {
        val vowelIndex = symbol.arabic.codePointAt(0) % vowelMarks.size
        val (mark, vowel) = vowelMarks[vowelIndex]
        val vowelPrompt = if (symbol == alif) when (vowel) {
            "i" -> "إِ"
            else -> "أ$mark"
        } else {
            symbol.arabic + mark
        }
        return listOf(
            question(
                symbol, AlphabetQuestionType.GlyphToSound, symbol.arabic, candidates,
                optionText = FoundationSymbol::transliteration, isArabic = false,
            ),
            question(
                symbol, AlphabetQuestionType.AudioToGlyph, null, candidates,
                optionText = FoundationSymbol::arabic, isArabic = true,
                spokenArabic = vowelPrompt,
                audioResourceName = "${symbol.soundAudioResourceName}_$vowel",
            ),
            question(
                symbol, AlphabetQuestionType.ConnectedToGlyph,
                symbol.medialForm ?: symbol.finalForm, candidates,
                optionText = FoundationSymbol::arabic, isArabic = true,
            ),
            question(
                symbol, AlphabetQuestionType.VowelledToSound, vowelPrompt, candidates,
                optionText = { candidate -> candidate.sound + vowel },
                isArabic = false,
            ),
        )
    }

    private fun question(
        symbol: FoundationSymbol,
        type: AlphabetQuestionType,
        promptArabic: String?,
        candidates: List<FoundationSymbol>,
        optionText: (FoundationSymbol) -> String,
        isArabic: Boolean,
        spokenArabic: String? = null,
        audioResourceName: String? = null,
    ): AlphabetPracticeQuestion {
        val distractors = candidates
            .filterNot { it == symbol }
            .distinctBy(optionText)
            .sortedBy { candidate ->
                Math.floorMod(
                    candidate.arabic.codePointAt(0) - symbol.arabic.codePointAt(0),
                    0x110000,
                )
            }
            .take(3)
            .toMutableList()
        val optionSymbols = (distractors + symbol).toMutableList()
        val correctIndex = Math.floorMod(symbol.arabic.codePointAt(0), optionSymbols.size)
        optionSymbols.remove(symbol)
        optionSymbols.add(correctIndex, symbol)
        return AlphabetPracticeQuestion(
            symbol = symbol,
            type = type,
            promptArabic = promptArabic,
            options = optionSymbols.map { AlphabetPracticeOption(optionText(it), isArabic) },
            correctOptionIndex = correctIndex,
            spokenArabic = spokenArabic,
            audioResourceName = audioResourceName,
        )
    }

    fun masteryKey(
        symbol: FoundationSymbol,
        dimension: AlphabetMasteryDimension,
    ): String = "alphabet:${symbol.masteryId}:${dimension.name}"

    val allMasteryKeys: Set<String> by lazy {
        alphabetLessons.flatMap(AlphabetLesson::symbols).flatMap { symbol ->
            AlphabetMasteryDimension.entries.map { masteryKey(symbol, it) }
        }.toSet()
    }

    val numberLessons: List<NumberLesson> = listOf(
        NumberLesson(0, "٠", "صِفْر", "ṣifr", "arabic_number_0_sifr"),
        NumberLesson(1, "١", "وَاحِد", "wāḥid", "arabic_number_1_wahid"),
        NumberLesson(2, "٢", "اِثْنَان", "ithnān", "arabic_number_2_ithnan"),
        NumberLesson(3, "٣", "ثَلَاثَة", "thalātha", "arabic_number_3_thalatha"),
        NumberLesson(4, "٤", "أَرْبَعَة", "arbaʿa", "arabic_number_4_arbaa"),
        NumberLesson(5, "٥", "خَمْسَة", "khamsa", "arabic_number_5_khamsa"),
        NumberLesson(6, "٦", "سِتَّة", "sitta", "arabic_number_6_sitta"),
        NumberLesson(7, "٧", "سَبْعَة", "sabʿa", "arabic_number_7_sabaa"),
        NumberLesson(8, "٨", "ثَمَانِيَة", "thamāniya", "arabic_number_8_thamaniya"),
        NumberLesson(9, "٩", "تِسْعَة", "tisʿa", "arabic_number_9_tisaa"),
    )

    /** Each row uses fatḥa, kasra, ḍamma, and sukūn, in that order. */
    val alphabetReference: List<ArabicLetterReference> by lazy {
        val sounds = listOf("a" to "َ", "i" to "ِ", "u" to "ُ", "" to "ْ")
        val alphabeticOrder = listOf(
            alif, baa, taa, thaa, jiim, haaPharyngeal, khaa, daal, dhaal, raa,
            zaay, siin, shiin, saad, daad, taaEmphatic, zaaEmphatic, ayn, ghayn,
            faa, qaaf, kaaf, laam, miim, nuun, haa, waaw, yaa,
        )
        alphabeticOrder.map { letter ->
            ArabicLetterReference(
                letter = letter,
                vowelVariants = if (letter == alif) {
                    listOf(
                        FoundationSymbol("أَ", "ʾa", sound = "ʾa", audioSlug = "alif_a"),
                        FoundationSymbol("إِ", "ʾi", sound = "ʾi", audioSlug = "alif_i"),
                        FoundationSymbol("أُ", "ʾu", sound = "ʾu", audioSlug = "alif_u"),
                        FoundationSymbol("أْ", "ʾ", sound = "ʾ", audioSlug = "alif_sukun"),
                    )
                } else {
                    sounds.map { (vowel, mark) ->
                        FoundationSymbol(
                            arabic = letter.arabic + mark,
                            transliteration = letter.sound + vowel,
                            spokenArabic = letter.arabic + mark,
                            sound = letter.sound + vowel,
                            audioSlug = "${letter.audioSlug}_${vowel.ifEmpty { "sukun" }}",
                        )
                    }
                },
            )
        }
    }

    val alphabetLessonCount: Int get() = alphabetLessons.size
    val numberLessonCount: Int get() = numberLessons.size
}

val StudyProgress.needsAlphabetFoundation: Boolean
    get() = alphabetFoundationRequired && hasAlphabetFoundationLesson

val StudyProgress.hasAlphabetFoundationLesson: Boolean
    get() = alphabetCourseRequested &&
        completedAlphabetLessons < ArabicFoundations.alphabetLessonCount

val StudyProgress.hasNumberFoundationLesson: Boolean
    get() = numberCourseRequested &&
        completedNumberLessons < ArabicFoundations.numberLessonCount

fun StudyProgress.startAlphabetFoundation(): StudyProgress = copy(
    alphabetCourseRequested = true,
    completedAlphabetLessons = completedAlphabetLessons
        .takeIf { it in 0 until ArabicFoundations.alphabetLessonCount }
        ?: 0,
)

fun StudyProgress.skipAlphabetFoundation(): StudyProgress = copy(
    alphabetCourseRequested = false,
    alphabetFoundationRequired = false,
)

fun StudyProgress.startNumberFoundation(): StudyProgress = copy(
    numberCourseRequested = true,
    completedNumberLessons = completedNumberLessons
        .takeIf { it in 0 until ArabicFoundations.numberLessonCount }
        ?: 0,
)
