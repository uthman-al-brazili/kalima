package com.kalima.quran.desktop

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kalima.quran.audio.ArabicSpeechText
import com.kalima.quran.localization.AppLanguage
import java.awt.EventQueue
import kotlin.concurrent.thread

internal enum class DesktopSpeechResult {
    Started,
    Unavailable,
    Failed,
}

private enum class DesktopSpeechIssueType {
    VoiceUnavailable,
    PlaybackFailed,
}

private data class DesktopSpeechIssue(
    val type: DesktopSpeechIssueType,
    val language: AppLanguage,
)

object DesktopPronouncer {
    private var issue by mutableStateOf<DesktopSpeechIssue?>(null)
    private val processLock = Any()
    private var activeProcess: Process? = null

    fun speak(arabic: String, language: AppLanguage, slow: Boolean = true) {
        val prepared = ArabicSpeechText.prepare(arabic)
        if (prepared.isBlank()) return
        thread(name = "kalima-windows-speech", isDaemon = true) {
            val result = runCatching { executeSpeech(prepared, slow) }
                .getOrDefault(DesktopSpeechResult.Failed)

            when (result) {
                DesktopSpeechResult.Started -> Unit
                DesktopSpeechResult.Unavailable -> showIssue(DesktopSpeechIssueType.VoiceUnavailable, language)
                DesktopSpeechResult.Failed -> showIssue(DesktopSpeechIssueType.PlaybackFailed, language)
            }
        }
    }

    internal fun executeSpeech(text: String, slow: Boolean): DesktopSpeechResult {
        val process = ProcessBuilder(
            "powershell.exe",
            "-NoProfile",
            "-NonInteractive",
            "-Command",
            SPEECH_SCRIPT,
        ).apply {
            environment()["KALIMA_SPEECH_TEXT"] = text
            environment()["KALIMA_SPEECH_RATE"] = if (slow) "-2" else "0"
            redirectErrorStream(true)
        }.start()
        synchronized(processLock) {
            activeProcess?.destroy()
            activeProcess = process
        }
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        synchronized(processLock) {
            if (activeProcess === process) activeProcess = null
        }
        return classifySpeechResult(exitCode, output)
    }

    fun openVoiceSettings(language: AppLanguage) {
        val opened = runCatching {
            ProcessBuilder("explorer.exe", "ms-settings:speech").start()
            true
        }.getOrDefault(false)
        if (!opened) showIssue(DesktopSpeechIssueType.PlaybackFailed, language)
    }

    private fun showIssue(type: DesktopSpeechIssueType, language: AppLanguage) {
        val update = { issue = DesktopSpeechIssue(type, language) }
        if (EventQueue.isDispatchThread()) update() else EventQueue.invokeLater(update)
    }

    private fun dismissIssue() {
        issue = null
    }

    @Composable
    fun FeedbackDialog() {
        val current = issue ?: return
        val unavailable = current.type == DesktopSpeechIssueType.VoiceUnavailable
        val language = current.language
        AlertDialog(
            onDismissRequest = ::dismissIssue,
            title = {
                Text(
                    if (unavailable) language.t("Voz árabe necessária", "Arabic voice required")
                    else language.t("Não foi possível reproduzir", "Could not play pronunciation"),
                )
            },
            text = {
                Text(
                    if (unavailable) {
                        language.t(
                            "Nenhuma voz árabe está instalada no Windows. Instale uma voz árabe nas configurações de fala e tente novamente.",
                            "No Arabic voice is installed in Windows. Install an Arabic voice in Speech settings, then try again.",
                        )
                    } else {
                        language.t(
                            "O Windows não conseguiu iniciar a voz do dispositivo. Verifique as configurações de fala e tente novamente.",
                            "Windows could not start the device voice. Check Speech settings and try again.",
                        )
                    },
                )
            },
            confirmButton = {
                if (unavailable) {
                    TextButton(
                        onClick = {
                            dismissIssue()
                            openVoiceSettings(language)
                        },
                    ) {
                        Text(language.t("Abrir configurações de voz", "Open voice settings"))
                    }
                } else {
                    TextButton(onClick = ::dismissIssue) {
                        Text(language.t("Fechar", "Close"))
                    }
                }
            },
            dismissButton = if (unavailable) {
                {
                    TextButton(onClick = ::dismissIssue) {
                        Text(language.t("Agora não", "Not now"))
                    }
                }
            } else {
                null
            },
        )
    }

