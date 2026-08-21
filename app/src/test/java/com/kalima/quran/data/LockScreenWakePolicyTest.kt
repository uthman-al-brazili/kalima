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

    @Test
    fun `notification wake keeps card armed but never launches it`() {
        val armed = LockScreenWakePolicy.transition(false, LockScreenWakeEvent.ScreenOff)
        val notificationWake = LockScreenWakePolicy.transition(
            armed.awaitingUnlock,
            LockScreenWakeEvent.DisplayWoke,
        )

        assertTrue(notificationWake.awaitingUnlock)
        assertFalse(notificationWake.showCard)
    }

    @Test
    fun `only the first confirmed unlock consumes an armed card`() {
        val unlock = LockScreenWakePolicy.transition(true, LockScreenWakeEvent.UserPresent)
        val duplicate = LockScreenWakePolicy.transition(
            unlock.awaitingUnlock,
            LockScreenWakeEvent.UserPresent,
        )

        assertTrue(unlock.showCard)
        assertFalse(duplicate.showCard)
    }
}
