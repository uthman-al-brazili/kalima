package com.kalima.quran.audio

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.speech.tts.TextToSpeech

object ArabicVoiceInstaller {
    const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
    private const val TTS_SETTINGS_ACTION = "com.android.settings.TTS_SETTINGS"

    fun isGoogleEngine(enginePackage: String?): Boolean = enginePackage == GOOGLE_TTS_PACKAGE

    fun open(context: Context): Boolean {
        val candidates = buildList {
            add(Intent(TTS_SETTINGS_ACTION))
            add(
                Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                    .setPackage(GOOGLE_TTS_PACKAGE),
            )
            add(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
            add(Intent(Settings.ACTION_SETTINGS))
        }

        return candidates.any { intent -> launch(context, intent) }
    }

    private fun launch(context: Context, intent: Intent): Boolean {
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
