package com.kalima.quran.data

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
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
            alreadyKnownIds = setOf("word-2"),
            reviewSchedules = mapOf("word-1" to schedule),
            customStudyIds = setOf("word-2"),
            reviewEvents = listOf(ReviewEvent(now, "word-1", true, false, ReviewSource.Study)),
            lockScreenCooldownMinutes = 15,
            showCompleteAyah = true,
            quranFontSizeSp = 34,
            alphabetCourseRequested = true,
            alphabetFoundationRequired = true,
            numberCourseRequested = true,
            completedAlphabetLessons = 3,
            completedNumberLessons = 7,
        )
        val encoded = ProgressBackupCodec.encode(progress, "0.17.0", "corpus", now)
        val decoded = ProgressBackupCodec.decode(encoded, "corpus", knownIds)
        assertEquals(now, decoded.metadata.createdAt)
        assertEquals("0.17.0", decoded.metadata.appVersion)
        assertEquals(progress.learnedIds, decoded.progress.learnedIds)
        assertEquals(progress.alreadyKnownIds, decoded.progress.alreadyKnownIds)
        assertEquals(progress.reviewSchedules, decoded.progress.reviewSchedules)
        assertEquals(progress.customStudyIds, decoded.progress.customStudyIds)
        assertEquals(15, decoded.progress.lockScreenCooldownMinutes)
        assertEquals(true, decoded.progress.showCompleteAyah)
        assertEquals(34, decoded.progress.quranFontSizeSp)
        assertEquals(true, decoded.progress.alphabetCourseRequested)
        assertEquals(true, decoded.progress.alphabetFoundationRequired)
        assertEquals(true, decoded.progress.numberCourseRequested)
        assertEquals(3, decoded.progress.completedAlphabetLessons)
        assertEquals(7, decoded.progress.completedNumberLessons)
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

    @Test
    fun backupsCreatedBeforeAlreadyKnownWordsRemainReadable() {
        val current = ProgressBackupCodec.encode(StudyProgress(), "0.18.0", "corpus", now)
        val encodedPayload = current.lineSequence().first { it.startsWith("payload=") }
            .removePrefix("payload=")
        val payload = String(
            Base64.getUrlDecoder().decode(encodedPayload),
            StandardCharsets.UTF_8,
        )
        val legacyPayload = payload.lineSequence()
            .filterNot { it.startsWith("alreadyKnownIds\t") }
            .joinToString("\n")
        val legacyEncodedPayload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(legacyPayload.toByteArray(StandardCharsets.UTF_8))
        val legacyChecksum = MessageDigest.getInstance("SHA-256")
            .digest(legacyPayload.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val legacy = listOf(
            "#kalima-progress-backup-v1",
            "sha256=$legacyChecksum",
            "payload=$legacyEncodedPayload",
        ).joinToString("\n")

        assertEquals(
            emptySet<String>(),
            ProgressBackupCodec.decode(legacy, "corpus", knownIds).progress.alreadyKnownIds,
        )
    }

    @Test
    fun backupsCreatedBeforeTheAyahPreferenceDefaultToHidden() {
        val current = ProgressBackupCodec.encode(StudyProgress(), "0.22.0", "corpus", now)
        val encodedPayload = current.lineSequence().first { it.startsWith("payload=") }
            .removePrefix("payload=")
        val payload = String(
            Base64.getUrlDecoder().decode(encodedPayload),
            StandardCharsets.UTF_8,
        )
        val legacyPayload = payload.lineSequence()
            .filterNot { it.startsWith("showCompleteAyah\t") }
            .joinToString("\n")
        val legacyEncodedPayload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(legacyPayload.toByteArray(StandardCharsets.UTF_8))
        val legacyChecksum = MessageDigest.getInstance("SHA-256")
            .digest(legacyPayload.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val legacy = listOf(
            "#kalima-progress-backup-v1",
            "sha256=$legacyChecksum",
            "payload=$legacyEncodedPayload",
        ).joinToString("\n")

        assertEquals(
            false,
            ProgressBackupCodec.decode(legacy, "corpus", knownIds).progress.showCompleteAyah,
        )
    }

    @Test
    fun backupsCreatedBeforeReaderSizingUseTheReadableDefault() {
        val current = ProgressBackupCodec.encode(StudyProgress(), "0.24.7", "corpus", now)
        val encodedPayload = current.lineSequence().first { it.startsWith("payload=") }
            .removePrefix("payload=")
        val payload = String(
            Base64.getUrlDecoder().decode(encodedPayload),
            StandardCharsets.UTF_8,
        )
        val legacyPayload = payload.lineSequence()
            .filterNot { it.startsWith("quranFontSizeSp\t") }
            .joinToString("\n")
        val legacyEncodedPayload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(legacyPayload.toByteArray(StandardCharsets.UTF_8))
        val legacyChecksum = MessageDigest.getInstance("SHA-256")
            .digest(legacyPayload.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val legacy = listOf(
            "#kalima-progress-backup-v1",
            "sha256=$legacyChecksum",
            "payload=$legacyEncodedPayload",
        ).joinToString("\n")

        assertEquals(
            QuranReaderTypography.DEFAULT_FONT_SIZE_SP,
            ProgressBackupCodec.decode(legacy, "corpus", knownIds).progress.quranFontSizeSp,
        )
    }

    @Test
    fun backupsCreatedBeforeFoundationCoursesKeepExistingLearnersUnblocked() {
        val current = ProgressBackupCodec.encode(StudyProgress(), "0.25.0", "corpus", now)
        val encodedPayload = current.lineSequence().first { it.startsWith("payload=") }
            .removePrefix("payload=")
        val payload = String(
            Base64.getUrlDecoder().decode(encodedPayload),
            StandardCharsets.UTF_8,
        )
        val legacyPayload = payload.lineSequence()
            .filterNot { line ->
                line.startsWith("alphabetCourseRequested\t") ||
                    line.startsWith("alphabetFoundationRequired\t") ||
                    line.startsWith("numberCourseRequested\t") ||
                    line.startsWith("completedAlphabetLessons\t") ||
                    line.startsWith("completedNumberLessons\t")
            }
            .joinToString("\n")
        val legacyEncodedPayload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(legacyPayload.toByteArray(StandardCharsets.UTF_8))
        val legacyChecksum = MessageDigest.getInstance("SHA-256")
            .digest(legacyPayload.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val legacy = listOf(
            "#kalima-progress-backup-v1",
            "sha256=$legacyChecksum",
            "payload=$legacyEncodedPayload",
        ).joinToString("\n")

        val progress = ProgressBackupCodec.decode(legacy, "corpus", knownIds).progress
        assertEquals(false, progress.alphabetCourseRequested)
        assertEquals(false, progress.numberCourseRequested)
        assertEquals(false, progress.needsAlphabetFoundation)
    }

    @Test
    fun backupsFromVoluntaryAlphabetReviewKeepEstablishedLearnersUnblocked() {
        val reviewingAlphabet = StudyProgress(
            learnedIds = setOf("word-1"),
            alphabetCourseRequested = true,
            alphabetFoundationRequired = false,
            completedAlphabetLessons = 0,
        )
        val legacy = withoutPayloadField(
            ProgressBackupCodec.encode(reviewingAlphabet, "0.27.2", "corpus", now),
            "alphabetFoundationRequired",
        )

        val progress = ProgressBackupCodec.decode(legacy, "corpus", knownIds).progress
        assertEquals(true, progress.hasAlphabetFoundationLesson)
        assertEquals(false, progress.needsAlphabetFoundation)
    }

    private fun withoutPayloadField(backup: String, field: String): String {
        val encodedPayload = backup.lineSequence().first { it.startsWith("payload=") }
            .removePrefix("payload=")
        val payload = String(
            Base64.getUrlDecoder().decode(encodedPayload),
            StandardCharsets.UTF_8,
        )
        val legacyPayload = payload.lineSequence()
            .filterNot { it.startsWith("$field\t") }
            .joinToString("\n")
        val legacyEncodedPayload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(legacyPayload.toByteArray(StandardCharsets.UTF_8))
        val legacyChecksum = MessageDigest.getInstance("SHA-256")
            .digest(legacyPayload.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return listOf(
            "#kalima-progress-backup-v1",
            "sha256=$legacyChecksum",
            "payload=$legacyEncodedPayload",
        ).joinToString("\n")
    }
}
