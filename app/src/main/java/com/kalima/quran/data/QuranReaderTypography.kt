package com.kalima.quran.data

object QuranReaderTypography {
    const val DEFAULT_FONT_SIZE_SP = 22
    const val MIN_FONT_SIZE_SP = 18
    const val MAX_FONT_SIZE_SP = 40
    const val FONT_SIZE_STEP_SP = 2

    fun normalize(fontSizeSp: Int): Int {
        val clamped = fontSizeSp.coerceIn(MIN_FONT_SIZE_SP, MAX_FONT_SIZE_SP)
        val stepsFromMinimum = (clamped - MIN_FONT_SIZE_SP) / FONT_SIZE_STEP_SP
        return MIN_FONT_SIZE_SP + stepsFromMinimum * FONT_SIZE_STEP_SP
    }
}
