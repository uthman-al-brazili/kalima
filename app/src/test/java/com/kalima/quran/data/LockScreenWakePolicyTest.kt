package com.kalima.quran.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockScreenWakePolicyTest {
    @Test
    fun `notification display wakes never open a study activity`() {
        assertFalse(LockScreenWakePolicy.shouldShowCard(LockScreenWakeEvent.DisplayWoke))
    }

    @Test
    fun `a confirmed present user can receive a study card`() {
        assertTrue(LockScreenWakePolicy.shouldShowCard(LockScreenWakeEvent.UserPresent))
    }
}
