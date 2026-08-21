package com.kalima.quran.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

data class ArabicCalendarTerm(
    val arabic: String,
    val shortArabic: String,
)

data class HijriCalendarDate(
    val dayOfMonth: Int,
    val monthOfYear: Int,
    val year: Int,
    val dayOfWeek: DayOfWeek,
)

object HijriCalendar {
    fun from(date: LocalDate): HijriCalendarDate {
        val hijri = HijrahDate.from(date)
        return HijriCalendarDate(
            dayOfMonth = hijri.get(ChronoField.DAY_OF_MONTH),
            monthOfYear = hijri.get(ChronoField.MONTH_OF_YEAR),
            year = hijri.get(ChronoField.YEAR_OF_ERA),
            dayOfWeek = date.dayOfWeek,
        )
    }

    fun weekday(dayOfWeek: DayOfWeek): ArabicCalendarTerm = when (dayOfWeek) {
        DayOfWeek.MONDAY -> ArabicCalendarTerm("ٱلِاثْنَيْن", "اث")
        DayOfWeek.TUESDAY -> ArabicCalendarTerm("ٱلثُّلَاثَاء", "ثل")
        DayOfWeek.WEDNESDAY -> ArabicCalendarTerm("ٱلْأَرْبِعَاء", "أر")
        DayOfWeek.THURSDAY -> ArabicCalendarTerm("ٱلْخَمِيس", "خم")
        DayOfWeek.FRIDAY -> ArabicCalendarTerm("ٱلْجُمُعَة", "جم")
        DayOfWeek.SATURDAY -> ArabicCalendarTerm("ٱلسَّبْت", "سب")
        DayOfWeek.SUNDAY -> ArabicCalendarTerm("ٱلْأَحَد", "أح")
    }

    fun month(monthOfYear: Int): ArabicCalendarTerm = MONTHS.getOrNull(monthOfYear - 1)
        ?: error("Invalid Hijri month: $monthOfYear")

    fun arabicIndicNumber(value: Int): String = value.toString().map { character ->
        if (character in '0'..'9') ARABIC_INDIC_DIGITS[character - '0'] else character
    }.joinToString("")

    private val MONTHS = listOf(
        ArabicCalendarTerm("مُحَرَّم", "محرّم"),
        ArabicCalendarTerm("صَفَر", "صفر"),
        ArabicCalendarTerm("رَبِيع ٱلْأَوَّل", "ربيع ١"),
        ArabicCalendarTerm("رَبِيع ٱلْآخِر", "ربيع ٢"),
        ArabicCalendarTerm("جُمَادَىٰ ٱلْأُولَىٰ", "جمادىٰ ١"),
        ArabicCalendarTerm("جُمَادَىٰ ٱلْآخِرَة", "جمادىٰ ٢"),
        ArabicCalendarTerm("رَجَب", "رجب"),
        ArabicCalendarTerm("شَعْبَان", "شعبان"),
        ArabicCalendarTerm("رَمَضَان", "رمضان"),
        ArabicCalendarTerm("شَوَّال", "شوّال"),
        ArabicCalendarTerm("ذُو ٱلْقَعْدَة", "ذو القعدة"),
        ArabicCalendarTerm("ذُو ٱلْحِجَّة", "ذو الحجّة"),
    )

    private const val ARABIC_INDIC_DIGITS = "٠١٢٣٤٥٦٧٨٩"
}
