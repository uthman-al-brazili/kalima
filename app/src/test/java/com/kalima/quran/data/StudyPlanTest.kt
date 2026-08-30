package com.kalima.quran.data

import com.kalima.quran.localization.AppLanguage
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyPlanTest {
    private val corpus by lazy {
        val asset = sequenceOf(
            java.io.File("src/main/assets/${VocabularyAssetLoader.ASSET_NAME}.gz"),
            java.io.File("app/src/main/assets/${VocabularyAssetLoader.ASSET_NAME}.gz"),
        ).first(java.io.File::isFile)
        VocabularyAssetLoader.load(asset.inputStream(), AppLanguage.English)
    }

    private val prayerWords by lazy {
        corpus.filter { it.surahNumber in setOf(1, 112, 113, 114) }
    }

    @Test
    fun `guided focus precedes supporting vocabulary and removes overlaps`() {
        val progress = StudyProgress(
            selectedStudyScopes = setOf(StudyScope.Prayer),
            activeUnderstandPath = UnderstandPathId.AlFatihahSevenDays,
            activeUnderstandPathStage = 0,
        )

        val selection = StudyPlan.calculate(progress, corpus, prayerWords)
        val focusIds = selection.focusWords.map(QuranWord::id)

        assertTrue(focusIds.isNotEmpty())
        assertEquals(focusIds, selection.combinedWords.take(focusIds.size).map(QuranWord::id))
        assertEquals(
            selection.combinedWords.size,
            selection.combinedWords.distinctBy(QuranWord::id).size,
        )
        val focusConcepts = selection.focusWords.mapTo(mutableSetOf(), ::understandingConceptKey)
        assertTrue(
            selection.combinedWords.drop(focusIds.size)
                .none { understandingConceptKey(it) in focusConcepts },
        )
    }

    @Test
    fun `supporting set cannot introduce a future path word early`() {
        val progress = StudyProgress(
            selectedStudyScopes = setOf(StudyScope.Prayer),
            activeUnderstandPath = UnderstandPathId.AlFatihahSevenDays,
            activeUnderstandPathStage = 0,
        )
        val initial = StudyPlan.calculate(progress, corpus, prayerWords)
        val futureWord = requireNotNull(
            initial.focus?.targetWords?.firstOrNull { target ->
                initial.focusWords.none { it.id == target.id }
            },
        )

        assertFalse(initial.combinedWords.any { it.id == futureWord.id })

        val established = StudyPlan.calculate(
            progress.copy(reviewingIds = setOf(futureWord.id)),
            corpus,
            prayerWords,
        )
        assertTrue(established.combinedWords.any { it.id == futureWord.id })
    }

    @Test
    fun `queue puts all due reviews before guided and supporting new words`() {
        val now = Instant.parse("2026-08-30T12:00:00Z")
        val focus = WordRepository.words[0]
        val supportingDue = WordRepository.words[1]
        val supportingNew = WordRepository.words[2]
        val queue = StudyPlan.orderedQueue(
            words = listOf(focus, supportingDue, supportingNew),
            focusWordIds = setOf(focus.id),
            schedules = mapOf(
                supportingDue.id to ReviewSchedule(
                    repetitions = 1,
                    intervalDays = 1,
                    easeFactor = 2.5,
                    dueAt = now.minusSeconds(60),
                    lastReviewedAt = now.minusSeconds(86_400),
                    lapses = 0,
                ),
            ),
            spacedRepetitionEnabled = true,
            now = now,
            newStartIndex = 0,
        )

        assertEquals(listOf(supportingDue.id, focus.id, supportingNew.id), queue.map(QuranWord::id))
    }
}
