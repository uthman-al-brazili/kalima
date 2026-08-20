package com.kalima.quran.data

data class FoundationSymbol(
    val arabic: String,
    val transliteration: String,
    val spokenArabic: String = arabic,
)

data class AlphabetLesson(
    val symbols: List<FoundationSymbol>,
    val teachesVowels: Boolean = false,
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

object ArabicFoundations {
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
        AlphabetLesson(
            symbols = listOf(
                FoundationSymbol("بَ", "ba"),
                FoundationSymbol("بِ", "bi"),
                FoundationSymbol("بُ", "bu"),
                FoundationSymbol("بْ", "b"),
            ),
            teachesVowels = true,
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

    val alphabetLessonCount: Int get() = alphabetLessons.size
    val numberLessonCount: Int get() = numberLessons.size
}

val StudyProgress.needsAlphabetFoundation: Boolean
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
)
