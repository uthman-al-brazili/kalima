package com.kalima.quran.data

import org.junit.Assert.assertEquals
import org.junit.Test

class QuranReaderTypographyTest {
    @Test
    fun `font size is kept inside the supported stepped range`() {
        assertEquals(18, QuranReaderTypography.normalize(4))
        assertEquals(22, QuranReaderTypography.normalize(22))
        assertEquals(22, QuranReaderTypography.normalize(23))
        assertEquals(40, QuranReaderTypography.normalize(80))
    }
}
