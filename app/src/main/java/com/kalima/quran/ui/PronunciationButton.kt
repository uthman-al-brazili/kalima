package com.kalima.quran.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kalima.quran.R
import com.kalima.quran.audio.ArabicPronouncer
import com.kalima.quran.audio.ArabicVoiceInstaller
import com.kalima.quran.audio.PronunciationResult
import androidx.annotation.StringRes

@Composable
fun rememberArabicPronouncer(): ArabicPronouncer {
    val applicationContext = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val pronouncer = remember(applicationContext) { ArabicPronouncer(applicationContext) }
    DisposableEffect(pronouncer, lifecycleOwner) {
        var refreshOnResume = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> refreshOnResume = true
                Lifecycle.Event.ON_RESUME -> if (refreshOnResume) {
                    refreshOnResume = false
                    pronouncer.refreshEngine()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            pronouncer.shutdown()
        }
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
    var showVoiceSetup by rememberSaveable { mutableStateOf(false) }
    val label = stringResource(labelRes)
    val loadingMessage = stringResource(R.string.pronunciation_loading)
    val installationFailedMessage = stringResource(R.string.pronunciation_installation_failed)
    val failedMessage = stringResource(R.string.pronunciation_failed)
    val onClick: () -> Unit = {
        when (pronouncer.speak(arabic, speechRate, repeatCount)) {
            PronunciationResult.Started -> Unit
            PronunciationResult.Initializing -> {
                Toast.makeText(context, loadingMessage, Toast.LENGTH_LONG).show()
            }
            PronunciationResult.Unavailable -> {
                showVoiceSetup = true
            }
            PronunciationResult.Failed -> {
                Toast.makeText(context, failedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showVoiceSetup) {
        val googleIsDefault = ArabicVoiceInstaller.isGoogleEngine(
            pronouncer.preferredEnginePackage(),
        )
        AlertDialog(
            onDismissRequest = { showVoiceSetup = false },
            title = { Text(stringResource(R.string.arabic_voice_setup_title)) },
            text = {
                Text(
                    stringResource(
                        if (googleIsDefault) {
                            R.string.arabic_voice_install_google
                        } else {
                            R.string.arabic_voice_choose_google
                        },
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showVoiceSetup = false
                        if (!ArabicVoiceInstaller.open(context)) {
                            Toast.makeText(
                                context,
                                installationFailedMessage,
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    },
                ) {
                    Text(stringResource(R.string.open_tts_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoiceSetup = false }) {
                    Text(stringResource(R.string.not_now_plain))
                }
            },
        )
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
