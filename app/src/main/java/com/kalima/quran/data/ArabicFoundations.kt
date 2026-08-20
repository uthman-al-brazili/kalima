package com.kalima.quran.data

data class FoundationSymbol(
    val arabic: String,
    val transliteration: String,
)

data class AlphabetLesson(
    val symbols: List<FoundationSymbol>,
    val teachesVowels: Boolean = false,
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
                FoundationSymbol("ا", "alif"),
                FoundationSymbol("ب", "bāʾ"),
                FoundationSymbol("ت", "tāʾ"),
                FoundationSymbol("ث", "thāʾ"),
            ),
        ),
        AlphabetLesson(
            listOf(
                FoundationSymbol("ج", "jīm"),
                FoundationSymbol("ح", "ḥāʾ"),
                FoundationSymbol("خ", "khāʾ"),
                FoundationSymbol("د", "dāl"),
            ),
        ),
        AlphabetLesson(
            listOf(
                FoundationSymbol("ذ", "dhāl"),
                FoundationSymbol("ر", "rāʾ"),
                FoundationSymbol("ز", "zāy"),
                FoundationSymbol("س", "sīn"),
            ),
        ),
        AlphabetLesson(
            listOf(
                FoundationSymbol("ش", "shīn"),
                FoundationSymbol("ص", "ṣād"),
                FoundationSymbol("ض", "ḍād"),
                FoundationSymbol("ط", "ṭāʾ"),
            ),
        ),
        AlphabetLesson(
            listOf(
                FoundationSymbol("ظ", "ẓāʾ"),
                FoundationSymbol("ع", "ʿayn"),
                FoundationSymbol("غ", "ghayn"),
                FoundationSymbol("ف", "fāʾ"),
            ),
        ),
        AlphabetLesson(
            listOf(
                FoundationSymbol("ق", "qāf"),
                FoundationSymbol("ك", "kāf"),
                FoundationSymbol("ل", "lām"),
                FoundationSymbol("م", "mīm"),
            ),
        ),
        AlphabetLesson(
            listOf(
                FoundationSymbol("ن", "nūn"),
                FoundationSymbol("ه", "hāʾ"),
                FoundationSymbol("و", "wāw"),
                FoundationSymbol("ي", "yāʾ"),
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
