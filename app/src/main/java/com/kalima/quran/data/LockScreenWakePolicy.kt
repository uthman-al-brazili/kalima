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
     * A notification can wake a display without any user action. Waiting for
     * USER_PRESENT prevents those notification wakes from opening an activity.
     */
    fun shouldShowCard(event: LockScreenWakeEvent): Boolean =
        event == LockScreenWakeEvent.UserPresent

    /**
     * A screen-off event arms one card. SCREEN_ON may be caused by a notification,
     * so only USER_PRESENT consumes the arm and requests a launch.
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
            awaitingUnlock = awaitingUnlock,
            showCard = false,
        )
        LockScreenWakeEvent.UserPresent -> LockScreenWakeTransition(
            awaitingUnlock = false,
            showCard = awaitingUnlock,
        )
    }
}
