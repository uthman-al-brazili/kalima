package com.kalima.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ArabicFoundationsTest {
    @Test
    fun `alphabet plan covers every letter once`() {
        val symbols = ArabicFoundations.alphabetLessons
            .flatMap(AlphabetLesson::symbols)
        val letters = symbols.map(FoundationSymbol::arabic)
        val recordings = symbols.map { AlphabetAudio.fromSpokenArabic(it.spokenArabic) }

        assertEquals(7, ArabicFoundations.alphabetLessons.size)
        assertEquals(28, letters.size)
        assertEquals(28, letters.toSet().size)
        assertTrue(letters.all { it.length == 1 })
        assertEquals(28, ArabicFoundations.alphabetReference.size)
        assertTrue(recordings.all { it != null })
        assertEquals(28, recordings.toSet().size)
    }

    @Test
    fun `alphabet lessons block complete words until finished`() {
        val starting = StudyProgress(
            alphabetCourseRequested = true,
            alphabetFoundationRequired = true,
            completedAlphabetLessons = 0,
        )
        val finished = starting.copy(
            completedAlphabetLessons = ArabicFoundations.alphabetLessonCount,
        )

        assertTrue(starting.needsAlphabetFoundation)
        assertFalse(finished.needsAlphabetFoundation)
    }

    @Test
    fun `every alphabet lesson has a complete recognition check`() {
        ArabicFoundations.alphabetLessons.forEachIndexed { lessonIndex, lesson ->
            val questions = ArabicFoundations.practiceQuestions(lessonIndex)

            assertEquals(lesson.symbols.size * 4, questions.size)
            assertEquals(lesson.symbols.toSet(), questions.map(AlphabetPracticeQuestion::symbol).toSet())
            questions.forEach { question ->
                assertEquals(4, question.options.size)
                assertEquals(4, question.options.toSet().size)
                assertTrue(question.correctOptionIndex in question.options.indices)
            }
        }
    }

    @Test
    fun `symbol recognition choices use complete letter names`() {
        val nameQuestions = ArabicFoundations.alphabetLessons.indices
            .flatMap(ArabicFoundations::practiceQuestions)
            .filter { it.type == AlphabetQuestionType.GlyphToSound }

        nameQuestions.forEach { question ->
            assertEquals(
                question.symbol.transliteration,
                question.options[question.correctOptionIndex].text,
            )
        }
        val choices = nameQuestions.flatMap(AlphabetPracticeQuestion::options)
            .map(AlphabetPracticeOption::text)
        assertTrue("jīm" in choices)
        assertTrue("ḥāʾ" in choices)
        assertTrue("khāʾ" in choices)
        assertTrue("ʿayn" in choices)
    }

    @Test
    fun `shape families keep confusing pairs together and teach connection breaks`() {
        val lessonFor = ArabicFoundations.alphabetLessons.flatMapIndexed { index, lesson ->
            lesson.symbols.map { it.arabic to index }
        }.toMap()

        listOf("د" to "ذ", "س" to "ش", "ط" to "ظ", "ف" to "ق").forEach { (left, right) ->
            assertEquals(lessonFor[left], lessonFor[right])
        }
        val breakLetters = ArabicFoundations.alphabetLessons.flatMap(AlphabetLesson::symbols)
            .filterNot(FoundationSymbol::connectsToFollowing)
            .map(FoundationSymbol::arabic)
            .toSet()
        assertEquals(setOf("ا", "د", "ذ", "ر", "ز", "و"), breakLetters)
    }

    @Test
    fun `every lesson ends with a manually curated decoding milestone`() {
        ArabicFoundations.alphabetLessons.forEach { lesson ->
            assertTrue(lesson.milestone.word.isNotBlank())
            assertTrue(lesson.milestone.segments.size >= 2)
            assertTrue(lesson.milestone.audioResourceName.startsWith("arabic_word_"))
        }
    }

    @Test
    fun `cumulative practice brings back due dimensions from earlier lessons`() {
        val now = Instant.parse("2026-08-29T12:00:00Z")
        val earlierQuestion = ArabicFoundations.practiceQuestions(0).first()
        val future = SpacedRepetition.review(null, ReviewGrade.Good, now)
        val withoutDueCard = ArabicFoundations.cumulativePracticeQuestions(
            lessonIndex = 1,
            schedules = mapOf(earlierQuestion.masteryKey to future),
            now = now,
        )
        val withDueCard = ArabicFoundations.cumulativePracticeQuestions(
            lessonIndex = 1,
            schedules = mapOf(
                earlierQuestion.masteryKey to future.copy(dueAt = now.minusSeconds(1)),
            ),
            now = now,
        )

        assertFalse(withoutDueCard.any { it.masteryKey == earlierQuestion.masteryKey })
        assertTrue(withDueCard.any { it.masteryKey == earlierQuestion.masteryKey })
    }

    @Test
    fun `every foundation symbol provides Arabic speech text`() {
        val symbols = ArabicFoundations.alphabetLessons.flatMap(AlphabetLesson::symbols)

        assertTrue(symbols.all { it.spokenArabic.isNotBlank() })
        assertEquals(
            "أَلِف",
            symbols.single { it.arabic == "ا" }.spokenArabic,
        )
        assertEquals(
            "يَاء",
            symbols.single { it.arabic == "ي" }.spokenArabic,
        )
    }

    @Test
    fun `alphabet reference covers all letters and four vowel forms with speech`() {
        val reference = ArabicFoundations.alphabetReference

        assertEquals(28, reference.size)
        assertEquals(28, reference.map { it.letter.arabic }.toSet().size)
        reference.forEach { row ->
            assertEquals(4, row.vowelVariants.size)
            assertEquals(
                listOf('َ', 'ِ', 'ُ', 'ْ'),
                row.vowelVariants.map { it.arabic.last() },
            )
            assertTrue(row.vowelVariants.all { it.spokenArabic == it.arabic })
            assertTrue(row.vowelVariants.all { it.transliteration.isNotBlank() })
        }
    }

    @Test
    fun `alphabet reference uses accurate hamza forms for initial alif vowels`() {
        val alif = ArabicFoundations.alphabetReference.first()

        assertEquals(listOf("أَ", "إِ", "أُ", "أْ"), alif.vowelVariants.map(FoundationSymbol::arabic))
        assertEquals(listOf("ʾa", "ʾi", "ʾu", "ʾ"), alif.vowelVariants.map(FoundationSymbol::transliteration))
        assertEquals("إِ", alif.vowelVariants[1].spokenArabic)
    }

    @Test
    fun `alphabet reference has compact pages of four letters`() {
        val pages = ArabicFoundations.alphabetReference.chunked(ArabicFoundations.alphabetReferencePageSize)

        assertEquals(7, pages.size)
        assertTrue(pages.all { it.size == ArabicFoundations.alphabetReferencePageSize })
        assertEquals(listOf("ا", "ب", "ت", "ث"), pages.first().map { it.letter.arabic })
    }

    @Test
    fun `skipping alphabet keeps lesson progress and resuming continues it`() {
        val inProgress = StudyProgress(
            alphabetCourseRequested = true,
            alphabetFoundationRequired = true,
            completedAlphabetLessons = 3,
        )

        val skipped = inProgress.skipAlphabetFoundation()
        val resumed = skipped.startAlphabetFoundation()

        assertFalse(skipped.needsAlphabetFoundation)
        assertEquals(3, skipped.completedAlphabetLessons)
        assertFalse(resumed.needsAlphabetFoundation)
        assertTrue(resumed.hasAlphabetFoundationLesson)
        assertEquals(3, resumed.completedAlphabetLessons)
    }

    @Test
    fun `a completed or previously declined alphabet course can be studied from the beginning`() {
        val previouslyKnown = StudyProgress(
            alphabetCourseRequested = false,
            completedAlphabetLessons = ArabicFoundations.alphabetLessonCount,
        )

        val restarted = previouslyKnown.startAlphabetFoundation()

        assertFalse(restarted.needsAlphabetFoundation)
        assertTrue(restarted.hasAlphabetFoundationLesson)
        assertEquals(0, restarted.completedAlphabetLessons)
    }

    @Test
    fun `number lessons remain parallel and cover zero through nine`() {
        val progress = StudyProgress(
            numberCourseRequested = true,
            completedNumberLessons = 4,
        )

        assertFalse(progress.needsAlphabetFoundation)
        assertTrue(progress.hasNumberFoundationLesson)
        assertEquals((0..9).toList(), ArabicFoundations.numberLessons.map(NumberLesson::westernDigit))
        assertEquals(
            listOf("٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩"),
            ArabicFoundations.numberLessons.map(NumberLesson::arabicDigit),
        )
    }

    @Test
    fun `a completed or previously declined number course can be studied from the beginning`() {
        val previouslyKnown = StudyProgress(
            numberCourseRequested = false,
            completedNumberLessons = ArabicFoundations.numberLessonCount,
        )

        val restarted = previouslyKnown.startNumberFoundation()

        assertTrue(restarted.hasNumberFoundationLesson)
        assertEquals(0, restarted.completedNumberLessons)
    }
}
