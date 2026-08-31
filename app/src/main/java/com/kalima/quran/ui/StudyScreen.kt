package com.kalima.quran.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kalima.quran.R
import com.kalima.quran.audio.ArabicPronouncer
import com.kalima.quran.data.SessionLevel
import com.kalima.quran.data.StudyPlan
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.StudySessionPlanner
import com.kalima.quran.data.WordRepository
import com.kalima.quran.data.WordStatus
import com.kalima.quran.data.hasAlphabetFoundationLesson
import com.kalima.quran.data.isStudySessionComplete
import com.kalima.quran.data.limitNewWords
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MISSION_REFRESH_MILLIS = 60_000L

@Composable
fun StudyScreen(
    progress: StudyProgress,
    onIntroduce: (String) -> Unit,
    onAnswer: (String, Boolean) -> Unit,
    onCheckpointAnswer: (String, Boolean) -> Unit,
    onCurrentWordChange: (String) -> Unit,
    onEnableLockScreen: () -> Unit,
    onOpenExcludedWords: () -> Unit,
    onToggleCustomList: (String) -> Unit,
    onToggleAlreadyKnown: (String) -> Unit,
    onOpenQuiz: () -> Unit,
    onSessionLevelChange: (SessionLevel) -> Unit,
    onAdvanceUnderstandPath: () -> Unit,
    onOpenSettings: () -> Unit,
    pronouncer: ArabicPronouncer,
    launchTarget: StudyLaunchTarget? = null,
    onLaunchTargetHandled: (Long) -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showLockScreenPrompt by rememberSaveable(progress.lockScreenEnabled) {
        mutableStateOf(!progress.lockScreenEnabled)
    }
    if (!progress.lockScreenEnabled && showLockScreenPrompt) {
        LockScreenLearningPopup(
            onDismiss = { showLockScreenPrompt = false },
            onEnableLockScreen = {
                showLockScreenPrompt = false
                onEnableLockScreen()
            },
        )
    }
    val excludedMessage = stringResource(R.string.word_excluded_message)
    val undoLabel = stringResource(R.string.undo)
    val studyPlan = remember(progress) { StudyPlan.calculate(progress, WordRepository.words) }
    val activePathState = studyPlan.focus
    val scopeKey = buildString {
        append(progress.studyScopes.map(StudyScope::name).sorted().joinToString(","))
        activePathState?.let { state ->
            append(":focus:")
            append(state.definition.id.name)
            append(':')
            append(state.currentStageIndex)
        }
    }
    val selectionKey = progress.selectedSurahs.sorted().joinToString(",")
    val selectedWords = studyPlan.combinedWords
    val availableWords = remember(
        selectedWords,
        progress.maximumWords,
        progress.learnedIds,
        progress.reviewingIds,
        progress.alreadyKnownIds,
    ) {
        progress.limitNewWords(selectedWords)
    }
    var routedStudyWordId by rememberSaveable(scopeKey, selectionKey) {
        mutableStateOf<String?>(null)
    }
    val requestedWordId = launchTarget?.wordId ?: routedStudyWordId
    val requestedWord = requestedWordId?.let(WordRepository::wordById)
    if (availableWords.isEmpty() && requestedWord == null) {
        Box(Modifier.fillMaxSize()) {
            when {
                selectedWords.isNotEmpty() && selectedWords.all { it.id in progress.alreadyKnownIds } ->
                    AllWordsAlreadyKnownState(
                        modifier = Modifier.fillMaxSize(),
                        onOpenExcludedWords = onOpenExcludedWords,
                    )
                progress.studyScopes == setOf(StudyScope.Custom) -> EmptyCollectionState(Modifier.fillMaxSize())
                else -> LearningLimitEmptyState(Modifier.fillMaxSize())
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
            )
            TabSettingsButton(
                onClick = onOpenSettings,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 8.dp, top = 2.dp),
            )
        }
        return
    }
    var missionNow by remember { mutableStateOf(Instant.now()) }
    val missionDate = missionNow.atZone(ZoneId.systemDefault()).toLocalDate()
    val previewPlan = remember(
        progress,
        studyPlan,
        availableWords,
        missionNow,
    ) {
        val dailyStart = LocalDate.now().toEpochDay().toInt()
        StudySessionPlanner.build(
            progress = progress,
            selection = studyPlan,
            availableWords = availableWords,
            now = missionNow,
            newStartIndex = dailyStart,
        )
    }
    var showMission by rememberSaveable(scopeKey, selectionKey, missionDate) { mutableStateOf(true) }
    var showCompletion by rememberSaveable(scopeKey, selectionKey, missionDate) { mutableStateOf(false) }
    var checkpointComplete by rememberSaveable(scopeKey, selectionKey) { mutableStateOf(false) }
    var plannedWordIds by rememberSaveable(scopeKey, selectionKey, missionDate) {
        mutableStateOf(arrayListOf<String>())
    }
    var plannedReviewWordIds by rememberSaveable(scopeKey, selectionKey) {
        mutableStateOf(arrayListOf<String>())
    }
    var plannedLessonWordIds by rememberSaveable(scopeKey, selectionKey) {
        mutableStateOf(arrayListOf<String>())
    }
    var completedSessionWordIds by rememberSaveable(scopeKey, selectionKey, missionDate) {
        mutableStateOf(arrayListOf<String>())
    }
    var sessionCheckpointRequested by rememberSaveable(scopeKey, selectionKey) {
        mutableStateOf(false)
    }
    var sessionPreferredContextWordIds by rememberSaveable(scopeKey, selectionKey) {
        mutableStateOf(arrayListOf<String>())
    }
    var sessionLevelName by rememberSaveable(scopeKey, selectionKey) {
        mutableStateOf(SessionLevel.Steady.name)
    }
    var missionSessionDate by rememberSaveable(scopeKey, selectionKey) {
        mutableStateOf(missionDate.toString())
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                missionNow = Instant.now()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            missionNow = Instant.now()
            delay(MISSION_REFRESH_MILLIS)
        }
    }
    val missionActivity = remember(progress.reviewEvents, missionNow) {
        buildMissionActivity(progress, missionNow)
    }
    val pathQuizEligibleIds = pathQuizEligibleWordIds(progress)
    val pathQuizReady = activePathState?.unlockedWords?.any { word ->
        word.id in pathQuizEligibleIds
    } ?: true
    LaunchedEffect(missionDate) {
        if (missionSessionDate != missionDate.toString()) {
            missionSessionDate = missionDate.toString()
            showMission = true
            showCompletion = false
            checkpointComplete = false
            plannedWordIds = arrayListOf()
            plannedReviewWordIds = arrayListOf()
            plannedLessonWordIds = arrayListOf()
            completedSessionWordIds = arrayListOf()
            sessionCheckpointRequested = false
            sessionPreferredContextWordIds = arrayListOf()
        }
    }
    if (
        shouldShowDailyMission(
            showMission = showMission,
            hasActiveUnderstandPath = activePathState != null,
            hasQueuedWords = previewPlan.wordIds.isNotEmpty(),
            showCompletion = showCompletion,
            hasLaunchTarget = launchTarget != null,
        )
    ) {
        DailyMissionScreen(
            plan = previewPlan,
            progress = progress,
            streakDays = progress.streakDays,
            activity = missionActivity,
            pathQuizReady = pathQuizReady,
            onOpenQuiz = onOpenQuiz,
            onSessionLevelChange = onSessionLevelChange,
            onOpenSettings = onOpenSettings,
            onStart = {
                routedStudyWordId = null
                plannedWordIds = ArrayList(previewPlan.wordIds)
                plannedReviewWordIds = ArrayList(previewPlan.reviewWordIds)
                plannedLessonWordIds = ArrayList(previewPlan.lessonWordIds)
                completedSessionWordIds = arrayListOf()
                checkpointComplete = false
                sessionCheckpointRequested = previewPlan.requestsContextCheckpoint
                sessionPreferredContextWordIds = ArrayList(previewPlan.preferredContextWordIds)
                sessionLevelName = previewPlan.level.name
                showMission = false
            },
        )
        return
    }
    if (showCompletion && completedSessionWordIds.isNotEmpty()) {
        val reviewedWords = completedSessionWordIds.mapNotNull(WordRepository::wordById)
        val recognizedWordIds = progress.learnedIds + progress.reviewingIds +
            progress.alreadyKnownIds + completedSessionWordIds
        val checkpoint = remember(
            reviewedWords,
            availableWords,
            missionDate,
            sessionPreferredContextWordIds,
        ) {
            if (sessionCheckpointRequested) buildContextCheckpointQuestion(
                practicedWords = reviewedWords,
                activeCollection = availableWords,
                seed = missionDate.toEpochDay().toInt(),
                preferredWordIds = sessionPreferredContextWordIds.toSet(),
            ) else null
        }
        if (!checkpointComplete && checkpoint != null) {
            ContextCheckpointScreen(
                question = checkpoint,
                pronouncer = pronouncer,
                onAnswer = onCheckpointAnswer,
                onContinue = { checkpointComplete = true },
                onOpenSettings = onOpenSettings,
            )
            return
        }
        val payoff = remember(reviewedWords, recognizedWordIds) {
            buildStudyCompletionPayoff(
                reviewedWords = reviewedWords,
                recognizedWordIds = recognizedWordIds,
                tokensFor = WordRepository::verseTokens,
            )
        }
        StudyCompletionScreen(
            payoff = payoff,
            recognizedWordIds = recognizedWordIds,
            pronouncer = pronouncer,
            onOpenSettings = onOpenSettings,
            onAdvancePath = if (
                SessionLevel.fromPersistedName(sessionLevelName) != SessionLevel.Quick &&
                plannedLessonWordIds.isNotEmpty() &&
                activePathState?.currentStageReadyToAdvance == true &&
                activePathState.currentStageIndex < activePathState.definition.stages.lastIndex
            ) {
                {
                    onAdvanceUnderstandPath()
                    routedStudyWordId = null
                    showCompletion = false
                    showMission = true
                    plannedWordIds = arrayListOf()
                    completedSessionWordIds = arrayListOf()
                }
            } else {
                null
            },
            onFinish = {
                routedStudyWordId = null
                showCompletion = false
                showMission = true
                checkpointComplete = false
                plannedWordIds = arrayListOf()
                plannedReviewWordIds = arrayListOf()
                plannedLessonWordIds = arrayListOf()
                completedSessionWordIds = arrayListOf()
                sessionCheckpointRequested = false
                sessionPreferredContextWordIds = arrayListOf()
            },
        )
        return
    }
    val words = remember(plannedWordIds, requestedWord?.id) {
        val planned = plannedWordIds.mapNotNull(WordRepository::wordById)
        if (planned.isNotEmpty()) planned else listOfNotNull(requestedWord)
    }
    if (words.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            AllCaughtUpState(Modifier.fillMaxSize())
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
            )
            TabSettingsButton(
                onClick = onOpenSettings,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 8.dp, top = 2.dp),
            )
        }
        return
    }
    val session = remember(words, requestedWord?.id) {
        buildStudySession(
            words = words,
            defaultWord = words.first(),
            requestedWord = requestedWord,
        )
    }
    var currentWordId by rememberSaveable(
        scopeKey,
        selectionKey,
        plannedWordIds.joinToString(","),
    ) { mutableStateOf(session.first().id) }
    val displayedWordId = displayedStudyWordId(currentWordId, launchTarget)
    val word = WordRepository.wordById(displayedWordId) ?: session.first()
    val isNewPresentation = rememberSaveable(word.id) {
        progress.statusFor(word.id) == WordStatus.New
    }
    var meaningRevealed by rememberSaveable(word.id) {
        mutableStateOf(shouldRevealMeaningInitially(progress.statusFor(word.id)))
    }
    val moveToNextWord = {
        routedStudyWordId = null
        val currentIndex = session.indexOfFirst { it.id == word.id }
        val nextWord = if (currentIndex < 0) {
            session.first()
        } else {
            session.drop(currentIndex + 1)
                .firstOrNull { it.id !in completedSessionWordIds }
                ?: session.first()
        }
        currentWordId = nextWord.id
        onCurrentWordChange(nextWord.id)
    }
    val finishCurrentWord = {
        val completed = (completedSessionWordIds + word.id).distinct()
        completedSessionWordIds = ArrayList(completed)
        val requiredIds = if (plannedWordIds.isNotEmpty()) plannedWordIds else listOf(word.id)
        if (isStudySessionComplete(requiredIds, completed)) {
            showCompletion = true
        } else {
            moveToNextWord()
        }
    }
    val requiredWordIds = plannedWordIds.ifEmpty { listOf(word.id) }
    val isLastSessionWord = isFinalStudySessionWord(
        requiredWordIds = requiredWordIds,
        completedWordIds = completedSessionWordIds,
        currentWordId = word.id,
    )
    val nextActionOpensCheckpoint = isNewPresentation && sessionCheckpointRequested &&
        isLastSessionWord
    val scrollState = rememberScrollState()
    var scrollPositionWordId by rememberSaveable(scopeKey, selectionKey) {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(launchTarget?.requestId) {
        val target = launchTarget ?: return@LaunchedEffect
        showMission = false
        if (WordRepository.containsWord(target.wordId)) {
            routedStudyWordId = target.wordId
            currentWordId = target.wordId
            onCurrentWordChange(target.wordId)
        }
        onLaunchTargetHandled(target.requestId)
    }

    LaunchedEffect(word.id) {
        onCurrentWordChange(word.id)
        if (isNewPresentation) {
            onIntroduce(word.id)
        }
        if (scrollPositionWordId != word.id) {
            scrollState.scrollTo(0)
            scrollPositionWordId = word.id
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            StudyHeader(
                progress = progress,
                completedWords = completedSessionWordIds.size,
                sessionWords = plannedWordIds.size.coerceAtLeast(1),
                reviewCount = plannedReviewWordIds.size,
                onOpenSettings = onOpenSettings,
            )
            Spacer(Modifier.height(10.dp))
            WordCard(
                word = word,
                progress = progress,
                displayedStatus = studyPresentationStatus(
                    status = progress.statusFor(word.id),
                    isNewPresentation = isNewPresentation,
                ),
                pronouncer = pronouncer,
                meaningRevealed = meaningRevealed,
                onToggleCustomList = onToggleCustomList,
                onToggleAlreadyKnown = {
                    val wordId = word.id
                    val markingAsKnown = wordId !in progress.alreadyKnownIds
                    onToggleAlreadyKnown(wordId)
                    if (markingAsKnown) {
                        finishCurrentWord()
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = excludedMessage,
                                actionLabel = undoLabel,
                                withDismissAction = true,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                onToggleAlreadyKnown(wordId)
                            }
                        }
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            StudyActionBar(
                meaningRevealed = meaningRevealed,
                isNewPresentation = isNewPresentation,
                nextActionOpensCheckpoint = nextActionOpensCheckpoint,
                nextActionCompletesSession = isLastSessionWord,
                spacedRepetitionEnabled = progress.spacedRepetitionEnabled,
                goodTiming = goodReviewTiming(
                    spacedRepetitionEnabled = progress.spacedRepetitionEnabled,
                    schedule = progress.scheduleFor(word.id),
                ),
                onRevealMeaning = { meaningRevealed = true },
                onNextWord = {
                    finishCurrentWord()
                },
                onAgain = {
                    onAnswer(word.id, false)
                    finishCurrentWord()
                },
                onRemembered = {
                    onAnswer(word.id, true)
                    finishCurrentWord()
                },
            )
            Spacer(Modifier.height(16.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 20.dp),
        )
    }
}

@Composable
internal fun LockScreenLearningPopup(
    onDismiss: () -> Unit,
    onEnableLockScreen: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.study_lock_screen_title),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                stringResource(R.string.study_lock_screen_description),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(onClick = onEnableLockScreen) {
                Text(stringResource(R.string.study_lock_screen_enable))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.study_lock_screen_not_now))
            }
        },
    )
}
