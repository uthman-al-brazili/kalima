package com.kalima.quran.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kalima.quran.R
import com.kalima.quran.audio.ArabicPronouncer
import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.ArabicFoundations
import com.kalima.quran.data.ReviewQueue
import com.kalima.quran.data.ReviewSchedule
import com.kalima.quran.data.SpacedRepetition
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.StudyScope
import com.kalima.quran.data.WordRepository
import com.kalima.quran.data.WordStatus
import com.kalima.quran.data.limitNewWords
import com.kalima.quran.data.hasNumberFoundationLesson
import com.kalima.quran.data.hasAlphabetFoundationLesson
import com.kalima.quran.data.needsAlphabetFoundation
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
    onShowCompleteAyahChange: (Boolean) -> Unit,
    onOpenFoundations: () -> Unit,
    onOpenQuiz: () -> Unit,
    pronouncer: ArabicPronouncer,
    launchTarget: StudyLaunchTarget? = null,
    onLaunchTargetHandled: (Long) -> Unit = {},
) {
    if (progress.needsAlphabetFoundation) {
        WordStudyLockedScreen(onOpenFoundations = onOpenFoundations)
        return
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val excludedMessage = stringResource(R.string.word_excluded_message)
    val undoLabel = stringResource(R.string.undo)
    val scopeKey = progress.studyScopes.map(StudyScope::name).sorted().joinToString(",")
    val selectionKey = progress.selectedSurahs.sorted().joinToString(",")
    val selectedWords = remember(
        scopeKey,
        selectionKey,
        progress.customStudyIds,
    ) {
        WordRepository.wordsFor(
            progress.studyScopes,
            progress.selectedSurahs,
            progress.customStudyIds,
        )
    }
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
        }
        return
    }
    var activeIntroductionId by rememberSaveable(scopeKey, selectionKey) {
        mutableStateOf<String?>(null)
    }
    val queueSourceWords = remember(availableWords, requestedWord?.id) {
        studyQueueSourceWords(availableWords, requestedWord)
    }
    val queuedWords = remember(
        queueSourceWords,
        progress.reviewSchedules,
        progress.spacedRepetitionEnabled,
    ) {
        val dailyStart = LocalDate.now().toEpochDay().toInt()
        if (progress.spacedRepetitionEnabled) {
            ReviewQueue.ordered(
                words = queueSourceWords,
                schedules = progress.reviewSchedules,
                now = Instant.now(),
                newStartIndex = dailyStart,
            )
        } else {
            ReviewQueue.rotated(queueSourceWords, dailyStart)
        }
    }
    val words = remember(queuedWords, queueSourceWords, activeIntroductionId) {
        studyWordsForPresentation(queuedWords, queueSourceWords, activeIntroductionId)
    }
    var missionNow by remember { mutableStateOf(Instant.now()) }
    val missionDate = missionNow.atZone(ZoneId.systemDefault()).toLocalDate()
    var showMission by rememberSaveable(scopeKey, selectionKey, missionDate) { mutableStateOf(true) }
    var showCompletion by rememberSaveable(scopeKey, selectionKey, missionDate) { mutableStateOf(false) }
    var checkpointComplete by rememberSaveable(scopeKey, selectionKey) { mutableStateOf(false) }
    var automaticCheckpointEligible by rememberSaveable(scopeKey, selectionKey) {
        mutableStateOf(false)
    }
    var sessionWordIds by rememberSaveable(scopeKey, selectionKey, missionDate) {
        mutableStateOf(arrayListOf<String>())
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
    val mission = remember(progress, availableWords, missionNow) {
        buildDailyMissionState(progress, availableWords, now = missionNow)
    }
    LaunchedEffect(missionDate) {
        if (missionSessionDate != missionDate.toString()) {
            missionSessionDate = missionDate.toString()
            showMission = true
            showCompletion = false
            checkpointComplete = false
            automaticCheckpointEligible = false
            sessionWordIds = arrayListOf()
        }
    }
    if (showMission && launchTarget == null) {
        DailyMissionScreen(
            mission = mission,
            streakDays = progress.streakDays,
            canStart = words.isNotEmpty(),
            lockScreenEnabled = progress.lockScreenEnabled,
            onEnableLockScreen = onEnableLockScreen,
            onOpenQuiz = onOpenQuiz,
            onStart = {
                sessionWordIds = arrayListOf()
                checkpointComplete = false
                automaticCheckpointEligible = !mission.goalComplete
                showMission = false
            },
        )
        return
    }
    if (showCompletion && sessionWordIds.isNotEmpty()) {
        val reviewedWords = sessionWordIds.mapNotNull(WordRepository::wordById)
        val recognizedWordIds = progress.learnedIds + progress.reviewingIds +
            progress.alreadyKnownIds + sessionWordIds
        val checkpoint = remember(reviewedWords, availableWords, missionDate) {
            buildContextCheckpointQuestion(
                practicedWords = reviewedWords,
                activeCollection = availableWords,
                seed = missionDate.toEpochDay().toInt(),
            )
        }
        if (!checkpointComplete && checkpoint != null) {
            ContextCheckpointScreen(
                question = checkpoint,
                pronouncer = pronouncer,
                onAnswer = onCheckpointAnswer,
                onContinue = { checkpointComplete = true },
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
            onFinish = {
                showCompletion = false
                showMission = true
                checkpointComplete = false
                automaticCheckpointEligible = false
                sessionWordIds = arrayListOf()
            },
        )
        return
    }
    if (words.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            AllCaughtUpState(Modifier.fillMaxSize())
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
            )
        }
        return
    }
    val session = remember(words, requestedWord?.id) {
        val resumedWord = progress.currentStudyWordId
            ?.let { currentId -> words.firstOrNull { it.id == currentId } }
        buildStudySession(
            words = words,
            defaultWord = resumedWord ?: words.first(),
            requestedWord = requestedWord,
        )
    }
    var currentWordId by rememberSaveable(
        scopeKey,
        selectionKey,
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
        val nextWord = if (currentIndex < 0) session.first() else session[(currentIndex + 1) % session.size]
        currentWordId = nextWord.id
        onCurrentWordChange(nextWord.id)
    }
    val finishCurrentWord: (Boolean) -> Unit = { wordCompletionAlreadyRecorded ->
        sessionWordIds = ArrayList((sessionWordIds + word.id).distinct())
        if (
            shouldOpenContextCheckpoint(
                mission = mission,
                wordId = word.id,
                dailyGoalWasIncompleteAtMissionStart = automaticCheckpointEligible,
                wordCompletionAlreadyRecorded = wordCompletionAlreadyRecorded,
            )
        ) {
            automaticCheckpointEligible = false
            showCompletion = true
        } else {
            moveToNextWord()
        }
    }
    val nextActionOpensCheckpoint = isNewPresentation && shouldOpenContextCheckpoint(
        mission = mission,
        wordId = word.id,
        dailyGoalWasIncompleteAtMissionStart = automaticCheckpointEligible,
        wordCompletionAlreadyRecorded = true,
    )
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
            activeIntroductionId = word.id
            sessionWordIds = ArrayList((sessionWordIds + word.id).distinct())
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
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            StudyHeader(
                progress = progress,
                dueCount = progress.dueReviewCount(availableWords.mapTo(mutableSetOf()) { it.id }),
                onViewTodayResults = if (
                    mission.goalComplete && sessionWordIds.isNotEmpty() &&
                    !nextActionOpensCheckpoint
                ) {
                    {
                        showMission = true
                        showCompletion = false
                        checkpointComplete = false
                        automaticCheckpointEligible = false
                        sessionWordIds = arrayListOf()
                    }
                } else {
                    null
                },
            )
            Spacer(Modifier.height(16.dp))
            if (!progress.lockScreenEnabled) {
                LockScreenLearningCard(onEnableLockScreen = onEnableLockScreen)
                Spacer(Modifier.height(20.dp))
            }
            WordCard(
                word = word,
                progress = progress,
                displayedStatus = studyPresentationStatus(
                    status = progress.statusFor(word.id),
                    isNewPresentation = isNewPresentation,
                ),
                pronouncer = pronouncer,
                meaningRevealed = meaningRevealed,
                onRevealChange = { meaningRevealed = it },
                onToggleCustomList = onToggleCustomList,
                onToggleAlreadyKnown = {
                    val wordId = word.id
                    val markingAsKnown = wordId !in progress.alreadyKnownIds
                    onToggleAlreadyKnown(wordId)
                    if (markingAsKnown) {
                        moveToNextWord()
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
                showCompleteAyah = progress.showCompleteAyah,
                onShowCompleteAyahChange = onShowCompleteAyahChange,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.context_meaning_note),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(148.dp))
        }

        StudyActionBar(
            meaningRevealed = meaningRevealed,
            isNewPresentation = isNewPresentation,
            nextActionOpensCheckpoint = nextActionOpensCheckpoint,
            spacedRepetitionEnabled = progress.spacedRepetitionEnabled,
            goodTiming = goodReviewTiming(
                spacedRepetitionEnabled = progress.spacedRepetitionEnabled,
                schedule = progress.scheduleFor(word.id),
            ),
            onRevealMeaning = { meaningRevealed = true },
            onNextWord = {
                activeIntroductionId = null
                finishCurrentWord(true)
            },
            onAgain = {
                onAnswer(word.id, false)
                finishCurrentWord(false)
            },
            onRemembered = {
                onAnswer(word.id, true)
                finishCurrentWord(false)
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 148.dp),
        )
    }
}

