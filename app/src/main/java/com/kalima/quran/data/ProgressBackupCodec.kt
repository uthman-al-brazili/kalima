package com.kalima.quran.data

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

data class ProgressBackupMetadata(
    val createdAt: Instant,
    val appVersion: String,
    val corpusIdentity: String,
)

data class DecodedProgressBackup(
    val metadata: ProgressBackupMetadata,
    val progress: StudyProgress,
)

class ProgressBackupValidationException(message: String) : IllegalArgumentException(message)

object ProgressBackupCodec {
    const val FILE_EXTENSION = "kalima-backup"
    private const val HEADER = "#kalima-progress-backup-v1"
    private const val PAYLOAD_MARKER = "payload="
    private const val CHECKSUM_MARKER = "sha256="

    fun encode(
        progress: StudyProgress,
        appVersion: String,
        corpusIdentity: String,
        createdAt: Instant = Instant.now(),
    ): String {
        val values = linkedMapOf(
            "createdAt" to createdAt.toString(),
            "appVersion" to appVersion,
            "corpusIdentity" to corpusIdentity,
            "learnedIds" to encodeSet(progress.learnedIds),
            "reviewingIds" to encodeSet(progress.reviewingIds),
            "alreadyKnownIds" to encodeSet(progress.alreadyKnownIds),
            "todayAnsweredIds" to encodeSet(progress.todayAnsweredIds),
            "dailyGoal" to progress.dailyGoal.toString(),
            "maximumWords" to progress.maximumWords.toString(),
            "streakDays" to progress.streakDays.toString(),
            "reminderEnabled" to progress.reminderEnabled.toString(),
            "lockScreenEnabled" to progress.lockScreenEnabled.toString(),
            "studyScope" to progress.studyScope.name,
            "selectedStudyScopes" to progress.studyScopes
                .sortedBy(StudyScope::ordinal)
                .joinToString(",") { it.name },
            "selectedSurahs" to progress.selectedSurahs.sorted().joinToString(","),
            "quizCorrectDays" to encodeSet(
                progress.quizCorrectDays.mapTo(mutableSetOf()) { (id, days) ->
                    "$id|${days.sorted().joinToString(",")}"
                },
            ),
            "quizCorrectAnswers" to progress.quizCorrectAnswers.toString(),
            "quizTotalAnswers" to progress.quizTotalAnswers.toString(),
            "lockScreenQuizEnabled" to progress.lockScreenQuizEnabled.toString(),
            "lockScreenQuizInterval" to progress.lockScreenQuizInterval.toString(),
            "themeMode" to progress.themeMode.name,
            "quranFontSizeSp" to progress.quranFontSizeSp.toString(),
            "quranLearningOverlayEnabled" to progress.quranLearningOverlayEnabled.toString(),
            "advancedSettingsVisible" to progress.advancedSettingsVisible.toString(),
            "showCompleteAyah" to progress.showCompleteAyah.toString(),
            "spacedRepetitionEnabled" to progress.spacedRepetitionEnabled.toString(),
            "currentStudyWordId" to progress.currentStudyWordId.orEmpty(),
            "reviewSchedules" to encodeSet(ReviewScheduleCodec.encode(progress.reviewSchedules)),
            "alphabetReviewSchedules" to encodeSet(
                ReviewScheduleCodec.encode(progress.alphabetReviewSchedules),
            ),
            // Retained as an empty field so v1 backups remain readable by older Kalima versions.
            "favoriteIds" to encodeSet(emptySet()),
            "customStudyIds" to encodeSet(progress.customStudyIds),
            "onboardingComplete" to progress.onboardingComplete.toString(),
            "alphabetCourseRequested" to progress.alphabetCourseRequested.toString(),
            "alphabetFoundationRequired" to progress.alphabetFoundationRequired.toString(),
            "numberCourseRequested" to progress.numberCourseRequested.toString(),
            "completedAlphabetLessons" to progress.completedAlphabetLessons.toString(),
            "completedNumberLessons" to progress.completedNumberLessons.toString(),
            "reviewEvents" to encodeSet(ReviewEventCodec.encode(progress.reviewEvents)),
            "quietHoursEnabled" to progress.quietHoursEnabled.toString(),
            "quietStartHour" to progress.quietStartHour.toString(),
            "quietEndHour" to progress.quietEndHour.toString(),
            "lockScreenDailyLimit" to progress.lockScreenDailyLimit.toString(),
            "lockScreenCooldownMinutes" to progress.lockScreenCooldownMinutes.toString(),
            "lockScreenCardsToday" to progress.lockScreenCardsToday.toString(),
            "lockScreenPausedUntil" to progress.lockScreenPausedUntil?.toString().orEmpty(),
            "lastLockScreenShownAt" to progress.lastLockScreenShownAt?.toString().orEmpty(),
        )
        val payload = values.entries.joinToString("\n") { (key, value) ->
            "$key\t${encodeText(value)}"
        }
        val encodedPayload = encodeText(payload)
        return listOf(
            HEADER,
            "$CHECKSUM_MARKER${sha256(payload)}",
            "$PAYLOAD_MARKER$encodedPayload",
        ).joinToString("\n")
    }

