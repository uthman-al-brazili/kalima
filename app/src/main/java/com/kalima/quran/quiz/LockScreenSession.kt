package com.kalima.quran.quiz

import com.kalima.quran.data.QuranWord
import java.nio.charset.StandardCharsets
import java.util.Base64

data class LockScreenSession(
    val id: String,
    val content: LockScreenContent,
    val shown: Boolean = false,
)

object LockScreenSessionCodec {
    private const val VERSION = "1"
    private const val WORD = "word"
    private const val QUIZ = "quiz"

    fun encode(session: LockScreenSession): String {
        val common = listOf(VERSION, session.id, if (session.shown) "1" else "0")
        val fields = when (val content = session.content) {
            is LockScreenContent.WordCard -> common + listOf(WORD, content.word.id)
            is LockScreenContent.QuizCard -> common + listOf(
                QUIZ,
                content.question.word.id,
                content.question.type.name,
                content.question.correctOptionIndex.toString(),
                content.question.options.joinToString(",", transform = ::encodeText),
            )
        }
        return fields.joinToString("|")
    }

    fun decode(
        value: String?,
        wordForId: (String) -> QuranWord?,
    ): LockScreenSession? {
        if (value.isNullOrBlank()) return null
        val fields = value.split('|')
        if (fields.size < 5 || fields[0] != VERSION || fields[1].isBlank()) return null
        val word = wordForId(fields[4]) ?: return null
        val content = when (fields[3]) {
            WORD -> if (fields.size == 5) LockScreenContent.WordCard(word) else return null
            QUIZ -> {
                if (fields.size != 8) return null
                val type = QuizQuestionType.entries.firstOrNull { it.name == fields[5] }
                    ?: return null
                val correct = fields[6].toIntOrNull() ?: return null
                val options = fields[7].split(',').mapNotNull(::decodeText)
                if (options.size != QuizQuestion.OPTION_COUNT || correct !in options.indices) return null
                LockScreenContent.QuizCard(QuizQuestion(word, type, options, correct))
            }
            else -> return null
        }
        return LockScreenSession(fields[1], content, shown = fields[2] == "1")
    }

    private fun encodeText(value: String): String = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeText(value: String): String? = runCatching {
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    }.getOrNull()
}
