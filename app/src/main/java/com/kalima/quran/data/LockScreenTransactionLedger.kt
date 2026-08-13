package com.kalima.quran.data

object LockScreenTransactionLedger {
    private const val MAX_COMPLETED = 64

    fun append(completed: Set<String>, sessionId: String): Set<String> {
        if (sessionId in completed) return completed
        return (completed.toList() + sessionId).takeLast(MAX_COMPLETED).toSet()
    }
}
