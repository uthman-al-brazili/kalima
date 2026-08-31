package com.kalima.quran.background

import android.content.BroadcastReceiver
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Runs receiver work away from the main thread and always releases its broadcast deadline. */
object AsyncBroadcastWork {
    private const val TAG = "KalimaReceiverWork"
    private const val MAX_BROADCAST_WORK_SECONDS = 8L

    private val worker = Executors.newFixedThreadPool(2) { runnable ->
        Thread(runnable, "kalima-receiver-worker").apply { isDaemon = true }
    }
    private val deadline = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "kalima-receiver-deadline").apply { isDaemon = true }
    }

    fun run(receiver: BroadcastReceiver, workName: String, block: () -> Unit) {
        val pendingResult = receiver.goAsync()
        val finished = AtomicBoolean(false)
        val workTask = AtomicReference<java.util.concurrent.Future<*>?>()
        val finish = {
            if (finished.compareAndSet(false, true)) pendingResult.finish()
        }
        val timeout = deadline.schedule(
            {
                Log.w(TAG, "$workName exceeded the broadcast work deadline")
                workTask.get()?.cancel(true)
                finish()
            },
            MAX_BROADCAST_WORK_SECONDS,
            TimeUnit.SECONDS,
        )
        workTask.set(worker.submit {
            try {
                block()
            } catch (error: RuntimeException) {
                Log.e(TAG, "$workName failed", error)
            } finally {
                timeout.cancel(false)
                finish()
            }
        })
    }
}
