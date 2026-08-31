package com.kalima.quran.data

import java.util.concurrent.Executor

/** Serializes writes and collapses queued values to the newest complete snapshot. */
internal class LatestValueWriter<T : Any>(
    private val executor: Executor,
    private val write: (T) -> Unit,
    private val onFailure: (Exception) -> Unit = {},
) {
    private val lock = Any()
    private var pending: T? = null
    private var workerScheduled = false

    fun submit(value: T) {
        val shouldSchedule = synchronized(lock) {
            pending = value
            if (workerScheduled) {
                false
            } else {
                workerScheduled = true
                true
            }
        }
        if (shouldSchedule) executor.execute(::drain)
    }

    private fun drain() {
        var shouldReschedule = false
        try {
            while (true) {
                val next = synchronized(lock) {
                    pending?.also { pending = null }
                } ?: return
                try {
                    write(next)
                } catch (error: Exception) {
                    onFailure(error)
                }
            }
        } finally {
            synchronized(lock) {
                workerScheduled = false
                if (pending != null) {
                    workerScheduled = true
                    shouldReschedule = true
                }
            }
            if (shouldReschedule) executor.execute(::drain)
        }
    }
}