@Composable
internal fun LockScreenLearningCard(
    onEnableLockScreen: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.study_lock_screen_title),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.study_lock_screen_description),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onEnableLockScreen) {
                Text(stringResource(R.string.study_lock_screen_enable))
            }
        }
    }
}

@Composable
fun FoundationsScreen(
    progress: StudyProgress,
    onCompleteAlphabetLesson: () -> Unit,
    onStartAlphabetFoundation: () -> Unit,
    onSkipAlphabetFoundation: () -> Unit,
    onCompleteNumberLesson: () -> Unit,
    onStartNumberFoundation: () -> Unit,
    pronouncer: ArabicPronouncer,
) {
    if (progress.hasAlphabetFoundationLesson) {
        AlphabetFoundationScreen(
            progress = progress,
            onCompleteAlphabetLesson = onCompleteAlphabetLesson,
            onCompleteNumberLesson = onCompleteNumberLesson,
            onSkipAlphabetFoundation = onSkipAlphabetFoundation,
            pronouncer = pronouncer,
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(
            stringResource(R.string.foundations_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.foundations_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(22.dp))
        AlphabetAccessCard(
            progress = progress,
            onStartAlphabetFoundation = onStartAlphabetFoundation,
        )
        Spacer(Modifier.height(16.dp))
        if (progress.hasNumberFoundationLesson) {
            NumberFoundationCard(
                progress = progress,
                onCompleteNumberLesson = onCompleteNumberLesson,
                pronouncer = pronouncer,
            )
        } else {
            NumberAccessCard(onStartNumberFoundation = onStartNumberFoundation)
        }
        Spacer(Modifier.height(22.dp))
        AlphabetReferenceTable()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun WordStudyLockedScreen(onOpenFoundations: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.study_after_foundations_title),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.study_after_foundations_description),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onOpenFoundations) {
                Text(stringResource(R.string.open_foundations))
            }
        }
    }
}

