package com.kalima.quran.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import com.kalima.quran.audio.ArabicVoiceInstaller
import com.kalima.quran.audio.PronunciationResult
import androidx.annotation.StringRes

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
    arabic: String,
    pronouncer: ArabicPronouncer,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    @StringRes labelRes: Int = R.string.listen_pronunciation,
    speechRate: Float = ArabicPronouncer.DEFAULT_RATE,
    repeatCount: Int = 1,
    contentColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
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
    val loadingMessage = stringResource(R.string.pronunciation_loading)
    val unavailableMessage = stringResource(R.string.pronunciation_unavailable)
    val installationFailedMessage = stringResource(R.string.pronunciation_installation_failed)
    val failedMessage = stringResource(R.string.pronunciation_failed)
    val onClick: () -> Unit = {
        when (pronouncer.speak(arabic, speechRate, repeatCount)) {
            PronunciationResult.Started -> Unit
            PronunciationResult.Initializing -> {
                Toast.makeText(context, loadingMessage, Toast.LENGTH_LONG).show()
            }
            PronunciationResult.Unavailable -> {
                Toast.makeText(context, unavailableMessage, Toast.LENGTH_LONG).show()
                val opened = ArabicVoiceInstaller.open(
                    context = context,
                    preferredEnginePackage = pronouncer.preferredEnginePackage(),
                )
                if (!opened) {
                    Toast.makeText(context, installationFailedMessage, Toast.LENGTH_LONG).show()
                }
            }
            PronunciationResult.Failed -> {
                Toast.makeText(context, failedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

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
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_volume_up),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    }
}
