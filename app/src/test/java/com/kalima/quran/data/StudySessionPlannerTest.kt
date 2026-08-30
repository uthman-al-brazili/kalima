package com.kalima.quran.data

import com.kalima.quran.localization.AppLanguage
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudySessionPlannerTest {
    private val now = Instant.parse("2026-08-30T12:00:00Z")
    private val genericWords = (1..9).map { index -> testWord("word-$index") }

    private val corpus by lazy {
        val asset = sequenceOf(
            java.io.File("src/main/assets/${VocabularyAssetLoader.ASSET_NAME}.gz"),
            java.io.File("app/src/main/assets/${VocabularyAssetLoader.ASSET_NAME}.gz"),
        ).first(java.io.File::isFile)
        VocabularyAssetLoader.load(asset.inputStream(), AppLanguage.English)
    }

    @Test
    fun `Quick produces due reviews only and is caught up when none are due`() {
        val due = genericWords[0]
        val fresh = genericWords[1]
        val progress = StudyProgress(
            sessionLevel = SessionLevel.Quick,
            reviewSchedules = mapOf(due.id to dueSchedule()),
        )

        val plan = genericPlan(progress, listOf(fresh, due))
        val caughtUp = genericPlan(StudyProgress(sessionLevel = SessionLevel.Quick), genericWords)

        assertEquals(listOf(due.id), plan.wordIds)
        assertEquals(plan.wordIds, plan.reviewWordIds)
        assertTrue(plan.lessonWordIds.isEmpty())
        assertTrue(caughtUp.isEmpty)
    }

    @Test
    fun `Steady puts all due reviews before at most two new words`() {
        val due = genericWords.take(3)
        val progress = StudyProgress(
            sessionLevel = SessionLevel.Steady,
            reviewSchedules = due.associate { it.id to dueSchedule() },
        )

        val plan = genericPlan(progress, genericWords)

        assertEquals(due.map(QuranWord::id), plan.reviewWordIds)
        assertEquals(2, plan.lessonWordIds.size)
        assertEquals(plan.reviewWordIds + plan.lessonWordIds, plan.wordIds)
        assertFalse(plan.requestsContextCheckpoint)
    }

    @Test
    fun `Deep adds at most five new words and requests a checkpoint`() {
        val due = genericWords.first()
        val progress = StudyProgress(
            sessionLevel = SessionLevel.Deep,
            reviewSchedules = mapOf(due.id to dueSchedule()),
        )

        val plan = genericPlan(progress, genericWords)

        assertEquals(5, plan.lessonWordIds.size)
        assertEquals(due.id, plan.wordIds.first())
        assertTrue(plan.requestsContextCheckpoint)
    }

    @Test
    fun `plan IDs are unique and remain a stable snapshot after schedules change`() {
        val progress = StudyProgress(sessionLevel = SessionLevel.Steady)
        val plan = genericPlan(progress, genericWords + genericWords.reversed())
        val snapshot = plan.wordIds.toList()

        genericPlan(
            progress.copy(reviewSchedules = snapshot.associateWith { dueSchedule() }),
            genericWords,
        )

        assertEquals(snapshot, plan.wordIds)
        assertEquals(plan.wordIds.size, plan.wordIds.distinct().size)
        assertTrue(isStudySessionComplete(plan.wordIds, plan.wordIds))
        assertFalse(isStudySessionComplete(plan.wordIds, plan.wordIds.dropLast(1)))
    }

    @Test
    fun `guided Quick reviews earlier path content and leaves the current lesson waiting`() {
        val selection = guidedSelection(stageIndex = 1, level = SessionLevel.Quick)
        val focus = requireNotNull(selection.focus)
        val currentIds = stageIds(focus, 1)
        val earlier = stageIds(focus, 0).filterNot(currentIds::contains)
        val progress = guidedProgress(1, SessionLevel.Quick).copy(
            reviewSchedules = earlier.associateWith { dueSchedule() },
        )
        val refreshed = StudyPlan.calculate(progress, corpus, emptyList())

        val plan = StudySessionPlanner.build(progress, refreshed, focus.unlockedWords, now, 0)

        assertTrue(plan.lessonWordIds.isEmpty())
        assertTrue(plan.wordIds.none(currentIds::contains))
        assertTrue(plan.currentGuidedLessonWaiting)
    }

    @Test
    fun `guided Steady includes the complete current ayah beyond the generic limit`() {
        val progress = guidedProgress(6, SessionLevel.Steady)
        val selection = StudyPlan.calculate(progress, corpus, emptyList())
        val focus = requireNotNull(selection.focus)
        val currentIds = stageIds(focus, 6)

        val plan = StudySessionPlanner.build(progress, selection, focus.unlockedWords, now, 0)

        assertEquals(8, currentIds.size)
        assertEquals(currentIds, plan.lessonWordIds)
        assertEquals(8, plan.lessonWordIds.size)
        assertFalse(plan.requestsContextCheckpoint)
    }

    @Test
    fun `guided lesson does not offer words already completed today`() {
        val initialProgress = guidedProgress(6, SessionLevel.Steady)
        val initialSelection = StudyPlan.calculate(initialProgress, corpus, emptyList())
        val focus = requireNotNull(initialSelection.focus)
        val currentIds = stageIds(focus, 6)
        val progress = initialProgress.copy(todayAnsweredIds = currentIds.toSet())
        val selection = StudyPlan.calculate(progress, corpus, emptyList())

        val plan = StudySessionPlanner.build(
            progress,
            selection,
            requireNotNull(selection.focus).unlockedWords,
            now,
            0,
        )

        assertTrue(plan.isEmpty)
        assertTrue(plan.currentGuidedLessonCompletedToday)
    }

    @Test
    fun `guided lesson resumes with only words not completed today`() {
        val initialProgress = guidedProgress(6, SessionLevel.Steady)
        val initialSelection = StudyPlan.calculate(initialProgress, corpus, emptyList())
        val focus = requireNotNull(initialSelection.focus)
        val currentIds = stageIds(focus, 6)
        val completedIds = currentIds.take(3)
        val progress = initialProgress.copy(todayAnsweredIds = completedIds.toSet())
        val selection = StudyPlan.calculate(progress, corpus, emptyList())

        val plan = StudySessionPlanner.build(
            progress,
            selection,
            requireNotNull(selection.focus).unlockedWords,
            now,
            0,
        )

        assertEquals(currentIds.drop(3), plan.lessonWordIds)
        assertFalse(plan.currentGuidedLessonCompletedToday)
    }

    @Test
    fun `guided Deep uses the same ayah and prefers it for context without later stages`() {
        val stageIndex = 4
        val progress = guidedProgress(stageIndex, SessionLevel.Deep)
        val selection = StudyPlan.calculate(progress, corpus, emptyList())
        val focus = requireNotNull(selection.focus)
        val currentIds = stageIds(focus, stageIndex)
        val laterIds = focus.definition.stages.drop(stageIndex + 1)
            .flatMap { UnderstandPathCatalog.stageWords(it, focus.targetWords) }
            .mapTo(mutableSetOf(), QuranWord::id)

        val plan = StudySessionPlanner.build(progress, selection, focus.unlockedWords, now, 0)

        assertEquals(currentIds, plan.lessonWordIds)
        assertEquals(currentIds, plan.preferredContextWordIds)
        assertTrue(plan.requestsContextCheckpoint)
        assertTrue(plan.wordIds.none(laterIds::contains))
    }

    @Test
    fun `Al-Fatihah ayah three keeps its repeated vocabulary as a meaningful lesson`() {
        val progress = guidedProgress(2, SessionLevel.Steady)
        val selection = StudyPlan.calculate(progress, corpus, emptyList())
        val focus = requireNotNull(selection.focus)
        val repeatedIds = stageIds(focus, 2)
        val alreadyEncountered = stageIds(focus, 0)
        val withSchedules = progress.copy(
            reviewSchedules = alreadyEncountered.associateWith { dueSchedule() },
        )
        val refreshed = StudyPlan.calculate(withSchedules, corpus, emptyList())

        val plan = StudySessionPlanner.build(
            withSchedules,
            refreshed,
            requireNotNull(refreshed.focus).unlockedWords,
            now,
            0,
        )

        assertEquals(2, repeatedIds.size)
        assertEquals(repeatedIds, plan.lessonWordIds)
        assertEquals(plan.wordIds.size, plan.wordIds.distinct().size)
    }

    @Test
    fun `missing persisted session levels default to Steady`() {
        assertEquals(SessionLevel.Steady, StudyProgress().sessionLevel)
        assertEquals(SessionLevel.Steady, SessionLevel.fromPersistedName(null))
        assertEquals(SessionLevel.Steady, SessionLevel.fromPersistedName("legacy"))
    }

    @Test
    fun `free practice remains bounded and rotates after completed practice events`() {
        val first = genericPlan(
            StudyProgress(spacedRepetitionEnabled = false),
            genericWords,
        )
        val progressed = StudyProgress(
            spacedRepetitionEnabled = false,
            reviewEvents = first.wordIds.mapIndexed { index, id ->
                ReviewEvent(
                    timestamp = now.plusSeconds(index.toLong()),
                    wordId = id,
                    correct = true,
                    wasNew = false,
                    source = ReviewSource.Study,
                )
            },
        )
        val second = genericPlan(progressed, genericWords)

        assertEquals(5, first.wordIds.size)
        assertEquals(5, second.wordIds.size)
        assertFalse(first.wordIds == second.wordIds)
        assertTrue(first.freePractice)
    }

    private fun genericPlan(progress: StudyProgress, words: List<QuranWord>): StudySessionPlan {
        val selection = StudyPlanSelection(null, emptyList(), words, words)
        return StudySessionPlanner.build(progress, selection, words, now, newStartIndex = 0)
    }

    private fun guidedProgress(stageIndex: Int, level: SessionLevel) = StudyProgress(
        selectedStudyScopes = setOf(StudyScope.Prayer),
        activeUnderstandPath = UnderstandPathId.AlFatihahSevenDays,
        activeUnderstandPathStage = stageIndex,
        sessionLevel = level,
    )

    private fun guidedSelection(stageIndex: Int, level: SessionLevel): StudyPlanSelection =
        StudyPlan.calculate(guidedProgress(stageIndex, level), corpus, emptyList())

    private fun stageIds(focus: UnderstandPathState, stageIndex: Int): List<String> =
        UnderstandPathCatalog.stageWords(focus.definition.stages[stageIndex], focus.targetWords)
            .distinctBy(QuranWord::id)
            .map(QuranWord::id)

    private fun dueSchedule() = ReviewSchedule(
        dueAt = now.minusSeconds(60),
        lastReviewedAt = now.minusSeconds(86_400),
    )

    private fun testWord(id: String) = QuranWord(
        id = id,
        arabic = id,
        lemma = id,
        transliteration = id,
        meaning = id,
        root = id,
        grammar = "noun",
        category = "test",
        reference = "Test 1:1",
        verseArabic = id,
        verseMeaning = id,
        insight = "",
    )
}