@Composable
private fun AlphabetFoundationScreen(
    progress: StudyProgress,
    onCompleteAlphabetLesson: () -> Unit,
    onCompleteNumberLesson: () -> Unit,
    onSkipAlphabetFoundation: () -> Unit,
    pronouncer: ArabicPronouncer,
) {
    val lessonIndex = progress.completedAlphabetLessons
        .coerceIn(0, ArabicFoundations.alphabetLessons.lastIndex)
    val lesson = ArabicFoundations.alphabetLessons[lessonIndex]
    val fraction = lessonIndex.toFloat() / ArabicFoundations.alphabetLessonCount
    var recalling by rememberSaveable(lessonIndex) { mutableStateOf(false) }
    var symbolIndex by rememberSaveable(lessonIndex) { mutableStateOf(0) }
    var selectedOptionIndex by rememberSaveable(lessonIndex) { mutableStateOf<Int?>(null) }
    val practiceQuestions = remember(lesson) { lesson.practiceQuestions() }
    var showReference by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            stringResource(R.string.alphabet_course_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.alphabet_course_words_locked),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            TextButton(onClick = onSkipAlphabetFoundation) {
                Text(stringResource(R.string.skip_alphabet_for_now))
            }
        }
        TextButton(
            onClick = { showReference = !showReference },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (showReference) R.string.hide_alphabet_reference
                    else R.string.open_alphabet_reference,
                ),
            )
        }
        if (showReference) {
            AlphabetReferenceTable()
            Spacer(Modifier.height(14.dp))
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.weight(1f).height(7.dp),
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(
                    R.string.foundation_step_progress,
                    lessonIndex + 1,
                    ArabicFoundations.alphabetLessonCount,
                ),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(18.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    stringResource(
                        if (recalling) {
                            R.string.alphabet_recall_title
                        } else {
                            R.string.alphabet_letters_lesson
                        },
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(
                        if (recalling) {
                            R.string.alphabet_recall_instruction
                        } else {
                            R.string.alphabet_learn_instruction
                        },
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                if (!recalling) {
                    val symbol = lesson.symbols[symbolIndex]
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(22.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                stringResource(
                                    R.string.alphabet_symbol_progress,
                                    symbolIndex + 1,
                                    lesson.symbols.size,
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Spacer(Modifier.height(8.dp))
                            ArabicText(
                                symbol.arabic,
                                modifier = Modifier.fillMaxWidth(),
                                size = 88,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (symbol.spokenArabic != symbol.arabic) {
                                ArabicText(
                                    symbol.spokenArabic,
                                    modifier = Modifier.fillMaxWidth(),
                                    size = 28,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Text(
                                symbol.transliteration,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(10.dp))
                            FoundationPronunciationButton(
                                text = symbol.spokenArabic,
                                pronouncer = pronouncer,
                                labelRes = R.string.hear_letter,
                            )
                            Text(
                                stringResource(R.string.letter_audio_matches_name),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.alphabet_letters_note),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { symbolIndex -= 1 },
                            modifier = Modifier.weight(1f).height(52.dp),
                            enabled = symbolIndex > 0,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(stringResource(R.string.previous_symbol))
                        }
                        Button(
                            onClick = {
                                if (symbolIndex == lesson.symbols.lastIndex) {
                                    symbolIndex = 0
                                    selectedOptionIndex = null
                                    recalling = true
                                } else {
                                    symbolIndex += 1
                                }
                            },
                            modifier = Modifier.weight(1.35f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(
                                stringResource(
                                    if (symbolIndex == lesson.symbols.lastIndex) {
                                        R.string.start_alphabet_recall
                                    } else {
                                        R.string.next_symbol
                                    },
                                ),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                } else {
                    val question = practiceQuestions[symbolIndex]
                    val answeredCorrectly = selectedOptionIndex == question.correctOptionIndex
                    Text(
                        stringResource(
                            R.string.alphabet_symbol_progress,
                            symbolIndex + 1,
                            practiceQuestions.size,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.alphabet_recall_prompt),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(22.dp),
                    ) {
                        ArabicText(
                            question.symbol.arabic,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            size = 88,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    question.options.forEachIndexed { optionIndex, option ->
                        val selected = selectedOptionIndex == optionIndex
                        val correct = selected && optionIndex == question.correctOptionIndex
                        val containerColor = when {
                            correct -> MaterialTheme.colorScheme.primaryContainer
                            selected -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val contentColor = when {
                            correct -> MaterialTheme.colorScheme.onPrimaryContainer
                            selected -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        val borderColor = when {
                            correct -> MaterialTheme.colorScheme.primary
                            selected -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.outline
                        }
                        OutlinedButton(
                            onClick = {
                                if (!answeredCorrectly) selectedOptionIndex = optionIndex
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = containerColor,
                                contentColor = contentColor,
                            ),
                            border = BorderStroke(1.dp, borderColor),
                        ) {
                            Text(option, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (selectedOptionIndex != null) {
                        Text(
                            stringResource(
                                if (answeredCorrectly) {
                                    R.string.alphabet_answer_correct
                                } else {
                                    R.string.alphabet_try_again
                                },
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            color = if (answeredCorrectly) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    if (answeredCorrectly) {
                        Button(
                            onClick = {
                                if (symbolIndex == practiceQuestions.lastIndex) {
                                    onCompleteAlphabetLesson()
                                } else {
                                    symbolIndex += 1
                                    selectedOptionIndex = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(
                                stringResource(
                                    when {
                                        symbolIndex < practiceQuestions.lastIndex -> {
                                            R.string.next_alphabet_question
                                        }
                                        lessonIndex == ArabicFoundations.alphabetLessons.lastIndex -> {
                                            R.string.finish_alphabet_course
                                        }
                                        else -> R.string.complete_alphabet_step
                                    },
                                ),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            recalling = false
                            symbolIndex = 0
                            selectedOptionIndex = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.review_alphabet_symbols))
                    }
                }
            }
        }
        if (progress.hasNumberFoundationLesson) {
            Spacer(Modifier.height(16.dp))
            NumberFoundationCard(
                progress = progress,
                onCompleteNumberLesson = onCompleteNumberLesson,
                pronouncer = pronouncer,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AlphabetReferenceTable() {
    var requestedPage by rememberSaveable { mutableStateOf(0) }
    val pages = remember {
        ArabicFoundations.alphabetReference.chunked(ArabicFoundations.alphabetReferencePageSize)
    }
    val pageIndex = requestedPage.coerceIn(0, pages.lastIndex)

    Text(
        stringResource(R.string.alphabet_reference_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Text(
        stringResource(R.string.alphabet_reference_description),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        stringResource(R.string.alphabet_reference_page, pageIndex + 1, pages.size),
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(12.dp)) {
            pages[pageIndex].forEachIndexed { index, reference ->
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(0.9f)) {
                            ArabicText(
                                reference.letter.arabic,
                                modifier = Modifier.fillMaxWidth(),
                                size = 32,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                reference.letter.transliteration,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        reference.vowelVariants.forEach { variant ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                ArabicText(
                                    variant.arabic,
                                    modifier = Modifier.fillMaxWidth(),
                                    size = 27,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    variant.transliteration.ifEmpty { "—" },
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
                if (index != pages[pageIndex].lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = { requestedPage = pageIndex - 1 },
            modifier = Modifier.weight(1f),
            enabled = pageIndex > 0,
        ) {
            Text(stringResource(R.string.previous_page))
        }
        Button(
            onClick = { requestedPage = pageIndex + 1 },
            modifier = Modifier.weight(1f),
            enabled = pageIndex < pages.lastIndex,
        ) {
            Text(stringResource(R.string.next_page))
        }
    }
}

@Composable
private fun NumberFoundationCard(
    progress: StudyProgress,
    onCompleteNumberLesson: () -> Unit,
    pronouncer: ArabicPronouncer,
) {
    val lessonIndex = progress.completedNumberLessons
        .coerceIn(0, ArabicFoundations.numberLessons.lastIndex)
    val lesson = ArabicFoundations.numberLessons[lessonIndex]
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.number_course_title),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(
                            R.string.foundation_step_progress,
                            lessonIndex + 1,
                            ArabicFoundations.numberLessonCount,
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Text(
                    "${lesson.arabicDigit}  =  ${lesson.westernDigit}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            ArabicText(
                lesson.arabicName,
                modifier = Modifier.fillMaxWidth(),
                size = 32,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                lesson.transliteration,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            FoundationPronunciationButton(
                text = lesson.arabicName,
                pronouncer = pronouncer,
                modifier = Modifier.fillMaxWidth(),
                labelRes = R.string.hear_number,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onCompleteNumberLesson,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    stringResource(
                        if (lessonIndex == ArabicFoundations.numberLessons.lastIndex) {
                            R.string.finish_number_course
                        } else {
                            R.string.next_number
                        },
                    ),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun NumberAccessCard(onStartNumberFoundation: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.numbers_shortcut_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(R.string.numbers_shortcut_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onStartNumberFoundation) {
                Text(stringResource(R.string.review_numbers))
            }
        }
    }
}

@Composable
private fun AlphabetAccessCard(
    progress: StudyProgress,
    onStartAlphabetFoundation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canResume = progress.completedAlphabetLessons in
        1 until ArabicFoundations.alphabetLessonCount
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.alphabet_shortcut_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    if (canResume) {
                        stringResource(
                            R.string.alphabet_shortcut_progress,
                            progress.completedAlphabetLessons,
                            ArabicFoundations.alphabetLessonCount,
                        )
                    } else {
                        stringResource(R.string.alphabet_shortcut_description)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onStartAlphabetFoundation) {
                Text(
                    stringResource(
                        if (canResume) R.string.continue_alphabet
                        else R.string.study_alphabet,
                    ),
                )
            }
        }
    }
}

@Composable
private fun StudyActionBar(
    meaningRevealed: Boolean,
    isNewPresentation: Boolean,
    nextActionOpensCheckpoint: Boolean,
    spacedRepetitionEnabled: Boolean,
    goodTiming: String,
    onRevealMeaning: () -> Unit,
    onNextWord: () -> Unit,
    onAgain: () -> Unit,
    onRemembered: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(8.dp)) {
            if (!meaningRevealed) {
                Text(
                    stringResource(R.string.recall_before_reveal),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = onRevealMeaning,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(stringResource(R.string.reveal_meaning), fontWeight = FontWeight.Bold)
                }
            } else if (isNewPresentation) {
                Button(
                    onClick = onNextWord,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        stringResource(
                            if (nextActionOpensCheckpoint) {
                                R.string.continue_to_context_checkpoint
                            } else {
                                R.string.next_word
                            },
                        ),
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Text(
                    stringResource(R.string.rate_recall_instruction),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onAgain,
                        modifier = Modifier.weight(1f).height(68.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        ReviewActionContent(
                            title = stringResource(R.string.review_hard),
                            detail = stringResource(
                                if (spacedRepetitionEnabled) R.string.review_again_timing
                                else R.string.review_without_schedule,
                            ),
                        )
                    }
                    Button(
                        onClick = onRemembered,
                        modifier = Modifier.weight(1f).height(68.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        ReviewActionContent(
                            title = stringResource(R.string.review_easy),
                            detail = goodTiming,
                        )
                    }
                }
            }
        }
    }
}

internal fun shouldRevealMeaningInitially(status: WordStatus): Boolean = status == WordStatus.New

internal fun studyPresentationStatus(
    status: WordStatus,
    isNewPresentation: Boolean,
): WordStatus = if (isNewPresentation) WordStatus.New else status

internal fun studyWordsForPresentation(
    queuedWords: List<QuranWord>,
    availableWords: List<QuranWord>,
    activeIntroductionId: String?,
): List<QuranWord> = if (queuedWords.isNotEmpty()) {
    queuedWords
} else {
    activeIntroductionId
        ?.let { introducedId -> availableWords.firstOrNull { it.id == introducedId } }
        ?.let(::listOf)
        .orEmpty()
}

@Composable
private fun ReviewActionContent(title: String, detail: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            detail,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun goodReviewTiming(
    spacedRepetitionEnabled: Boolean,
    schedule: ReviewSchedule?,
): String {
    if (!spacedRepetitionEnabled) return stringResource(R.string.review_without_schedule)
    val intervalDays = SpacedRepetition.nextGoodIntervalDays(schedule)
    return if (intervalDays == 1) {
        stringResource(R.string.review_good_tomorrow)
    } else {
        pluralStringResource(
            R.plurals.review_good_days,
            intervalDays,
            intervalDays,
        )
    }
}

internal fun buildStudySession(
    words: List<QuranWord>,
    defaultWord: QuranWord,
    requestedWord: QuranWord?,
): List<QuranWord> {
    val firstWord = requestedWord ?: defaultWord
    val firstIndex = words.indexOfFirst { it.id == firstWord.id }
    return when {
        firstIndex >= 0 -> words.drop(firstIndex) + words.take(firstIndex)
        requestedWord != null -> listOf(requestedWord) + words.filterNot { it.id == requestedWord.id }
        else -> words
    }
}

internal fun displayedStudyWordId(
    savedWordId: String,
    launchTarget: StudyLaunchTarget?,
): String = launchTarget?.wordId ?: savedWordId

internal fun studyQueueSourceWords(
    availableWords: List<QuranWord>,
    requestedWord: QuranWord?,
): List<QuranWord> = availableWords.ifEmpty { listOfNotNull(requestedWord) }

@Composable
private fun StudyHeader(
    progress: StudyProgress,
    dueCount: Int,
    onViewTodayResults: (() -> Unit)?,
) {
    val fraction = (progress.todayCompleted.toFloat() / progress.dailyGoal).coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.today_word), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                if (progress.studyScopes.size > 1) {
                    stringResource(R.string.study_paths_combined, progress.studyScopes.size)
                } else when (progress.studyScopes.single()) {
                    StudyScope.All -> stringResource(R.string.scope_all_description)
                    StudyScope.Frequent50 -> stringResource(R.string.scope_first_50_description)
                    StudyScope.Frequent -> stringResource(R.string.scope_frequent_description)
                    StudyScope.Frequent300 -> stringResource(R.string.scope_top_300_description)
                    StudyScope.Frequent500 -> stringResource(R.string.scope_top_500_description)
                    StudyScope.Prayer -> stringResource(R.string.scope_prayer_description)
                    StudyScope.ShortSurahs -> stringResource(R.string.scope_short_description)
                    StudyScope.Custom -> stringResource(R.string.scope_custom_description)
                    StudyScope.Surahs -> if (progress.selectedSurahs.size <= 4) {
                        stringResource(
                            R.string.scope_surah_list,
                            progress.selectedSurahs.sorted().joinToString(", "),
                        )
                    } else {
                        pluralStringResource(
                            R.plurals.selected_surahs_count,
                            progress.selectedSurahs.size,
                            progress.selectedSurahs.size,
                        )
                    }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            if (dueCount > 0) {
                Text(
                    pluralStringResource(R.plurals.reviews_due, dueCount, dueCount),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
            shape = RoundedCornerShape(100.dp),
        ) {
            Text(
                pluralStringResource(
                    R.plurals.streak_days,
                    progress.streakDays,
                    progress.streakDays,
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.weight(1f).height(7.dp),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "${progress.todayCompleted}/${progress.dailyGoal}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
    if (onViewTodayResults != null) {
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onViewTodayResults,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(stringResource(R.string.view_today_results), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun WordCard(
    word: QuranWord,
    progress: StudyProgress,
    displayedStatus: WordStatus,
    pronouncer: ArabicPronouncer,
    meaningRevealed: Boolean,
    onRevealChange: (Boolean) -> Unit,
    onToggleCustomList: (String) -> Unit,
    onToggleAlreadyKnown: (String) -> Unit,
    showCompleteAyah: Boolean,
    onShowCompleteAyahChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(100.dp)) {
                    Text(
                        word.category,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                WordStatusPill(displayedStatus)
            }
            Spacer(Modifier.height(22.dp))
            ArabicText(
                word.arabic,
                modifier = Modifier.fillMaxWidth(),
                size = 50,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                word.transliteration,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = { onToggleAlreadyKnown(word.id) }) {
                Text(
                    stringResource(
                        if (word.id in progress.alreadyKnownIds) {
                            R.string.restore_to_practice
                        } else {
                            R.string.mark_already_known
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PronunciationButton(
                    word = word,
                    pronouncer = pronouncer,
                    modifier = Modifier.size(44.dp),
                    compact = true,
                    labelRes = R.string.device_voice_slow,
                    playbackRate = ArabicPronouncer.WORD_SLOW_RATE,
                )
            }
            Spacer(Modifier.height(14.dp))
            WordCollectionActions(
                word = word,
                inCustomList = word.id in progress.customStudyIds,
                alreadyKnown = word.id in progress.alreadyKnownIds,
                onToggleCustomList = onToggleCustomList,
                onToggleAlreadyKnown = {},
                showAlreadyKnown = false,
            )
            if (meaningRevealed) {
                Text(
                    word.meaning,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(18.dp))
                RootAndGrammar(word.root, word.grammar)
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                Spacer(Modifier.height(20.dp))
                Box(Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            word.reference,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        if (showCompleteAyah) {
                            VerseExplorerPanel(word = word)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                VersePronunciationButton(
                                    word = word,
                                    pronouncer = pronouncer,
                                    modifier = Modifier.weight(1f),
                                    dense = true,
                                    centerLabel = true,
                                    labelRes = R.string.ayah_audio_short,
                                )
                                OutlinedButton(
                                    onClick = { onShowCompleteAyahChange(false) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(stringResource(R.string.hide_complete_ayah))
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onShowCompleteAyahChange(true) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.show_complete_ayah))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        "💡 ${word.learnerInsight}",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(14.dp))
                TextButton(onClick = { onRevealChange(false) }) {
                    Text(stringResource(R.string.hide_meaning))
                }
            }
            EditorialReviewPanel(word)
        }
    }
}
