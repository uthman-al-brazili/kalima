package com.kalima.quran.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class ReviewSource {
    Study,
    Quiz,
    LockScreen,
}

data class ReviewEvent(
    val timestamp: Instant,
    val wordId: String,
    val correct: Boolean,
    val wasNew: Boolean,
    val source: ReviewSource,
)

object ReviewHistory {
    private const val MAX_EVENTS = 1_000

    fun append(events: List<ReviewEvent>, event: ReviewEvent): List<ReviewEvent> =
        (events + event).takeLast(MAX_EVENTS)

    fun accuracy(
        events: List<ReviewEvent>,
        days: Long,
        now: Instant = Instant.now(),
    ): Int? {
        val recent = events.filter { it.timestamp >= now.minusSeconds(days * 86_400) }
        if (recent.isEmpty()) return null
        return (recent.count(ReviewEvent::correct) * 100f / recent.size).toInt()
    }

    fun countByDay(
        events: List<ReviewEvent>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Map<LocalDate, Int> = events.groupingBy { it.timestamp.atZone(zoneId).toLocalDate() }.eachCount()
}

object ReviewEventCodec {
    fun encode(events: List<ReviewEvent>): Set<String> = events.mapIndexedTo(mutableSetOf()) { index, event ->
        listOf(
            event.timestamp.toEpochMilli(),
            index,
            event.wordId,
            if (event.correct) 1 else 0,
            if (event.wasNew) 1 else 0,
            event.source.name,
        ).joinToString("|")
    }

    fun decode(entries: Set<String>): List<ReviewEvent> = entries.mapNotNull { entry ->
        val parts = entry.split('|')
        if (parts.size != 6) return@mapNotNull null
        val timestamp = parts[0].toLongOrNull()?.let(Instant::ofEpochMilli) ?: return@mapNotNull null
        val source = ReviewSource.entries.firstOrNull { it.name == parts[5] } ?: return@mapNotNull null
        ReviewEvent(
            timestamp = timestamp,
            wordId = parts[2],
            correct = parts[3] == "1",
            wasNew = parts[4] == "1",
            source = source,
        )
    }.sortedBy(ReviewEvent::timestamp)
}
