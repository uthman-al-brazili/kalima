package com.kalima.quran.data

import android.content.Context
import androidx.core.content.edit

/**
 * Feature switches that process entry points may read without constructing [ProgressStore].
 *
 * Broadcast receivers and services run on the main thread by default. Keeping these reads
 * separate prevents a simple boot-time decision from loading the vocabulary corpus.
 */
object ProgressFeatureFlags {
    internal const val PREFERENCES = "kalima_progress"
    internal const val KEY_REMINDER = "reminder"
    internal const val KEY_LOCK_SCREEN_ENABLED = "lock_screen_enabled"

    fun isReminderEnabled(context: Context): Boolean =
        preferences(context).getBoolean(KEY_REMINDER, false)

    fun isLockScreenEnabled(context: Context): Boolean =
        preferences(context).getBoolean(KEY_LOCK_SCREEN_ENABLED, false)

    fun setLockScreenEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit { putBoolean(KEY_LOCK_SCREEN_ENABLED, enabled) }
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
