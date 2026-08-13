package com.kalima.quran.data

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ProgressBackupCodecTest {
    private val now = Instant.parse("2026-08-13T12:00:00Z")
    private val knownIds = setOf("word-1", "word-2")

    @Test
    fun roundTripsProgressAndMetadata() {
        val schedule = ReviewSchedule(2, 3, 2.5, now.plusSeconds(300), now, 1)
        val progress = StudyProgress(
            learnedIds = setOf("word-1"),
            reviewingIds = setOf("word-2"),
            reviewSchedules = mapOf("word-1" to schedule),
            customStudyIds = setOf("word-2"),
            reviewEvents = listOf(ReviewEvent(now, "word-1", true, false, ReviewSource.Study)),
            lockScreenCooldownMinutes = 15,
        )
        val encoded = ProgressBackupCodec.encode(progress, "0.17.0", "corpus", now)
        val decoded = ProgressBackupCodec.decode(encoded, "corpus", knownIds)
        assertEquals(now, decoded.metadata.createdAt)
        assertEquals("0.17.0", decoded.metadata.appVersion)
        assertEquals(progress.learnedIds, decoded.progress.learnedIds)
        assertEquals(progress.reviewSchedules, decoded.progress.reviewSchedules)
        assertEquals(progress.customStudyIds, decoded.progress.customStudyIds)
        assertEquals(15, decoded.progress.lockScreenCooldownMinutes)
    }

    @Test
    fun tamperingAndUnknownCorporaAreRejected() {
        val encoded = ProgressBackupCodec.encode(StudyProgress(), "0.17.0", "corpus", now)
        assertThrows(ProgressBackupValidationException::class.java) {
            ProgressBackupCodec.decode(encoded.replace("payload=", "payload=x"), "corpus", knownIds)
        }
        assertThrows(ProgressBackupValidationException::class.java) {
            ProgressBackupCodec.decode(encoded, "different", knownIds)
        }
    }
}
