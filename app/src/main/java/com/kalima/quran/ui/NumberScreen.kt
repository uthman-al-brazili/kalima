package com.kalima.quran.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.audio.ArabicPronouncer
import com.kalima.quran.data.ArabicFoundations
import com.kalima.quran.data.StudyProgress
import com.kalima.quran.data.hasNumberFoundationLesson

@Composable
fun NumberScreen(
    progress: StudyProgress,
    onCompleteNumberLesson: () -> Unit,
    onStartNumberFoundation: () -> Unit,
    pronouncer: ArabicPronouncer,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        if (progress.hasNumberFoundationLesson) {
            NumberFoundationCard(
                progress = progress,
                onCompleteNumberLesson = onCompleteNumberLesson,
                pronouncer = pronouncer,
            )
        } else {
            NumberAccessCard(onStartNumberFoundation = onStartNumberFoundation)
        }
        Spacer(Modifier.height(12.dp))
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
                Text(
                    stringResource(
                        R.string.foundation_step_progress,
                        lessonIndex + 1,
                        ArabicFoundations.numberLessonCount,
                    ),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelMedium,
                )
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
                audioResourceName = lesson.audioResourceName,
                pronouncer = pronouncer,
                modifier = Modifier.fillMaxWidth(),
                labelRes = R.string.listen_pronunciation,
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
                            R.string.finish_action
                        } else {
                            R.string.continue_action
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.foundation_course_title),
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
