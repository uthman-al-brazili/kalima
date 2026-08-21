package com.kalima.quran.data

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HijriCalendarTest {
    @Test
    fun `known first day of Ramadan is displayed in the Hijri calendar`() {
        val date = HijriCalendar.from(LocalDate.of(2024, 3, 11))

        assertEquals(1, date.dayOfMonth)
        assertEquals(9, date.monthOfYear)
        assertEquals(1445, date.year)
        assertEquals(DayOfWeek.MONDAY, date.dayOfWeek)
    }

    @Test
    fun `calendar terms provide Arabic learning labels`() {
        assertEquals("ٱلْجُمُعَة", HijriCalendar.weekday(DayOfWeek.FRIDAY).arabic)
        assertEquals("رَمَضَان", HijriCalendar.month(9).arabic)
        assertEquals("١٤٤٨", HijriCalendar.arabicIndicNumber(1448))
    }
}
