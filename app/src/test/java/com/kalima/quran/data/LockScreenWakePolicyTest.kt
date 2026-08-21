package com.kalima.quran.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockScreenWakePolicyTest {
    @Test
    fun `display wake is the event that can open over the keyguard`() {
        assertTrue(LockScreenWakePolicy.shouldShowCard(LockScreenWakeEvent.DisplayWoke))
    }

    @Test
    fun `user present is too late to open a lock screen card`() {
        assertFalse(LockScreenWakePolicy.shouldShowCard(LockScreenWakeEvent.UserPresent))
    }

    @Test
    fun `first display wake consumes the armed lock screen card`() {
        val armed = LockScreenWakePolicy.transition(false, LockScreenWakeEvent.ScreenOff)
        val displayWake = LockScreenWakePolicy.transition(
            armed.awaitingUnlock,
            LockScreenWakeEvent.DisplayWoke,
        )

        assertFalse(displayWake.awaitingUnlock)
        assertTrue(displayWake.showCard)
    }

    @Test
    fun `unlock cannot cause a duplicate launch after screen on`() {
        val displayWake = LockScreenWakePolicy.transition(true, LockScreenWakeEvent.DisplayWoke)
        val unlock = LockScreenWakePolicy.transition(
            displayWake.awaitingUnlock,
            LockScreenWakeEvent.UserPresent,
        )

        assertTrue(displayWake.showCard)
        assertFalse(unlock.showCard)
    }

    @Test
    fun `display wake without a preceding screen off does not launch`() {
        val displayWake = LockScreenWakePolicy.transition(false, LockScreenWakeEvent.DisplayWoke)

        assertFalse(displayWake.awaitingUnlock)
        assertFalse(displayWake.showCard)
    }
}
