package com.kalima.quran.desktop

import com.sun.jna.Platform
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import java.nio.file.Path

object DesktopStartupManager {
    private const val RUN_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    private const val VALUE_NAME = "Kalima"

    fun isAvailable(): Boolean = currentExecutable()?.let(::startupCommandFor) != null

    fun isEnabled(): Boolean = Platform.isWindows() && runCatching {
        Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, RUN_KEY, VALUE_NAME)
    }.getOrDefault(false)

    fun setEnabled(enabled: Boolean): Boolean {
        if (!Platform.isWindows()) return false
        return runCatching {
            if (enabled) {
                val command = currentExecutable()?.let(::startupCommandFor) ?: return false
                Advapi32Util.registrySetStringValue(
                    WinReg.HKEY_CURRENT_USER,
                    RUN_KEY,
                    VALUE_NAME,
                    command,
                )
            } else if (Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, RUN_KEY, VALUE_NAME)) {
                Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, RUN_KEY, VALUE_NAME)
            }
            true
        }.getOrDefault(false)
    }

    internal fun startupCommandFor(executable: Path): String? {
        val fileName = executable.fileName?.toString().orEmpty()
        if (!fileName.equals("Kalima.exe", ignoreCase = true)) return null
        return "\"${executable.toAbsolutePath().normalize()}\" --background"
    }

    private fun currentExecutable(): Path? = ProcessHandle.current().info().command()
        .orElse(null)
        ?.takeIf(String::isNotBlank)
        ?.let(Path::of)
}
