package com.kalima.quran.desktop

import java.awt.MenuItem
import java.awt.PopupMenu
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
    private var trayIcon: TrayIcon? = null
    private var timer: Timer? = null

    fun start(
        store: DesktopProgressStore,
        onOpen: () -> Unit,
        onExit: () -> Unit,
    ) {
        if (started || !SystemTray.isSupported()) return
        val resource = javaClass.getResource("/ic_launcher-playstore.png") ?: return
        val icon = TrayIcon(Toolkit.getDefaultToolkit().getImage(resource), "Kalima").apply {
            isImageAutoSize = true
            popupMenu = PopupMenu().apply {
                add(MenuItem(store.language.t("Abrir o Kalima", "Open Kalima")).apply {
                    addActionListener { onOpen() }
                })
                addSeparator()
                add(MenuItem(store.language.t("Sair", "Exit")).apply {
                    addActionListener { onExit() }
                })
            }
            addActionListener { onOpen() }
        }
        runCatching { SystemTray.getSystemTray().add(icon) }.onFailure { return }
        trayIcon = icon
        started = true
        timer = Timer("kalima-reminder", true).also { reminderTimer -> reminderTimer.scheduleAtFixedRate(
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
        ) }
    }

    fun stop() {
        timer?.cancel()
        timer = null
        trayIcon?.let { runCatching { SystemTray.getSystemTray().remove(it) } }
        trayIcon = null
        started = false
    }

    fun isSupported(): Boolean = SystemTray.isSupported()
}
