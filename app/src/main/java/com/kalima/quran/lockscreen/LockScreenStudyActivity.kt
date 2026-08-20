package com.kalima.quran.lockscreen

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.kalima.quran.MainActivity
import com.kalima.quran.data.ProgressStore
import com.kalima.quran.data.WordRepository
import com.kalima.quran.localization.LanguageManager
import com.kalima.quran.quiz.LockScreenContent
import com.kalima.quran.quiz.LockScreenSession
import com.kalima.quran.quiz.QuizQuestion
import com.kalima.quran.quiz.QuizQuestionType

class LockScreenStudyActivity : ComponentActivity() {
    private lateinit var progressStore: ProgressStore
    private lateinit var currentSession: LockScreenSession
    private var studyRememberedSelection: Boolean? = null
    private var quizSelectedOption: Int? = null
    private var receiverRegistered = false
    private var openingMainApp = false
    private var answerCommitted = false
    private var preview = false

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> finish()
                ACTION_CLOSE -> if (!openingMainApp) finish()
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.localizedContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressStore = ProgressStore.get(applicationContext)
        preview = intent.getBooleanExtra(EXTRA_PREVIEW, false)
        if (!preview && LockScreenSystemSafety.blockReason(this) != null) {
            progressStore.recordLockScreenSafetySkip()
            finish()
            return
        }
        configureWindow()

        val session = if (preview) {
            restoreContent(savedInstanceState)?.let { restored ->
                LockScreenSession(
                    id = savedInstanceState?.getString(STATE_SESSION_ID)
                        ?: "preview-${SystemClock.elapsedRealtimeNanos()}",
                    content = restored,
                    shown = true,
                )
            } ?: progressStore.nextLockScreenSession(preview = true)
        } else {
            progressStore.nextLockScreenSession(preview = false)
        }
        if (session == null) {
            finish()
            return
        }
        currentSession = session
        studyRememberedSelection = savedInstanceState
            ?.takeIf { it.containsKey(STATE_STUDY_REMEMBERED) }
            ?.getBoolean(STATE_STUDY_REMEMBERED)
        quizSelectedOption = savedInstanceState?.getInt(STATE_SELECTED_OPTION, -1)
            ?.takeIf { it in 0 until QuizQuestion.OPTION_COUNT }
        intent.getLongExtra(EXTRA_REQUESTED_AT_ELAPSED, -1L)
            .takeIf { it >= 0L && savedInstanceState == null }
            ?.let { requestedAt ->
                progressStore.recordLockScreenLatency(SystemClock.elapsedRealtime() - requestedAt)
            }
        val progress = progressStore.progress.value

