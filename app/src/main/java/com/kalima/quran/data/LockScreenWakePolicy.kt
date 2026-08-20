package com.kalima.quran.data

enum class LockScreenWakeEvent {
    DisplayWoke,
    UserPresent,
}

object LockScreenWakePolicy {
    /**
     * A notification can wake a display without any user action. Waiting for
     * USER_PRESENT prevents those notification wakes from opening an activity.
     */
    fun shouldShowCard(event: LockScreenWakeEvent): Boolean =
        event == LockScreenWakeEvent.UserPresent
}
