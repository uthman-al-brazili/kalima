package com.kalima.quran.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.kalima.quran.ui.theme.Forest

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
    contentColor: Color = Forest,
    borderColor: Color = contentColor.copy(alpha = 0.55f),
) {
    val context = LocalContext.current
    val label = stringResource(R.string.listen_pronunciation)
    val loadingMessage = stringResource(R.string.pronunciation_loading)
    val unavailableMessage = stringResource(R.string.pronunciation_unavailable)
    val failedMessage = stringResource(R.string.pronunciation_failed)
    val onClick: () -> Unit = {
        val message = when (pronouncer.speak(arabic)) {
            PronunciationResult.Started -> null
            PronunciationResult.Initializing -> loadingMessage
            PronunciationResult.Unavailable -> unavailableMessage
            PronunciationResult.Failed -> failedMessage
        }
        message?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        Unit
    }

    if (compact) {
        IconButton(onClick = onClick, modifier = modifier) {
            Icon(
                painter = painterResource(R.drawable.ic_volume_up),
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = contentColor,
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
            border = BorderStroke(1.dp, borderColor),
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
