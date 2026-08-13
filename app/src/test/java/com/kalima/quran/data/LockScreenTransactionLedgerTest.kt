package com.kalima.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockScreenTransactionLedgerTest {
    @Test
    fun completedSessionsAreBoundedAndIdempotent() {
        var completed = emptySet<String>()
        repeat(70) { completed = LockScreenTransactionLedger.append(completed, "session-$it") }
        assertEquals(64, completed.size)
        assertFalse("session-0" in completed)
        assertTrue("session-69" in completed)
        assertEquals(completed, LockScreenTransactionLedger.append(completed, "session-69"))
    }
}
