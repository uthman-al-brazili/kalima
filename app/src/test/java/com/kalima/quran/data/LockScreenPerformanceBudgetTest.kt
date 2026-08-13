package com.kalima.quran.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockScreenPerformanceBudgetTest {
    @Test
    fun `launch latency must stay within the declared budget`() {
        assertTrue(LockScreenPerformanceBudget.isWithinBudget(700))
        assertFalse(LockScreenPerformanceBudget.isWithinBudget(701))
        assertFalse(LockScreenPerformanceBudget.isWithinBudget(null))
    }
}
