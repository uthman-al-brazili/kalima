package com.kalima.quran.desktop

import kotlin.concurrent.thread

object DesktopPronouncer {
    fun speak(arabic: String, slow: Boolean = true) {
        if (arabic.isBlank()) return
        thread(name = "kalima-windows-speech", isDaemon = true) {
            runCatching {
                val script = if (slow) SLOW_SCRIPT else NORMAL_SCRIPT
                ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-NonInteractive",
                    "-Command",
                    script,
                ).apply {
                    environment()["KALIMA_SPEECH_TEXT"] = arabic
                    redirectErrorStream(true)
                }.start().apply {
                    inputStream.bufferedReader().use { it.readText() }
                    waitFor()
                }
            }
        }
    }

    private const val BASE_SCRIPT =
        "Add-Type -AssemblyName System.Speech; " +
            "\$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
            "\$v = \$s.GetInstalledVoices() | Where-Object { \$_.Enabled -and \$_.VoiceInfo.Culture.Name -like 'ar*' } | Select-Object -First 1; " +
            "if (\$v) { \$s.SelectVoice(\$v.VoiceInfo.Name) }; "
    private const val SLOW_SCRIPT = BASE_SCRIPT + "\$s.Rate = -2; \$s.Speak(\$env:KALIMA_SPEECH_TEXT)"
    private const val NORMAL_SCRIPT = BASE_SCRIPT + "\$s.Rate = 0; \$s.Speak(\$env:KALIMA_SPEECH_TEXT)"
}
