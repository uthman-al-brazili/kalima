package com.kalima.quran.data

import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

enum class ReviewGrade {
    Again,
    Good,
}

data class ReviewSchedule(
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Double = SpacedRepetition.INITIAL_EASE_FACTOR,
    val dueAt: Instant,
    val lastReviewedAt: Instant,
    val lapses: Int = 0,
) {
    fun isDue(now: Instant): Boolean = !dueAt.isAfter(now)

    val isLearned: Boolean get() = repetitions >= SpacedRepetition.LEARNED_REPETITIONS
}

/**
 * A compact SM-2-style scheduler with an intraday relearning step.
 *
 * Successful recalls graduate from one day to three days and then expand using
 * the card's ease factor. A forgotten card returns after ten minutes, loses
 * some ease, and must graduate again. Early successful reviews never inflate
 * the interval.
 */
object SpacedRepetition {
    const val INITIAL_EASE_FACTOR = 2.5
    const val MINIMUM_EASE_FACTOR = 1.3
    const val LEARNED_REPETITIONS = 2
    const val AGAIN_DELAY_MINUTES = 10L

    fun introduce(now: Instant): ReviewSchedule = ReviewSchedule(
        repetitions = 0,
        intervalDays = 0,
        easeFactor = INITIAL_EASE_FACTOR,
        dueAt = now.plus(Duration.ofMinutes(AGAIN_DELAY_MINUTES)),
        lastReviewedAt = now,
        lapses = 0,
    )

    fun review(
        previous: ReviewSchedule?,
        grade: ReviewGrade,
        now: Instant,
    ): ReviewSchedule = when (grade) {
        ReviewGrade.Again -> ReviewSchedule(
            repetitions = 0,
            intervalDays = 0,
            easeFactor = ((previous?.easeFactor ?: INITIAL_EASE_FACTOR) - 0.2)
                .coerceAtLeast(MINIMUM_EASE_FACTOR),
            dueAt = now.plus(Duration.ofMinutes(AGAIN_DELAY_MINUTES)),
            lastReviewedAt = now,
            lapses = (previous?.lapses ?: 0) + 1,
        )

        ReviewGrade.Good -> {
            if (previous != null && !previous.isDue(now)) {
                previous
            } else {
                val repetitions = (previous?.repetitions ?: 0) + 1
                val intervalDays = nextGoodIntervalDays(previous)
                ReviewSchedule(
                    repetitions = repetitions,
                    intervalDays = intervalDays,
                    easeFactor = previous?.easeFactor ?: INITIAL_EASE_FACTOR,
                    dueAt = now.plus(Duration.ofDays(intervalDays.toLong())),
                    lastReviewedAt = now,
                    lapses = previous?.lapses ?: 0,
                )
            }
        }
    }

    fun nextGoodIntervalDays(previous: ReviewSchedule?): Int = when (previous?.repetitions ?: 0) {
        0 -> 1
        1 -> 3
        else -> maxOf(
            (previous?.intervalDays ?: 1) + 1,
            ((previous?.intervalDays ?: 1) *
                (previous?.easeFactor ?: INITIAL_EASE_FACTOR)).roundToInt(),
        )
    }

    fun migrated(
        learned: Boolean,
        now: Instant,
    ): ReviewSchedule = ReviewSchedule(
        repetitions = if (learned) LEARNED_REPETITIONS else 0,
        intervalDays = if (learned) 3 else 0,
        dueAt = now,
        lastReviewedAt = now,
    )
}

object ReviewQueue {
    fun rotated(
        words: List<QuranWord>,
        startIndex: Int,
    ): List<QuranWord> {
        if (words.isEmpty()) return emptyList()
        val start = Math.floorMod(startIndex, words.size)
        return words.drop(start) + words.take(start)
    }

    fun ordered(
        words: List<QuranWord>,
        schedules: Map<String, ReviewSchedule>,
        now: Instant,
        newStartIndex: Int = 0,
    ): List<QuranWord> {
        val due = dueWords(words, schedules, now)
        val new = newWords(words, schedules)
        if (new.isEmpty()) return due
        return due + rotated(new, newStartIndex)
    }

    fun dueWords(
        words: List<QuranWord>,
        schedules: Map<String, ReviewSchedule>,
        now: Instant,
    ): List<QuranWord> = words
        .mapIndexedNotNull { index, word ->
            schedules[word.id]
                ?.takeIf { it.isDue(now) }
                ?.let { schedule -> DueWord(word, schedule.dueAt, index) }
        }
        .sortedWith(compareBy<DueWord> { it.dueAt }.thenBy { it.sourceIndex })
        .map(DueWord::word)

    fun newWords(
        words: List<QuranWord>,
        schedules: Map<String, ReviewSchedule>,
    ): List<QuranWord> = words.filterNot { schedules.containsKey(it.id) }

    private data class DueWord(
        val word: QuranWord,
        val dueAt: Instant,
        val sourceIndex: Int,
    )
}

object ReviewScheduleCodec {
    private const val SEPARATOR = '|'

    fun encode(schedules: Map<String, ReviewSchedule>): Set<String> =
        schedules.mapTo(mutableSetOf()) { (id, schedule) ->
            listOf(
                id,
                schedule.repetitions,
                schedule.intervalDays,
                (schedule.easeFactor * 1_000).roundToInt(),
                schedule.dueAt.toEpochMilli(),
                schedule.lastReviewedAt.toEpochMilli(),
                schedule.lapses,
            ).joinToString(SEPARATOR.toString())
        }

    fun decode(entries: Set<String>): Map<String, ReviewSchedule> =
        entries.mapNotNull(::decodeEntry).toMap()

    private fun decodeEntry(entry: String): Pair<String, ReviewSchedule>? {
        val fields = entry.split(SEPARATOR)
        if (fields.size != 7 || fields[0].isBlank()) return null
        return runCatching {
            val schedule = ReviewSchedule(
                repetitions = fields[1].toInt().coerceAtLeast(0),
                intervalDays = fields[2].toInt().coerceAtLeast(0),
                easeFactor = (fields[3].toInt() / 1_000.0)
                    .coerceAtLeast(SpacedRepetition.MINIMUM_EASE_FACTOR),
                dueAt = Instant.ofEpochMilli(fields[4].toLong()),
                lastReviewedAt = Instant.ofEpochMilli(fields[5].toLong()),
                lapses = fields[6].toInt().coerceAtLeast(0),
            )
            fields[0] to schedule
        }.getOrNull()
    }
}
