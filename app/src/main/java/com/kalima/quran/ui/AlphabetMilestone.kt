package com.kalima.quran.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.audio.ArabicPronouncer
import com.kalima.quran.data.QuranicDecodingMilestone

@Composable
internal fun AlphabetPromptArabicText(
    text: String,
    modifier: Modifier = Modifier,
    size: Int,
    color: androidx.compose.ui.graphics.Color,
) {
    if (!text.endsWith(ARABIC_KASRA)) {
        ArabicText(text, modifier = modifier, size = size, color = color)
        return
    }
    Box(
        modifier = modifier.clearAndSetSemantics { contentDescription = text },
        contentAlignment = Alignment.Center,
    ) {
        ArabicText(
            text = text.dropLast(ARABIC_KASRA.length),
            modifier = Modifier.fillMaxWidth(),
            size = size,
            color = color,
        )
        Canvas(Modifier.matchParentSize()) {
            val halfLength = 10.dp.toPx()
            val rise = 3.dp.toPx()
            val centerX = this.size.width / 2f
            val centerY = this.size.height * 0.92f
            drawLine(
                color = color,
                start = Offset(centerX - halfLength, centerY + rise),
                end = Offset(centerX + halfLength, centerY - rise),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

private const val ARABIC_KASRA = "ِ"

@Composable
internal fun AlphabetDecodingMilestone(
    milestone: QuranicDecodingMilestone,
    pronouncer: ArabicPronouncer,
    onComplete: () -> Unit,
) {
    var readingHintVisible by rememberSaveable(milestone.word) { mutableStateOf(false) }
    var meaningVisible by rememberSaveable(milestone.word) { mutableStateOf(false) }
    Spacer(Modifier.height(4.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ArabicText(
                milestone.word,
                modifier = Modifier.fillMaxWidth(),
                size = 68,
                color = MaterialTheme.colorScheme.primary,
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    milestone.segments.forEach { segment ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            ArabicText(
                                segment,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                size = 28,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (pronouncer.hasFoundationAudio(milestone.audioResourceName)) {
                FoundationPronunciationButton(
                    text = milestone.word,
                    audioResourceName = milestone.audioResourceName,
                    pronouncer = pronouncer,
                    labelRes = R.string.alphabet_hear_word,
                )
            } else {
                Text(
                    stringResource(R.string.alphabet_audio_fallback_note),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    if (readingHintVisible) {
        Text(
            milestone.transliteration,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    } else {
        TextButton(
            onClick = { readingHintVisible = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.alphabet_show_reading_hint))
        }
    }
    if (meaningVisible) {
        val meaning = if (LocalLocale.current.language == "pt") {
            milestone.meaningPortuguese
        } else {
            milestone.meaningEnglish
        }
        Text(
            stringResource(R.string.alphabet_meaning, milestone.word, meaning),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.alphabet_complete_milestone))
        }
    } else {
        Button(
            onClick = { meaningVisible = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.alphabet_show_meaning))
        }
    }
}
