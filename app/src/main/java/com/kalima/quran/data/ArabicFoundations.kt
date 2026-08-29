package com.kalima.quran.data

data class FoundationSymbol(
    val arabic: String,
    val transliteration: String,
    val spokenArabic: String = arabic,
)

data class AlphabetLesson(
    val symbols: List<FoundationSymbol>,
) {
    fun practiceQuestions(): List<AlphabetPracticeQuestion> = symbols.mapIndexed { index, symbol ->
        val correctOptionIndex = (index * 3 + 1) % symbols.size
        val distractors = symbols
            .asSequence()
            .filterNot { it == symbol }
            .map(FoundationSymbol::transliteration)
            .toMutableList()
            .apply { add(correctOptionIndex, symbol.transliteration) }
        AlphabetPracticeQuestion(
            symbol = symbol,
            options = distractors,
            correctOptionIndex = correctOptionIndex,
        )
    }
}

data class AlphabetPracticeQuestion(
    val symbol: FoundationSymbol,
    val options: List<String>,
    val correctOptionIndex: Int,
)

data class NumberLesson(
    val westernDigit: Int,
    val arabicDigit: String,
    val arabicName: String,
    val transliteration: String,
)

/** A permanent, quick-reference row for one of the 28 Arabic letters. */
data class ArabicLetterReference(
    val letter: FoundationSymbol,
    val vowelVariants: List<FoundationSymbol>,
)

object ArabicFoundations {
    const val alphabetReferencePageSize = 4

    val alphabetLessons: List<AlphabetLesson> = listOf(
        AlphabetLesson(
            listOf(
                FoundationSymbol("ا", "alif", "أَلِف"),
                FoundationSymbol("ب", "bāʾ", "بَاء"),
                FoundationSymbol("ت", "tāʾ", "تَاء"),
                FoundationSymbol("ث", "thāʾ", "ثَاء"),
            ),
        ),
        AlphabetLesson(
            listOf(
                FoundationSymbol("ج", "jīm", "جِيم"),
                FoundationSymbol("ح", "ḥāʾ", "حَاء"),
                FoundationSymbol("خ", "khāʾ", "خَاء"),
                FoundationSymbol("د", "dāl", "دَال"),
            ),
        ),
        AlphabetLesson(
            listOf(
                FoundationSymbol("ذ", "dhāl", "ذَال"),
                FoundationSymbol("ر", "rāʾ", "رَاء"),
                FoundationSymbol("ز", "zāy", "زَاي"),
                FoundationSymbol("س", "sīn", "سِين"),
            ),
        ),
        AlphabetLesson(
            listOf(
                FoundationSymbol("ش", "shīn", "شِين"),
                FoundationSymbol("ص", "ṣād", "صَاد"),
                FoundationSymbol("ض", "ḍād", "ضَاد"),
                FoundationSymbol("ط", "ṭāʾ", "طَاء"),
            ),
        ),
        AlphabetLesson(
            listOf(
                FoundationSymbol("ظ", "ẓāʾ", "ظَاء"),
                FoundationSymbol("ع", "ʿayn", "عَيْن"),
                FoundationSymbol("غ", "ghayn", "غَيْن"),
                FoundationSymbol("ف", "fāʾ", "فَاء"),
            ),
        ),
        AlphabetLesson(
            listOf(
                FoundationSymbol("ق", "qāf", "قَاف"),
                FoundationSymbol("ك", "kāf", "كَاف"),
                FoundationSymbol("ل", "lām", "لَام"),
                FoundationSymbol("م", "mīm", "مِيم"),
            ),
        ),
        AlphabetLesson(
            listOf(
                FoundationSymbol("ن", "nūn", "نُون"),
                FoundationSymbol("ه", "hāʾ", "هَاء"),
                FoundationSymbol("و", "wāw", "وَاو"),
                FoundationSymbol("ي", "yāʾ", "يَاء"),
            ),
        ),
    )

    val numberLessons: List<NumberLesson> = listOf(
        NumberLesson(0, "٠", "صِفْر", "ṣifr"),
        NumberLesson(1, "١", "وَاحِد", "wāḥid"),
        NumberLesson(2, "٢", "اِثْنَان", "ithnān"),
        NumberLesson(3, "٣", "ثَلَاثَة", "thalātha"),
        NumberLesson(4, "٤", "أَرْبَعَة", "arbaʿa"),
        NumberLesson(5, "٥", "خَمْسَة", "khamsa"),
        NumberLesson(6, "٦", "سِتَّة", "sitta"),
        NumberLesson(7, "٧", "سَبْعَة", "sabʿa"),
        NumberLesson(8, "٨", "ثَمَانِيَة", "thamāniya"),
        NumberLesson(9, "٩", "تِسْعَة", "tisʿa"),
    )

    /**
     * The complete alphabet reference remains available after the guided course.
     * Each row has the four short-vowel forms in the same order: fatḥa, kasra,
     * ḍamma, and sukūn.
     */
    val alphabetReference: List<ArabicLetterReference> by lazy {
        val sounds = listOf(
            "a" to "َ", "i" to "ِ", "u" to "ُ", "" to "ْ",
        )
        alphabetLessons
            .flatMap(AlphabetLesson::symbols)
            .map { letter ->
                val stem = letterVowelStem(letter.arabic)
                ArabicLetterReference(
                    letter = letter,
                    vowelVariants = if (letter.arabic == "ا") {
                        // A word-initial vowel is written on an alif with hamza. Showing the
                        // hamza avoids the misleading bare forms اَ, اِ, and اُ.
                        listOf(
                            FoundationSymbol("أَ", "ʾa"),
                            FoundationSymbol("إِ", "ʾi"),
                            FoundationSymbol("أُ", "ʾu"),
                            FoundationSymbol("أْ", "ʾ"),
                        )
                    } else {
                        sounds.map { (sound, mark) ->
                            FoundationSymbol(
                                arabic = letter.arabic + mark,
                                transliteration = stem + sound,
                                // The letter plus its vowel mark is what the device voice should say.
                                spokenArabic = letter.arabic + mark,
                            )
                        }
                    },
                )
            }
    }

    val alphabetLessonCount: Int get() = alphabetLessons.size
    val numberLessonCount: Int get() = numberLessons.size

    private fun letterVowelStem(letter: String): String = when (letter) {
        "ا" -> ""
        "ب" -> "b"
        "ت" -> "t"
        "ث" -> "th"
        "ج" -> "j"
        "ح" -> "ḥ"
        "خ" -> "kh"
        "د" -> "d"
        "ذ" -> "dh"
        "ر" -> "r"
        "ز" -> "z"
        "س" -> "s"
        "ش" -> "sh"
        "ص" -> "ṣ"
        "ض" -> "ḍ"
        "ط" -> "ṭ"
        "ظ" -> "ẓ"
        "ع" -> "ʿ"
        "غ" -> "gh"
        "ف" -> "f"
        "ق" -> "q"
        "ك" -> "k"
        "ل" -> "l"
        "م" -> "m"
        "ن" -> "n"
        "ه" -> "h"
        "و" -> "w"
        "ي" -> "y"
        else -> error("Unknown Arabic alphabet letter: $letter")
    }

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
