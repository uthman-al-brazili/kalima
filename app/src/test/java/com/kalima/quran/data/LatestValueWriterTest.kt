package com.kalima.quran.data

import java.util.ArrayDeque
import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Test

class LatestValueWriterTest {
    @Test
    fun `queued snapshots collapse to the newest value`() {
        val executor = QueuedExecutor()
        val written = mutableListOf<Int>()
        val writer = LatestValueWriter(executor, written::add)

        writer.submit(1)
        writer.submit(2)
        writer.submit(3)

        assertEquals(1, executor.size)
        executor.runNext()
        assertEquals(listOf(3), written)
        assertEquals(0, executor.size)
    }

    @Test
    fun `a value submitted during a write is persisted afterward`() {
        val executor = QueuedExecutor()
        val written = mutableListOf<Int>()
        lateinit var writer: LatestValueWriter<Int>
        writer = LatestValueWriter(
            executor = executor,
            write = { value ->
                written += value
                if (value == 1) writer.submit(2)
            },
        )

        writer.submit(1)
        executor.runNext()

        assertEquals(listOf(1, 2), written)
        assertEquals(0, executor.size)
    }

    private class QueuedExecutor : Executor {
        private val tasks = ArrayDeque<Runnable>()
        val size: Int get() = tasks.size

        override fun execute(command: Runnable) {
            tasks.addLast(command)
        }

        fun runNext() {
            tasks.removeFirst().run()
        }
    }
}
