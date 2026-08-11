package com.kalima.quran.lockscreen

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.kalima.quran.MainActivity
import com.kalima.quran.R
import com.kalima.quran.data.ProgressStore
import com.kalima.quran.localization.LanguageManager

class LockScreenStudyService : Service() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var receiverRegistered = false

    private val showCard = Runnable {
        if (
            ProgressStore(applicationContext).progress.value.lockScreenEnabled &&
            Settings.canDrawOverlays(this)
        ) {
            try {
                startActivity(
                    Intent(this, LockScreenStudyActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                                Intent.FLAG_ACTIVITY_NO_ANIMATION,
                        )
                    },
                )
            } catch (error: RuntimeException) {
                Log.e(TAG, "Não foi possível mostrar o estudo na tela bloqueada", error)
            }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    mainHandler.removeCallbacks(showCard)
                    mainHandler.postDelayed(showCard, SCREEN_ON_DELAY_MS)
                }
                Intent.ACTION_SCREEN_OFF,
                Intent.ACTION_USER_PRESENT,
                -> closeStudyCard()
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.localizedContext(newBase))
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            ProgressStore(applicationContext).setLockScreenEnabled(false)
            stopSelf()
            return START_NOT_STICKY
        }

        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            serviceType,
        )
        return START_STICKY
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(showCard)
        closeStudyCard()
        if (receiverRegistered) {
            unregisterReceiver(screenReceiver)
            receiverRegistered = false
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): android.app.Notification {
        val localized = LanguageManager.localizedContext(this)
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopService = PendingIntent.getService(
            this,
            1,
            Intent(this, LockScreenStudyService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(localized, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localized.getString(R.string.lock_screen_service_title))
            .setContentText(localized.getString(R.string.lock_screen_service_text))
            .setContentIntent(openApp)
            .addAction(0, localized.getString(R.string.disable), stopService)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        val localized = LanguageManager.localizedContext(this)
        val channel = NotificationChannel(
            CHANNEL_ID,
            localized.getString(R.string.lock_screen_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = localized.getString(R.string.lock_screen_channel_description)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun closeStudyCard() {
        sendBroadcast(
            Intent(LockScreenStudyActivity.ACTION_CLOSE)
                .setPackage(packageName),
        )
    }

    companion object {
        private const val TAG = "KalimaLockScreen"
        private const val CHANNEL_ID = "lock_screen_study"
        private const val NOTIFICATION_ID = 1401
        private const val SCREEN_ON_DELAY_MS = 300L
        private const val ACTION_START = "com.kalima.quran.action.START_LOCK_SCREEN"
        private const val ACTION_STOP = "com.kalima.quran.action.STOP_LOCK_SCREEN"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, LockScreenStudyService::class.java).setAction(ACTION_START),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LockScreenStudyService::class.java))
        }
    }
}
