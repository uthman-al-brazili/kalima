package com.kalima.quran.desktop

import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Timer
import java.util.TimerTask

object DesktopReminderManager {
    private var started = false
    private var lastShown: LocalDate? = null

    fun start(store: DesktopProgressStore) {
        if (started || !SystemTray.isSupported()) return
        val resource = javaClass.getResource("/ic_launcher-playstore.png") ?: return
        val icon = TrayIcon(Toolkit.getDefaultToolkit().getImage(resource), "Kalima").apply {
            isImageAutoSize = true
        }
        runCatching { SystemTray.getSystemTray().add(icon) }.onFailure { return }
        started = true
        Timer("kalima-reminder", true).scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    val now = LocalDateTime.now()
                    if (
                        store.progress.reminderEnabled &&
                        now.hour == store.reminderHour &&
                        now.minute == store.reminderMinute &&
                        lastShown != now.toLocalDate()
                    ) {
                        val language = store.language
                        icon.displayMessage(
                            "Kalima",
                            language.t(
                                "Sua revisão de árabe corânico está pronta.",
                                "Your Quranic Arabic review is ready.",
                            ),
                            TrayIcon.MessageType.INFO,
                        )
                        lastShown = now.toLocalDate()
                    }
                }
            },
            1_000L,
            30_000L,
        )
    }
}
