package com.kalima.quran.ui

import com.kalima.quran.data.StudyProgress
import java.time.Instant

internal enum class QuranWordLearningState {
    Recognized,
    Reviewing,
    Due,
    Unknown,
    Unindexed,
}

internal enum class QuranReaderStudyAction {
    Learn,
    Review,
    PracticeAgain,
}

internal fun classifyQuranReaderWord(
    indexedWordId: String?,
    progress: StudyProgress,
    now: Instant = Instant.now(),
): QuranWordLearningState = when {
    indexedWordId == null -> QuranWordLearningState.Unindexed
    indexedWordId in progress.learnedIds || indexedWordId in progress.alreadyKnownIds ->
        QuranWordLearningState.Recognized
    indexedWordId in progress.reviewingIds &&
        progress.spacedRepetitionEnabled &&
        progress.reviewSchedules[indexedWordId]?.isDue(now) == true ->
        QuranWordLearningState.Due
    indexedWordId in progress.reviewingIds -> QuranWordLearningState.Reviewing
    else -> QuranWordLearningState.Unknown
}

internal fun displayedQuranWordLearningState(
    overlayEnabled: Boolean,
    state: QuranWordLearningState,
): QuranWordLearningState? = state.takeIf { overlayEnabled }

internal fun quranReaderStudyActionFor(
    state: QuranWordLearningState,
): QuranReaderStudyAction? = when (state) {
    QuranWordLearningState.Recognized -> QuranReaderStudyAction.PracticeAgain
    QuranWordLearningState.Reviewing,
    QuranWordLearningState.Due,
    -> QuranReaderStudyAction.Review
    QuranWordLearningState.Unknown -> QuranReaderStudyAction.Learn
    QuranWordLearningState.Unindexed -> null
}

internal fun launchQuranReaderWordStudy(
    indexedWordId: String?,
    state: QuranWordLearningState,
    onStudyWord: (String) -> Unit,
): QuranReaderStudyAction? {
    val action = quranReaderStudyActionFor(state)
    if (indexedWordId != null && action != null) onStudyWord(indexedWordId)
    return action
}