    fun decode(
        backup: String,
        expectedCorpusIdentity: String,
        knownWordIds: Set<String>,
    ): DecodedProgressBackup {
        val lines = backup.lineSequence().filter(String::isNotBlank).toList()
        if (lines.firstOrNull() != HEADER) throw invalid("Unsupported backup format")
        val expectedChecksum = lines.firstOrNull { it.startsWith(CHECKSUM_MARKER) }
            ?.removePrefix(CHECKSUM_MARKER)
            ?: throw invalid("Backup checksum is missing")
        val payload = lines.firstOrNull { it.startsWith(PAYLOAD_MARKER) }
            ?.removePrefix(PAYLOAD_MARKER)
            ?.let(::decodeText)
            ?: throw invalid("Backup payload is missing")
        if (!sha256(payload).equals(expectedChecksum, ignoreCase = true)) {
            throw invalid("Backup checksum does not match")
        }
        val values = payload.lineSequence().associate { line ->
            val separator = line.indexOf('\t')
            if (separator <= 0) throw invalid("Backup payload is malformed")
            line.substring(0, separator) to decodeText(line.substring(separator + 1))
        }
        val metadata = ProgressBackupMetadata(
            createdAt = values.required("createdAt").let(Instant::parse),
            appVersion = values.required("appVersion"),
            corpusIdentity = values.required("corpusIdentity"),
        )
        if (metadata.corpusIdentity != expectedCorpusIdentity) {
            throw invalid("Backup belongs to an incompatible Quran corpus")
        }

        val learned = values.idSet("learnedIds", knownWordIds)
        val reviewing = values.idSet("reviewingIds", knownWordIds)
        val alreadyKnown = values.optionalIdSet("alreadyKnownIds", knownWordIds)
        val schedules = values.encodedSet("reviewSchedules").let { entries ->
            ReviewScheduleCodec.decode(entries).also { decoded ->
                if (decoded.size != entries.size || decoded.keys.any { it !in knownWordIds }) {
                    throw invalid("Backup contains invalid review schedules")
                }
            }
        }
        val reviewEvents = values.encodedSet("reviewEvents").let { entries ->
            ReviewEventCodec.decode(entries).also { decoded ->
                if (decoded.size != entries.size || decoded.any { it.wordId !in knownWordIds }) {
                    throw invalid("Backup contains invalid review history")
                }
            }
        }
        val alphabetSchedules = values.optionalEncodedSet("alphabetReviewSchedules").let { entries ->
            ReviewScheduleCodec.decode(entries).also { decoded ->
                if (decoded.size != entries.size ||
                    decoded.keys.any { it !in ArabicFoundations.allMasteryKeys }
                ) {
                    throw invalid("Backup contains invalid alphabet review schedules")
                }
            }
        }
        val quizCorrectDays = values.encodedSet("quizCorrectDays").associate { entry ->
            val separator = entry.indexOf('|')
            if (separator <= 0) throw invalid("Backup contains invalid quiz history")
            val id = entry.substring(0, separator)
            if (id !in knownWordIds) throw invalid("Backup references an unknown word")
            id to entry.substring(separator + 1).split(',').filter(String::isNotBlank).toSet()
        }
        val currentWordId = values.required("currentStudyWordId").ifBlank { null }
        if (currentWordId != null && currentWordId !in knownWordIds) {
            throw invalid("Backup references an unknown current word")
        }
        val customStudyIds = mergePersonalCollections(
            legacyFavoriteIds = values.idSet("favoriteIds", knownWordIds),
            customStudyIds = values.idSet("customStudyIds", knownWordIds),
        )
        val alphabetCourseRequested = values.optionalBoolean(
            "alphabetCourseRequested",
            false,
        )
        val completedAlphabetLessons = values.optionalInt(
            "completedAlphabetLessons",
            0,
        ).coerceIn(0, ArabicFoundations.alphabetLessonCount)
        val alphabetFoundationRequired = values.optionalBoolean(
            "alphabetFoundationRequired",
            alphabetCourseRequested &&
                completedAlphabetLessons < ArabicFoundations.alphabetLessonCount &&
                learned.isEmpty() && reviewing.isEmpty() && alreadyKnown.isEmpty(),
        )
        val legacyStudyScope = StudyScope.fromPersistedName(values.required("studyScope"))
            ?: throw invalid("Backup has an invalid study scope")
        val selectedStudyScopes = values["selectedStudyScopes"]
            ?.takeIf(String::isNotBlank)
            ?.split(',')
            ?.map { stored ->
                StudyScope.fromPersistedName(stored)
                    ?: throw invalid("Backup has an invalid selected study scope")
            }
            ?.toSet()
            .orEmpty()
        val progress = StudyProgress(
            learnedIds = learned,
            reviewingIds = reviewing,
            alreadyKnownIds = alreadyKnown,
            todayAnsweredIds = values.idSet("todayAnsweredIds", knownWordIds),
            dailyGoal = values.int("dailyGoal").coerceIn(3, 20),
            maximumWords = values.int("maximumWords"),
            streakDays = values.int("streakDays").coerceAtLeast(0),
            reminderEnabled = values.boolean("reminderEnabled"),
            lockScreenEnabled = values.boolean("lockScreenEnabled"),
            studyScope = legacyStudyScope,
            selectedStudyScopes = selectedStudyScopes.ifEmpty { setOf(legacyStudyScope) },
            selectedSurahs = values.required("selectedSurahs").split(',')
                .mapNotNull(String::toIntOrNull).filterTo(mutableSetOf()) { it in 1..114 },
            quizCorrectDays = quizCorrectDays,
            quizCorrectAnswers = values.int("quizCorrectAnswers").coerceAtLeast(0),
            quizTotalAnswers = values.int("quizTotalAnswers").coerceAtLeast(0),
            lockScreenQuizEnabled = values.boolean("lockScreenQuizEnabled"),
            lockScreenQuizInterval = values.int("lockScreenQuizInterval").coerceIn(1, 10),
            themeMode = AppThemeMode.entries.firstOrNull { it.name == values.required("themeMode") }
                ?: throw invalid("Backup has an invalid theme"),
            quranFontSizeSp = QuranReaderTypography.normalize(
                values.optionalInt(
                    "quranFontSizeSp",
                    QuranReaderTypography.DEFAULT_FONT_SIZE_SP,
                ),
            ),
            quranLearningOverlayEnabled = values.optionalBoolean(
                "quranLearningOverlayEnabled",
                false,
            ),
            advancedSettingsVisible = values.boolean("advancedSettingsVisible"),
            showCompleteAyah = values.optionalBoolean("showCompleteAyah", false),
            spacedRepetitionEnabled = values.boolean("spacedRepetitionEnabled"),
            currentStudyWordId = currentWordId,
            reviewSchedules = schedules,
            alphabetReviewSchedules = alphabetSchedules,
            customStudyIds = customStudyIds,
            onboardingComplete = values.boolean("onboardingComplete"),
            alphabetCourseRequested = alphabetCourseRequested,
            alphabetFoundationRequired = alphabetFoundationRequired,
            numberCourseRequested = values.optionalBoolean(
                "numberCourseRequested",
                false,
            ),
            completedAlphabetLessons = completedAlphabetLessons,
            completedNumberLessons = values.optionalInt(
                "completedNumberLessons",
                0,
            ).coerceIn(0, ArabicFoundations.numberLessonCount),
            reviewEvents = reviewEvents,
            quietHoursEnabled = values.boolean("quietHoursEnabled"),
            quietStartHour = values.int("quietStartHour").coerceIn(0, 23),
            quietEndHour = values.int("quietEndHour").coerceIn(0, 23),
            lockScreenDailyLimit = values.int("lockScreenDailyLimit").coerceIn(0, 100),
            lockScreenCooldownMinutes = values.int("lockScreenCooldownMinutes").coerceIn(0, 120),
            lockScreenCardsToday = values.int("lockScreenCardsToday").coerceAtLeast(0),
            lockScreenPausedUntil = values.required("lockScreenPausedUntil")
                .ifBlank { null }?.let(Instant::parse),
            lastLockScreenShownAt = values.required("lastLockScreenShownAt")
                .ifBlank { null }?.let(Instant::parse),
        )
        return DecodedProgressBackup(metadata, progress)
    }

