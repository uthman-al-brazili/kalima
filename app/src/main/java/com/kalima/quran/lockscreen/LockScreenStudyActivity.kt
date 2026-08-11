package com.kalima.quran.lockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.kalima.quran.data.ProgressStore
import com.kalima.quran.data.WordRepository
import com.kalima.quran.quiz.LockScreenContent
import com.kalima.quran.quiz.QuizQuestion
import com.kalima.quran.quiz.QuizQuestionType

class LockScreenStudyActivity : ComponentActivity() {
    private lateinit var progressStore: ProgressStore
    private lateinit var currentContent: LockScreenContent
    private var quizSelectedOption: Int? = null
    private var receiverRegistered = false

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            when (intent?.action) {
                ACTION_CLOSE,
                Intent.ACTION_SCREEN_OFF,
                Intent.ACTION_USER_PRESENT,
                -> finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressStore = ProgressStore(applicationContext)
        configureWindowForLockScreen()

        currentContent = restoreContent(savedInstanceState)
            ?: progressStore.nextLockScreenContent()

        setContent {
            when (val content = currentContent) {
                is LockScreenContent.WordCard -> LockScreenStudyScreen(
                    word = content.word,
                    onReview = {
                        progressStore.answer(content.word.id, learned = false)
                        finish()
                    },
                    onLearned = {
                        progressStore.answer(content.word.id, learned = true)
                        finish()
                    },
                    onDismiss = ::finish,
                )

                is LockScreenContent.QuizCard -> LockScreenQuizScreen(
                    question = content.question,
                    initialSelectedOption = quizSelectedOption,
                    onAnswered = { option, correct ->
                        quizSelectedOption = option
                        progressStore.answerQuiz(content.question.word.id, correct)
                    },
                    onContinue = ::finish,
                    onDismiss = ::finish,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(ACTION_CLOSE)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(
            this,
            closeReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    override fun onStop() {
        if (receiverRegistered) {
            unregisterReceiver(closeReceiver)
            receiverRegistered = false
        }
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        when (val content = currentContent) {
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
        val word = WordRepository.words.firstOrNull { it.id == state.getString(STATE_WORD_ID) }
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
                quizSelectedOption = state.getInt(STATE_SELECTED_OPTION, -1).takeIf { it in options.indices }
                LockScreenContent.QuizCard(QuizQuestion(word, type, options, correctOption))
            }

            else -> null
        }
    }

    private fun configureWindowForLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(false)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    companion object {
        const val ACTION_CLOSE = "com.kalima.quran.action.CLOSE_LOCK_SCREEN_CARD"
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
