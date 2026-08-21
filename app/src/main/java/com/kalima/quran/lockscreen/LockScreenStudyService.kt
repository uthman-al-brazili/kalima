package com.kalima.quran.lockscreen

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.kalima.quran.MainActivity
import com.kalima.quran.R
import com.kalima.quran.data.ProgressStore
import com.kalima.quran.data.LockScreenDeviceBlockReason
import com.kalima.quran.data.LockScreenWakeEvent
import com.kalima.quran.data.LockScreenWakePolicy
import com.kalima.quran.localization.LanguageManager
import java.util.concurrent.Executors

class LockScreenStudyService : Service() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val precomputeExecutor = Executors.newSingleThreadExecutor()
    private lateinit var progressStore: ProgressStore
    private lateinit var audioManager: AudioManager
    private var receiverRegistered = false
    private var audioCallbackRegistered = false
    @Volatile private var criticalAudioActive = false
    private var requestedAtElapsed = 0L
    private var awaitingUnlock = false
    private var unlockRetryCount = 0

    private val showCard: Runnable = Runnable {
        if (!progressStore.canShowLockScreenCard() || !Settings.canDrawOverlays(this)) return@Runnable
        val systemBlock = LockScreenSystemSafety.blockReason(this, criticalAudioActive)
        if (systemBlock != null) {
            if (systemBlock in RETRYABLE_UNLOCK_BLOCKS && unlockRetryCount < MAX_UNLOCK_RETRIES) {
                unlockRetryCount += 1
                mainHandler.postDelayed(showCard, UNLOCK_RETRY_DELAY_MS)
                return@Runnable
            }
            progressStore.recordLockScreenSafetySkip()
            return@Runnable
        }
        try {
            startActivity(
                Intent(this, LockScreenStudyActivity::class.java).apply {
                    putExtra(LockScreenStudyActivity.EXTRA_REQUESTED_AT_ELAPSED, requestedAtElapsed)
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION,
                    )
                },
            )
        } catch (error: RuntimeException) {
            Log.e(TAG, "Unable to show the return-to-phone study card", error)
        }
    }

    private val audioCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
            criticalAudioActive = configs.orEmpty().any { configuration ->
                configuration.audioAttributes.usage in CRITICAL_AUDIO_USAGES
            }
            if (criticalAudioActive) {
                mainHandler.removeCallbacks(showCard)
                closeStudyCard()
            }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    mainHandler.removeCallbacks(showCard)
                    closeStudyCard()
                    awaitingUnlock = LockScreenWakePolicy.transition(
                        awaitingUnlock,
                        LockScreenWakeEvent.ScreenOff,
                    ).awaitingUnlock
                    if (progressStore.canShowLockScreenCard()) {
                        precomputeExecutor.execute {
                            runCatching { progressStore.prepareNextLockScreenSession() }
                                .onFailure { error -> Log.w(TAG, "Unable to precompute study card", error) }
                        }
                    }
                }

                Intent.ACTION_SCREEN_ON -> {
                    awaitingUnlock = LockScreenWakePolicy.transition(
                        awaitingUnlock,
                        LockScreenWakeEvent.DisplayWoke,
                    ).awaitingUnlock
                }

                Intent.ACTION_USER_PRESENT -> {
                    val transition = LockScreenWakePolicy.transition(
                        awaitingUnlock,
                        LockScreenWakeEvent.UserPresent,
                    )
                    awaitingUnlock = transition.awaitingUnlock
                    if (transition.showCard) {
                        requestedAtElapsed = SystemClock.elapsedRealtime()
                        unlockRetryCount = 0
                        mainHandler.removeCallbacks(showCard)
                        mainHandler.postDelayed(showCard, SCREEN_ON_DELAY_MS)
                    }
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.localizedContext(newBase))
    }

    override fun onCreate() {
        super.onCreate()
        progressStore = ProgressStore.get(applicationContext)
        audioManager = getSystemService(AudioManager::class.java)
        awaitingUnlock = !getSystemService(PowerManager::class.java).isInteractive ||
            getSystemService(KeyguardManager::class.java).isKeyguardLocked
        createChannel()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
        audioManager.registerAudioPlaybackCallback(audioCallback, mainHandler)
        audioCallbackRegistered = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            progressStore.setLockScreenEnabled(false)
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
        if (audioCallbackRegistered) {
            audioManager.unregisterAudioPlaybackCallback(audioCallback)
            audioCallbackRegistered = false
        }
        if (receiverRegistered) {
            unregisterReceiver(screenReceiver)
            receiverRegistered = false
        }
        precomputeExecutor.shutdownNow()
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
            Intent(LockScreenStudyActivity.ACTION_CLOSE).setPackage(packageName),
        )
    }

    companion object {
        private const val TAG = "KalimaLockScreen"
        private const val CHANNEL_ID = "lock_screen_study"
        private const val NOTIFICATION_ID = 1401
        private const val SCREEN_ON_DELAY_MS = 200L
        private const val UNLOCK_RETRY_DELAY_MS = 250L
        private const val MAX_UNLOCK_RETRIES = 8
        private const val ACTION_START = "com.kalima.quran.action.START_LOCK_SCREEN"
        private const val ACTION_STOP = "com.kalima.quran.action.STOP_LOCK_SCREEN"
        private val CRITICAL_AUDIO_USAGES = setOf(
            AudioAttributes.USAGE_ALARM,
            AudioAttributes.USAGE_NOTIFICATION_RINGTONE,
            AudioAttributes.USAGE_VOICE_COMMUNICATION,
        )
        private val RETRYABLE_UNLOCK_BLOCKS = setOf(
            LockScreenDeviceBlockReason.ScreenNotInteractive,
            LockScreenDeviceBlockReason.DeviceLocked,
        )

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