    private val SPEECH_SCRIPT =
        """
        ${'$'}ErrorActionPreference = 'Stop'
        ${'$'}voiceFound = ${'$'}false

        # Prefer the modern Windows speech API because it sees current OneCore voice packages.
        try {
            Add-Type -AssemblyName System.Runtime.WindowsRuntime
            ${'$'}null = [Windows.Media.SpeechSynthesis.SpeechSynthesizer, Windows.Media.SpeechSynthesis, ContentType = WindowsRuntime]
            ${'$'}streamType = [Windows.Media.SpeechSynthesis.SpeechSynthesisStream, Windows.Media.SpeechSynthesis, ContentType = WindowsRuntime]
            ${'$'}modernVoice = [Windows.Media.SpeechSynthesis.SpeechSynthesizer]::AllVoices |
                Where-Object { ${'$'}_.Language -eq 'ar' -or ${'$'}_.Language -like 'ar-*' } |
                Select-Object -First 1
            if (${'$'}null -ne ${'$'}modernVoice) {
                ${'$'}voiceFound = ${'$'}true
                ${'$'}synthesizer = New-Object Windows.Media.SpeechSynthesis.SpeechSynthesizer
                ${'$'}synthesizer.Voice = ${'$'}modernVoice
                ${'$'}synthesizer.Options.SpeakingRate = if (${'$'}env:KALIMA_SPEECH_RATE -eq '-2') { 0.65 } else { 1.0 }
                ${'$'}operation = ${'$'}synthesizer.SynthesizeTextToStreamAsync(${'$'}env:KALIMA_SPEECH_TEXT)
                ${'$'}asTask = [System.WindowsRuntimeSystemExtensions].GetMethods() |
                    Where-Object { ${'$'}_.Name -eq 'AsTask' -and ${'$'}_.IsGenericMethod -and ${'$'}_.GetParameters().Count -eq 1 } |
                    Select-Object -First 1
                ${'$'}task = ${'$'}asTask.MakeGenericMethod(${'$'}streamType).Invoke(${'$'}null, @(${'$'}operation))
                ${'$'}task.Wait()
                ${'$'}speechStream = ${'$'}task.Result
                ${'$'}managedStream = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead(${'$'}speechStream)
                ${'$'}player = New-Object System.Media.SoundPlayer(${'$'}managedStream)
                ${'$'}player.PlaySync()
                Write-Output '$STARTED_MARKER'
                exit 0
            }
        } catch {
            # Some Windows editions disable the modern API for unpackaged desktop processes.
            # Fall through to classic SAPI, which remains available on those systems.
        }

        try {
            ${'$'}speaker = New-Object -ComObject SAPI.SpVoice
            ${'$'}arabicVoice = @(${'$'}speaker.GetVoices()) | Where-Object {
                try {
                    ${'$'}language = ${'$'}_.GetAttribute('Language').Split(';')[0]
                    ([Convert]::ToInt32(${'$'}language, 16) -band 0x3ff) -eq 1
                } catch { ${'$'}false }
            } | Select-Object -First 1
            if (${'$'}null -ne ${'$'}arabicVoice) {
                ${'$'}voiceFound = ${'$'}true
                ${'$'}speaker.Voice = ${'$'}arabicVoice
                ${'$'}speaker.Rate = [int]${'$'}env:KALIMA_SPEECH_RATE
                ${'$'}speaker.Volume = 100
                [void]${'$'}speaker.Speak(${'$'}env:KALIMA_SPEECH_TEXT)
                Write-Output '$STARTED_MARKER'
                exit 0
            }
        } catch {
            ${'$'}voiceFound = ${'$'}true
        }

        if (-not ${'$'}voiceFound) {
            Write-Output '$UNAVAILABLE_MARKER'
            exit 2
        } else {
            Write-Output '$FAILED_MARKER'
            exit 3
        }
        """.trimIndent()

    internal fun classifySpeechResult(exitCode: Int, output: String): DesktopSpeechResult = when {
        exitCode == 0 && STARTED_MARKER in output -> DesktopSpeechResult.Started
        exitCode == 2 && UNAVAILABLE_MARKER in output -> DesktopSpeechResult.Unavailable
        else -> DesktopSpeechResult.Failed
    }

    private const val STARTED_MARKER = "KALIMA_SPEECH_STARTED"
    private const val UNAVAILABLE_MARKER = "KALIMA_ARABIC_VOICE_UNAVAILABLE"
    private const val FAILED_MARKER = "KALIMA_SPEECH_FAILED"
}
