package com.kalima.quran.data

import java.time.Instant

data class StudySessionPlan(
    val level: SessionLevel,
    val wordIds: List<String>,
    val reviewWordIds: List<String>,
    val lessonWordIds: List<String>,
    val requestsContextCheckpoint: Boolean,
    val preferredContextWordIds: List<String> = emptyList(),
    val guidedStageIndex: Int? = null,
    val guidedStageCount: Int? = null,
    val guidedAyahNumber: Int? = null,
    val currentGuidedLessonWaiting: Boolean = false,
    val currentGuidedLessonCompletedToday: Boolean = false,
    val freePractice: Boolean = false,
) {
    init {
        require(wordIds.distinct().size == wordIds.size) { "Session word IDs must be unique." }
        require(reviewWordIds.all(wordIds::contains))
        require(lessonWordIds.all(wordIds::contains))
    }

    val isEmpty: Boolean get() = wordIds.isEmpty()
}

/** Builds the complete, immutable foreground-session snapshot selected when Start is pressed. */
object StudySessionPlanner {
    private const val FREE_PRACTICE_LIMIT = 5

    fun build(
        progress: StudyProgress,
        selection: StudyPlanSelection,
        availableWords: List<QuranWord>,
        now: Instant,
        newStartIndex: Int,
    ): StudySessionPlan {
        val uniqueAvailable = availableWords.distinctBy(QuranWord::id)
        val focus = selection.focus
        return when {
            focus != null -> guidedPlan(progress, focus, uniqueAvailable, now)
            !progress.spacedRepetitionEnabled -> freePracticePlan(
                progress.sessionLevel,
                uniqueAvailable,
                newStartIndex + progress.reviewEvents.size,
            )
            else -> standardPlan(progress, uniqueAvailable, now, newStartIndex)
        }
    }

    private fun standardPlan(
        progress: StudyProgress,
        words: List<QuranWord>,
        now: Instant,
        newStartIndex: Int,
    ): StudySessionPlan {
        val due = ReviewQueue.dueWords(words, progress.reviewSchedules, now)
            .distinctBy(QuranWord::id)
        val dueIds = due.map(QuranWord::id)
        val newWords = ReviewQueue.rotated(
            ReviewQueue.newWords(words, progress.reviewSchedules).distinctBy(QuranWord::id),
            newStartIndex,
        ).filterNot { it.id in dueIds }
            .take(progress.sessionLevel.newWordLimit)
        val newIds = newWords.map(QuranWord::id)
        return StudySessionPlan(
            level = progress.sessionLevel,
            wordIds = dueIds + newIds,
            reviewWordIds = dueIds,
            lessonWordIds = newIds,
            requestsContextCheckpoint = progress.sessionLevel.requestsContextCheckpoint,
        )
    }

    private fun guidedPlan(
        progress: StudyProgress,
        focus: UnderstandPathState,
        availableWords: List<QuranWord>,
        now: Instant,
    ): StudySessionPlan {
        val stageIndex = focus.currentStageIndex
        val stage = focus.definition.stages[stageIndex]
        val allowedIds = availableWords.mapTo(mutableSetOf(), QuranWord::id)
        val currentStageWords = UnderstandPathCatalog.stageWords(stage, focus.targetWords)
            .filter { it.id in allowedIds }
            .distinctBy(QuranWord::id)
        val currentStageIds = currentStageWords.map(QuranWord::id)
        val earlierIds = focus.definition.stages
            .take(stageIndex)
            .flatMap { earlier -> UnderstandPathCatalog.stageWords(earlier, focus.targetWords) }
            .mapTo(linkedSetOf(), QuranWord::id)
            .apply { removeAll(currentStageIds.toSet()) }
        val earlierWords = focus.targetWords.filter { it.id in earlierIds && it.id in allowedIds }
        val dueIds = if (progress.spacedRepetitionEnabled) {
            ReviewQueue.dueWords(earlierWords, progress.reviewSchedules, now)
                .distinctBy(QuranWord::id)
                .map(QuranWord::id)
        } else {
            emptyList()
        }
        val includesLesson = progress.sessionLevel != SessionLevel.Quick
        val lessonIds = if (includesLesson) {
            currentStageIds.filterNot(progress.todayAnsweredIds::contains)
        } else {
            emptyList()
        }
        return StudySessionPlan(
            level = progress.sessionLevel,
            wordIds = (dueIds + lessonIds).distinct(),
            reviewWordIds = dueIds,
            lessonWordIds = lessonIds,
            requestsContextCheckpoint = progress.sessionLevel == SessionLevel.Deep,
            preferredContextWordIds = if (progress.sessionLevel == SessionLevel.Deep) {
                currentStageIds
            } else {
                emptyList()
            },
            guidedStageIndex = stageIndex,
            guidedStageCount = focus.definition.stages.size,
            guidedAyahNumber = stage.ayahNumber,
            currentGuidedLessonWaiting = progress.sessionLevel == SessionLevel.Quick,
            currentGuidedLessonCompletedToday = includesLesson &&
                currentStageIds.isNotEmpty() &&
                currentStageIds.all(progress.todayAnsweredIds::contains),
        )
    }

    private fun freePracticePlan(
        level: SessionLevel,
        words: List<QuranWord>,
        startIndex: Int,
    ): StudySessionPlan {
        val ids = ReviewQueue.rotated(words, startIndex)
            .distinctBy(QuranWord::id)
            .take(FREE_PRACTICE_LIMIT)
            .map(QuranWord::id)
        return StudySessionPlan(
            level = level,
            wordIds = ids,
            reviewWordIds = ids,
            lessonWordIds = emptyList(),
            requestsContextCheckpoint = level.requestsContextCheckpoint,
            freePractice = true,
        )
    }
}

fun isStudySessionComplete(plannedWordIds: List<String>, completedWordIds: Collection<String>): Boolean =
    plannedWordIds.isNotEmpty() && plannedWordIds.all(completedWordIds::contains)