    private fun Map<String, String>.required(key: String): String =
        this[key] ?: throw invalid("Backup field '$key' is missing")

    private fun Map<String, String>.int(key: String): Int =
        required(key).toIntOrNull() ?: throw invalid("Backup field '$key' is invalid")

    private fun Map<String, String>.optionalInt(key: String, default: Int): Int =
        this[key]?.toIntOrNull() ?: if (key in this) {
            throw invalid("Backup field '$key' is invalid")
        } else {
            default
        }

    private fun Map<String, String>.boolean(key: String): Boolean = when (val value = required(key)) {
        "true" -> true
        "false" -> false
        else -> throw invalid("Backup field '$key' is invalid: $value")
    }

    private fun Map<String, String>.optionalBoolean(key: String, default: Boolean): Boolean =
        this[key]?.let { value ->
            when (value) {
                "true" -> true
                "false" -> false
                else -> throw invalid("Backup field '$key' is invalid: $value")
            }
        } ?: default

    private fun Map<String, String>.encodedSet(key: String): Set<String> =
        decodeSet(required(key))

    private fun Map<String, String>.optionalEncodedSet(key: String): Set<String> =
        this[key]?.let(::decodeSet).orEmpty()

    private fun Map<String, String>.idSet(key: String, knownWordIds: Set<String>): Set<String> =
        encodedSet(key).also { ids ->
            if (ids.any { it !in knownWordIds }) throw invalid("Backup references an unknown word")
        }

    private fun Map<String, String>.optionalIdSet(
        key: String,
        knownWordIds: Set<String>,
    ): Set<String> = this[key]?.let(::decodeSet).orEmpty().also { ids ->
        if (ids.any { it !in knownWordIds }) throw invalid("Backup references an unknown word")
    }

    private fun encodeSet(values: Set<String>): String =
        values.sorted().joinToString("\n", transform = ::encodeText)

    private fun decodeSet(value: String): Set<String> = if (value.isBlank()) {
        emptySet()
    } else {
        value.lineSequence().map(::decodeText).toSet()
    }

    private fun encodeText(value: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeText(value: String): String = try {
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    } catch (_: IllegalArgumentException) {
        throw invalid("Backup contains invalid encoded text")
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun invalid(message: String) = ProgressBackupValidationException(message)
}
