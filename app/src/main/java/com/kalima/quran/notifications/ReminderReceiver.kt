package com.kalima.quran.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kalima.quran.MainActivity
import com.kalima.quran.R
import com.kalima.quran.background.AsyncBroadcastWork
import com.kalima.quran.data.ProgressFeatureFlags
import com.kalima.quran.data.ProgressStore
import com.kalima.quran.data.StudyPlan
import com.kalima.quran.data.WordRepository
import com.kalima.quran.data.limitNewWords
import com.kalima.quran.lockscreen.LockScreenStudyService
import com.kalima.quran.localization.LanguageManager

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!ProgressFeatureFlags.isReminderEnabled(context)) return
        val applicationContext = context.applicationContext
        AsyncBroadcastWork.run(this, "daily reminder") {
            NotificationHelper.showWordOfTheDay(applicationContext)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (ProgressFeatureFlags.isReminderEnabled(context)) {
            ReminderScheduler.schedule(context)
        }
        if (ProgressFeatureFlags.isLockScreenEnabled(context) && Settings.canDrawOverlays(context)) {
            LockScreenStudyService.start(context)
        }
    }
}

object NotificationHelper {
    private const val CHANNEL_ID = "daily_word"
    private const val NOTIFICATION_ID = 1207

    fun createChannel(context: Context) {
        val localized = LanguageManager.localizedContext(context)
        val channel = NotificationChannel(
            CHANNEL_ID,
            localized.getString(R.string.daily_word_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = localized.getString(R.string.daily_word_channel_description)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun showWordOfTheDay(context: Context) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val localized = LanguageManager.localizedContext(context)
        createChannel(localized)
        val progress = ProgressStore.get(context).progress.value
        val activeWords = progress.limitNewWords(
            StudyPlan.calculate(progress, WordRepository.words).combinedWords,
        )
        if (activeWords.isEmpty()) return
        val word = WordRepository.wordFor(source = activeWords)
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(localized, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${word.arabic}  •  ${word.transliteration}")
            .setContentText(word.meaning)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${word.meaning}\n${word.reference} — ${word.verseMeaning}",
                ),
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
