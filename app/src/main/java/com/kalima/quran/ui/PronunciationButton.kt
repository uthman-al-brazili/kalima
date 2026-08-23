package com.kalima.quran.ui

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.audio.ArabicPronouncer
import com.kalima.quran.audio.PronunciationResult
import com.kalima.quran.audio.VerseAudioPlaybackProgress
import com.kalima.quran.data.QuranVerseAudioLocation
import com.kalima.quran.data.QuranWord

@Composable
fun rememberArabicPronouncer(): ArabicPronouncer {
    val applicationContext = LocalContext.current.applicationContext
    val pronouncer = remember(applicationContext) { ArabicPronouncer(applicationContext) }
    DisposableEffect(pronouncer) {
        onDispose(pronouncer::shutdown)
    }
    return pronouncer
}

@Composable
fun PronunciationButton(
    word: QuranWord,
    pronouncer: ArabicPronouncer,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    dense: Boolean = false,
    @StringRes labelRes: Int = R.string.listen_pronunciation,
    playbackRate: Float = ArabicPronouncer.WORD_DEFAULT_RATE,
    repeatCount: Int = 1,
    contentColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
) {
    PronunciationControl(
        play = { onPlaybackResult ->
            pronouncer.speakWord(
                location = word.audioLocation,
                playbackRate = playbackRate,
                repeatCount = repeatCount,
                onPlaybackResult = onPlaybackResult,
            )
        },
        modifier = modifier,
        compact = compact,
        dense = dense,
        labelRes = labelRes,
        offlineMessageRes = R.string.offline_word_audio_missing,
        contentColor = contentColor,
        borderColor = borderColor,
    )
}

@Composable
fun VersePronunciationButton(
    word: QuranWord,
    pronouncer: ArabicPronouncer,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    dense: Boolean = false,
    @StringRes labelRes: Int = R.string.hussary_verse_recitation,
    playbackRate: Float = ArabicPronouncer.VERSE_DEFAULT_RATE,
    repeatCount: Int = 1,
    contentColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    onPlaybackProgress: (VerseAudioPlaybackProgress) -> Unit = {},
) {
    val verseLocation = word.audioLocation?.let(QuranVerseAudioLocation::fromWord)
    PronunciationControl(
        play = { onPlaybackResult ->
            pronouncer.speakVerse(
                location = verseLocation,
                playbackRate = playbackRate,
                repeatCount = repeatCount,
                onPlaybackResult = onPlaybackResult,
                onPlaybackProgress = onPlaybackProgress,
            )
        },
        modifier = modifier,
        compact = compact,
        dense = dense,
        labelRes = labelRes,
        offlineMessageRes = R.string.offline_verse_audio_missing,
        contentColor = contentColor,
        borderColor = borderColor,
    )
}

@Composable
fun FoundationPronunciationButton(
    text: String,
    pronouncer: ArabicPronouncer,
    modifier: Modifier = Modifier,
    @StringRes labelRes: Int,
    compact: Boolean = false,
) {
    PronunciationControl(
        play = { onPlaybackResult ->
            pronouncer.speakFoundation(
                text = text,
                onPlaybackResult = onPlaybackResult,
            )
        },
        modifier = modifier,
        compact = compact,
        dense = true,
        labelRes = labelRes,
        offlineMessageRes = R.string.foundation_voice_unavailable,
        contentColor = MaterialTheme.colorScheme.primary,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
    )
}

@Composable
private fun PronunciationControl(
    play: ((PronunciationResult) -> Unit) -> PronunciationResult,
    modifier: Modifier,
    compact: Boolean,
    dense: Boolean,
    @StringRes labelRes: Int,
    @StringRes offlineMessageRes: Int,
    contentColor: Color,
    borderColor: Color,
) {
    val resolvedContentColor = if (contentColor == Color.Unspecified) {
        MaterialTheme.colorScheme.primary
    } else {
        contentColor
    }
    val resolvedBorderColor = if (borderColor == Color.Unspecified) {
        resolvedContentColor.copy(alpha = 0.55f)
    } else {
        borderColor
    }
    val context = LocalContext.current
    val label = stringResource(labelRes)
    val failedMessage = stringResource(R.string.pronunciation_failed)
    val offlineAudioMissingMessage = stringResource(offlineMessageRes)
    val deviceVoiceUnavailableMessage = stringResource(R.string.foundation_voice_unavailable)
    fun handleResult(result: PronunciationResult) {
        when (result) {
            PronunciationResult.Started -> Unit
            PronunciationResult.OfflineAudioMissing -> {
                Toast.makeText(context, offlineAudioMissingMessage, Toast.LENGTH_LONG).show()
            }
            PronunciationResult.DeviceVoiceUnavailable -> {
                Toast.makeText(context, deviceVoiceUnavailableMessage, Toast.LENGTH_LONG).show()
            }
            PronunciationResult.Failed -> {
                Toast.makeText(context, failedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }
    val onClick: () -> Unit = { handleResult(play(::handleResult)) }

    if (compact) {
        IconButton(onClick = onClick, modifier = modifier) {
            Icon(
                painter = painterResource(R.drawable.ic_volume_up),
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = resolvedContentColor,
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = resolvedContentColor),
            border = BorderStroke(1.dp, resolvedBorderColor),
            contentPadding = if (dense) {
                PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            } else {
                ButtonDefaults.ContentPadding
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_volume_up),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = if (dense) {
                    MaterialTheme.typography.labelMedium
                } else {
                    MaterialTheme.typography.labelLarge
                },
            )
        }
    }
}
