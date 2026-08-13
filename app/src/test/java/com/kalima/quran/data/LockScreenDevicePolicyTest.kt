package com.kalima.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LockScreenDevicePolicyTest {
    private val safe = LockScreenDeviceState(
        screenInteractive = true,
        deviceLocked = false,
        callOrAlarmActive = false,
        mediaActive = false,
        carMode = false,
        powerSaver = false,
        thermalPressure = false,
    )

    @Test
    fun criticalSystemSurfacesTakePriority() {
        assertEquals(
            LockScreenDeviceBlockReason.ScreenNotInteractive,
            LockScreenDevicePolicy.blockReason(safe.copy(screenInteractive = false)),
        )
        assertEquals(
            LockScreenDeviceBlockReason.CallOrAlarm,
            LockScreenDevicePolicy.blockReason(safe.copy(callOrAlarmActive = true)),
        )
        assertEquals(
            LockScreenDeviceBlockReason.CarMode,
            LockScreenDevicePolicy.blockReason(safe.copy(carMode = true)),
        )
        assertEquals(
            LockScreenDeviceBlockReason.PowerSaver,
            LockScreenDevicePolicy.blockReason(safe.copy(powerSaver = true)),
        )
        assertNull(LockScreenDevicePolicy.blockReason(safe.copy(deviceLocked = true)))
        assertNull(LockScreenDevicePolicy.blockReason(safe))
    }
}
