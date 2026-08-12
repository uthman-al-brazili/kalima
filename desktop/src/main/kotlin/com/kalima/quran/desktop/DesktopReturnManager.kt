package com.kalima.quran.desktop

import com.sun.jna.Platform
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinUser
import java.awt.EventQueue
import java.util.Timer
import java.util.TimerTask

fun interface IdleTimeProvider {
    fun idleMillis(): Long?
}

fun interface InterruptionGuard {
    fun shouldDeferCard(): Boolean
}

class WindowsIdleTimeProvider : IdleTimeProvider {
    override fun idleMillis(): Long? {
        if (!Platform.isWindows()) return null
        val info = WinUser.LASTINPUTINFO().apply { cbSize = size() }
        if (!User32.INSTANCE.GetLastInputInfo(info)) return null
        val currentTick = Kernel32.INSTANCE.GetTickCount()
        return Integer.toUnsignedLong(currentTick - info.dwTime)
    }
}

class WindowsFullScreenGuard : InterruptionGuard {
    override fun shouldDeferCard(): Boolean {
        if (!Platform.isWindows()) return false
        val foreground = User32.INSTANCE.GetForegroundWindow() ?: return false
        val windowRect = com.sun.jna.platform.win32.WinDef.RECT()
        if (!User32.INSTANCE.GetWindowRect(foreground, windowRect)) return false
        val monitor = User32.INSTANCE.MonitorFromWindow(
            foreground,
            WinUser.MONITOR_DEFAULTTONEAREST,
        ) ?: return false
        val monitorInfo = WinUser.MONITORINFO().apply { cbSize = size() }
        if (!User32.INSTANCE.GetMonitorInfo(monitor, monitorInfo).booleanValue()) return false
        val screen = monitorInfo.rcMonitor
        return windowRect.left <= screen.left &&
            windowRect.top <= screen.top &&
            windowRect.right >= screen.right &&
            windowRect.bottom >= screen.bottom
    }
}

class DesktopReturnManager(
    private val idleTimeProvider: IdleTimeProvider = WindowsIdleTimeProvider(),
    private val detector: ReturnFromIdleDetector = ReturnFromIdleDetector(),
    private val interruptionGuard: InterruptionGuard = WindowsFullScreenGuard(),
) {
    private var timer: Timer? = null
    private var pendingReturn = false

    fun start(store: DesktopProgressStore, onReturn: () -> Unit) {
        if (timer != null) return
        timer = Timer("kalima-return-cards", true).also { returnTimer ->
            returnTimer.scheduleAtFixedRate(
                object : TimerTask() {
                    override fun run() {
                        val idleMillis = idleTimeProvider.idleMillis() ?: return
                        val thresholdMillis = store.returnCardIdleMinutes * 60_000L
                        if (detector.sample(idleMillis, thresholdMillis)) pendingReturn = true
                        if (pendingReturn && !interruptionGuard.shouldDeferCard()) {
                            pendingReturn = false
                            EventQueue.invokeLater(onReturn)
                        }
                    }
                },
                POLL_INTERVAL_MILLIS,
                POLL_INTERVAL_MILLIS,
            )
        }
    }

    fun stop() {
        timer?.cancel()
        timer = null
        pendingReturn = false
    }

    companion object {
        const val POLL_INTERVAL_MILLIS = 1_000L
    }
}
