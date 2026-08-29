package com.kalima.quran.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ArabicIndicClockTest {
    @Test
    fun `clock digits are displayed as Arabic-Indic numerals`() {
        assertEquals("\u0661\u0664:\u0660\u0665", "14:05".toArabicIndicDigits())
    }

    @Test
    fun `clock punctuation and period marker are preserved`() {
        assertEquals("\u0669:\u0663\u0660 PM", "9:30 PM".toArabicIndicDigits())
    }
}