        setContent {
            when (val content = currentSession.content) {
                is LockScreenContent.WordCard -> LockScreenStudyScreen(
                    word = content.word,
                    spacedRepetitionEnabled = progress.spacedRepetitionEnabled,
                    initialRememberedSelection = studyRememberedSelection,
                    showCompleteAyah = progress.showCompleteAyah,
                    onShowCompleteAyahChange = progressStore::setShowCompleteAyah,
                    onSelect = { remembered -> studyRememberedSelection = remembered },
                    onConfirm = confirm@{
                        val remembered = studyRememberedSelection ?: return@confirm
                        if (!answerCommitted) {
                            answerCommitted = if (preview) {
                                true
                            } else {
                                progressStore.commitLockScreenAnswer(
                                    currentSession.id,
                                    content.word.id,
                                    remembered,
                                )
                            }
                        }
                        if (answerCommitted) finish()
                    },
                    onAlreadyKnown = {
                        if (!answerCommitted) {
                            answerCommitted = if (preview) {
                                true
                            } else {
                                progressStore.commitLockScreenAlreadyKnown(
                                    currentSession.id,
                                    content.word.id,
                                )
                            }
                        }
                        if (answerCommitted) {
                            if (!preview) {
                                Toast.makeText(
                                    this@LockScreenStudyActivity,
                                    com.kalima.quran.R.string.word_excluded_message,
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                            finish()
                        }
                    },
                    onDismiss = ::finish,
                    onOpenApp = ::openMainApp,
                )

                is LockScreenContent.QuizCard -> LockScreenQuizScreen(
                    question = content.question,
                    initialSelectedOption = quizSelectedOption,
                    onAnswered = { option, _ -> quizSelectedOption = option },
                    onContinue = continueQuiz@{
                        val option = quizSelectedOption ?: return@continueQuiz
                        if (!answerCommitted) {
                            answerCommitted = if (preview) {
                                true
                            } else {
                                progressStore.commitLockScreenQuizAnswer(
                                    currentSession.id,
                                    content.question.word.id,
                                    option == content.question.correctOptionIndex,
                                )
                            }
                        }
                        if (answerCommitted) finish()
                    },
                    onDismiss = ::finish,
                    onOpenApp = ::openMainApp,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(ACTION_CLOSE)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(
            this,
            closeReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    override fun onPause() {
        super.onPause()
        if (!preview && !openingMainApp && !answerCommitted && !isChangingConfigurations) {
            finish()
        }
    }

    override fun onStop() {
        if (receiverRegistered) {
            unregisterReceiver(closeReceiver)
            receiverRegistered = false
        }
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_SESSION_ID, currentSession.id)
        studyRememberedSelection?.let { outState.putBoolean(STATE_STUDY_REMEMBERED, it) }
        when (val content = currentSession.content) {
            is LockScreenContent.WordCard -> {
                outState.putString(STATE_CONTENT_TYPE, CONTENT_WORD)
                outState.putString(STATE_WORD_ID, content.word.id)
            }

            is LockScreenContent.QuizCard -> {
                outState.putString(STATE_CONTENT_TYPE, CONTENT_QUIZ)
                outState.putString(STATE_WORD_ID, content.question.word.id)
                outState.putString(STATE_QUESTION_TYPE, content.question.type.name)
                outState.putStringArrayList(STATE_OPTIONS, ArrayList(content.question.options))
                outState.putInt(STATE_CORRECT_OPTION, content.question.correctOptionIndex)
                quizSelectedOption?.let { outState.putInt(STATE_SELECTED_OPTION, it) }
            }
        }
        super.onSaveInstanceState(outState)
    }

    private fun restoreContent(state: Bundle?): LockScreenContent? {
        if (state == null) return null
        val word = WordRepository.wordById(state.getString(STATE_WORD_ID))
            ?: return null
        return when (state.getString(STATE_CONTENT_TYPE)) {
            CONTENT_WORD -> LockScreenContent.WordCard(word)
            CONTENT_QUIZ -> {
                val type = state.getString(STATE_QUESTION_TYPE)
                    ?.let { stored -> QuizQuestionType.entries.firstOrNull { it.name == stored } }
                    ?: return null
                val options = state.getStringArrayList(STATE_OPTIONS)?.toList() ?: return null
                val correctOption = state.getInt(STATE_CORRECT_OPTION, -1)
                if (options.size != QuizQuestion.OPTION_COUNT || correctOption !in options.indices) return null
                LockScreenContent.QuizCard(QuizQuestion(word, type, options, correctOption))
            }

            else -> null
        }
    }

    private fun configureWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private fun openMainApp() {
        if (openingMainApp) return
        openingMainApp = true

        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (!keyguardManager.isKeyguardLocked) {
            launchMainActivity()
            return
        }

        keyguardManager.requestDismissKeyguard(
            this,
            object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() = launchMainActivity()

                override fun onDismissCancelled() {
                    openingMainApp = false
                }

                override fun onDismissError() {
                    openingMainApp = false
                }
            },
        )
    }

    private fun launchMainActivity() {
        if (!preview) progressStore.clearPendingLockScreenSession()
        val wordId = when (val content = currentSession.content) {
            is LockScreenContent.WordCard -> content.word.id
            is LockScreenContent.QuizCard -> content.question.word.id
        }
        startActivity(
            MainActivity.createStudyIntent(this, wordId).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
            },
        )
        finish()
    }

    companion object {
        const val EXTRA_PREVIEW = "com.kalima.quran.extra.LOCK_SCREEN_PREVIEW"
        const val EXTRA_REQUESTED_AT_ELAPSED = "com.kalima.quran.extra.LOCK_SCREEN_REQUESTED_AT"
        const val ACTION_CLOSE = "com.kalima.quran.action.CLOSE_LOCK_SCREEN_CARD"
        private const val STATE_SESSION_ID = "session_id"
        private const val STATE_STUDY_REMEMBERED = "study_remembered"
        private const val STATE_CONTENT_TYPE = "content_type"
        private const val STATE_WORD_ID = "word_id"
        private const val STATE_QUESTION_TYPE = "question_type"
        private const val STATE_OPTIONS = "options"
        private const val STATE_CORRECT_OPTION = "correct_option"
        private const val STATE_SELECTED_OPTION = "selected_option"
        private const val CONTENT_WORD = "word"
        private const val CONTENT_QUIZ = "quiz"
    }
}
