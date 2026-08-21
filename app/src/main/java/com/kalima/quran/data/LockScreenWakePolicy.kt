package com.kalima.quran.data

enum class LockScreenWakeEvent {
    ScreenOff,
    DisplayWoke,
    UserPresent,
}

data class LockScreenWakeTransition(
    val awaitingUnlock: Boolean,
    val showCard: Boolean,
)

object LockScreenWakePolicy {
    /**
     * The lock-screen card must launch while the keyguard is still visible.
     * USER_PRESENT is too late because Android sends it after authentication.
     */
    fun shouldShowCard(event: LockScreenWakeEvent): Boolean =
        event == LockScreenWakeEvent.DisplayWoke

    /**
     * A screen-off event arms one card. The first SCREEN_ON consumes that arm and
     * requests a launch over the keyguard. USER_PRESENT must never launch again.
     */
    fun transition(
        awaitingUnlock: Boolean,
        event: LockScreenWakeEvent,
    ): LockScreenWakeTransition = when (event) {
        LockScreenWakeEvent.ScreenOff -> LockScreenWakeTransition(
            awaitingUnlock = true,
            showCard = false,
        )
        LockScreenWakeEvent.DisplayWoke -> LockScreenWakeTransition(
            awaitingUnlock = false,
            showCard = awaitingUnlock,
        )
        LockScreenWakeEvent.UserPresent -> LockScreenWakeTransition(
            awaitingUnlock = false,
            showCard = false,
        )
    }
}
