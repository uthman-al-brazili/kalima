package com.kalima.quran.data

enum class LockScreenDeviceBlockReason {
    ScreenNotInteractive,
    DeviceLocked,
    CallOrAlarm,
    MediaActive,
    CarMode,
    PowerSaver,
    ThermalPressure,
}

data class LockScreenDeviceState(
    val screenInteractive: Boolean,
    val deviceLocked: Boolean,
    val callOrAlarmActive: Boolean,
    val mediaActive: Boolean,
    val carMode: Boolean,
    val powerSaver: Boolean,
    val thermalPressure: Boolean,
)

object LockScreenDevicePolicy {
    fun blockReason(
        state: LockScreenDeviceState,
        allowLockedDevice: Boolean = false,
    ): LockScreenDeviceBlockReason? = when {
        !state.screenInteractive -> LockScreenDeviceBlockReason.ScreenNotInteractive
        state.deviceLocked && !allowLockedDevice -> LockScreenDeviceBlockReason.DeviceLocked
        state.callOrAlarmActive -> LockScreenDeviceBlockReason.CallOrAlarm
        state.mediaActive -> LockScreenDeviceBlockReason.MediaActive
        state.carMode -> LockScreenDeviceBlockReason.CarMode
        state.powerSaver -> LockScreenDeviceBlockReason.PowerSaver
        state.thermalPressure -> LockScreenDeviceBlockReason.ThermalPressure
        else -> null
    }
}
