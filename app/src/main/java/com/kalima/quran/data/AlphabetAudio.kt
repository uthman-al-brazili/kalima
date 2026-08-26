package com.kalima.quran.data

/** The approved offline recording for each Arabic letter name. */
enum class AlphabetAudio(val spokenArabic: String) {
    ALIF("أَلِف"),
    BAA("بَاء"),
    TAA("تَاء"),
    THAA("ثَاء"),
    JIIM("جِيم"),
    HAA_PHARYNGEAL("حَاء"),
    KHAA("خَاء"),
    DAAL("دَال"),
    DHAAL("ذَال"),
    RAA("رَاء"),
    ZAAY("زَاي"),
    SIIN("سِين"),
    SHIIN("شِين"),
    SAAD("صَاد"),
    DAAD("ضَاد"),
    TAA_EMPHATIC("طَاء"),
    ZAA_EMPHATIC("ظَاء"),
    AYN("عَيْن"),
    GHAYN("غَيْن"),
    FAA("فَاء"),
    QAAF("قَاف"),
    KAAF("كَاف"),
    LAAM("لَام"),
    MIIM("مِيم"),
    NUUN("نُون"),
    HAA("هَاء"),
    WAAW("وَاو"),
    YAA("يَاء"),
    ;

    companion object {
        private val bySpokenArabic = entries.associateBy(AlphabetAudio::spokenArabic)

        fun fromSpokenArabic(text: String): AlphabetAudio? = bySpokenArabic[text]
    }
}
