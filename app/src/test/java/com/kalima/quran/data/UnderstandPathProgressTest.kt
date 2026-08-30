package com.kalima.quran.data

import com.kalima.quran.localization.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnderstandPathProgressTest {
    private val corpus by lazy {
        val asset = sequenceOf(
            java.io.File("src/main/assets/${VocabularyAssetLoader.ASSET_NAME}.gz"),
            java.io.File("app/src/main/assets/${VocabularyAssetLoader.ASSET_NAME}.gz"),
        ).first(java.io.File::isFile)
        VocabularyAssetLoader.load(asset.inputStream(), AppLanguage.English)
    }

    @Test
    fun `fatihah path contains seven populated ayah stages`() {
        val definition = UnderstandPathCatalog.definition(UnderstandPathId.AlFatihahSevenDays)
        val target = UnderstandPathCatalog.targetWords(definition, corpus)

        assertEquals(7, definition.stages.size)
        assertEquals(setOf(1), target.mapNotNull(QuranWord::surahNumber).toSet())
        assertEquals(26, target.size)
        assertEquals(29, target.sumOf(QuranWord::frequency))
        assertTrue(definition.stages.all { stage ->
            UnderstandPathCatalog.stageWords(stage, target).isNotEmpty()
        })
    }

    @Test
    fun `last ten path excludes the broader short surah collection`() {
        val definition = UnderstandPathCatalog.definition(UnderstandPathId.LastTenSurahs)
        val target = UnderstandPathCatalog.targetWords(definition, corpus)

        assertEquals((105..114).toSet(), target.mapNotNull(QuranWord::surahNumber).toSet())
        assertEquals(172, target.size)
        assertEquals(201, target.sumOf(QuranWord::frequency))
    }

    @Test
    fun `known vocabulary needs separate recall evidence`() {
        val definition = UnderstandPathCatalog.definition(UnderstandPathId.AlFatihahSevenDays)
        val target = UnderstandPathCatalog.targetWords(definition, corpus)
        val knownOnly = UnderstandPathProgress.calculate(
            progress = StudyProgress(alreadyKnownIds = target.mapTo(mutableSetOf(), QuranWord::id)),
            pathId = definition.id,
            words = corpus,
        )

        assertEquals(100, knownOnly.metric.coveragePercent)
        assertEquals(0, knownOnly.metric.recallPercent)
        assertFalse(knownOnly.meetsCompletionGoal)

        val recalled = UnderstandPathProgress.calculate(
            progress = StudyProgress(
                alreadyKnownIds = target.mapTo(mutableSetOf(), QuranWord::id),
                quizCorrectDays = target.associate { word ->
                    word.id to setOf("2026-08-27", "2026-08-28", "2026-08-29")
                },
            ),
            pathId = definition.id,
            words = corpus,
        )

        assertEquals(100, recalled.metric.coveragePercent)
        assertEquals(100, recalled.metric.recallPercent)
        assertTrue(recalled.meetsCompletionGoal)
    }

    @Test
    fun `only introduced stages enter the active study queue`() {
        val definition = UnderstandPathCatalog.definition(UnderstandPathId.AlFatihahSevenDays)
        val target = UnderstandPathCatalog.targetWords(definition, corpus)
        val firstStage = UnderstandPathCatalog.stageWords(definition.stages.first(), target)
        val state = UnderstandPathProgress.calculate(
            progress = StudyProgress(
                reviewingIds = firstStage.mapTo(mutableSetOf(), QuranWord::id),
            ),
            pathId = definition.id,
            words = corpus,
        )

        assertEquals(1, state.currentStageIndex)
        assertTrue(firstStage.all { it in state.unlockedWords })
        assertTrue(
            UnderstandPathCatalog.stageWords(definition.stages[1], target)
                .all { it in state.unlockedWords },
        )
        assertEquals(
            (firstStage + UnderstandPathCatalog.stageWords(definition.stages[1], target))
                .mapTo(mutableSetOf(), QuranWord::id),
            state.unlockedWords.mapTo(mutableSetOf(), QuranWord::id),
        )
    }
}
