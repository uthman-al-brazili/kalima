package com.kalima.quran.lockscreen

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import com.kalima.quran.data.LockScreenDeviceBlockReason
import com.kalima.quran.data.LockScreenDevicePolicy
import com.kalima.quran.data.LockScreenDeviceState

object LockScreenSystemSafety {
    private const val UPCOMING_ALARM_GUARD_MS = 2 * 60 * 1_000L

    fun blockReason(
        context: Context,
        criticalAudioActive: Boolean = false,
        nowMillis: Long = System.currentTimeMillis(),
    ): LockScreenDeviceBlockReason? {
        val power = context.getSystemService(PowerManager::class.java)
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        val audio = context.getSystemService(AudioManager::class.java)
        val uiMode = context.getSystemService(UiModeManager::class.java)
        val alarm = context.getSystemService(AlarmManager::class.java)
        val nextAlarmAt = alarm.nextAlarmClock?.triggerTime
        val alarmImminent = nextAlarmAt != null && nextAlarmAt - nowMillis in 0..UPCOMING_ALARM_GUARD_MS
        val thermalPressure = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            power.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE
        val callMode = audio.mode == AudioManager.MODE_RINGTONE ||
            audio.mode == AudioManager.MODE_IN_CALL ||
            audio.mode == AudioManager.MODE_IN_COMMUNICATION

        return LockScreenDevicePolicy.blockReason(
            LockScreenDeviceState(
                screenInteractive = power.isInteractive,
                deviceLocked = keyguard.isKeyguardLocked,
                callOrAlarmActive = criticalAudioActive || callMode || alarmImminent,
                mediaActive = audio.isMusicActive,
                carMode = uiMode.currentModeType == Configuration.UI_MODE_TYPE_CAR,
                powerSaver = power.isPowerSaveMode,
                thermalPressure = thermalPressure,
            ),
        )
    }
}
