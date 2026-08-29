package com.kalima.quran.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kalima.quran.R
import com.kalima.quran.data.QuranPageToken
import com.kalima.quran.data.QuranWord
import com.kalima.quran.recitation.RecitationRecognizerState
import com.kalima.quran.recitation.RecitationWordMatcher
import com.kalima.quran.recitation.TilawaRecitationRecognizer

private enum class ReadAndUnderstandState {
    Ready,
    Loading,
    Listening,
    Processing,
    Result,
    NoMatch,
    PermissionDenied,
    Failed,
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ReadAndUnderstandSheet(
    verseTokens: List<QuranPageToken>,
    verseWords: List<QuranWord?>,
    reference: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val recognizer = remember(context.applicationContext) {
        TilawaRecitationRecognizer(context.applicationContext)
    }
    val pronouncer = rememberArabicPronouncer()
    var state by remember(reference) { mutableStateOf(ReadAndUnderstandState.Ready) }
    var matchedWordIndexes by remember(reference) { mutableStateOf(emptySet<Int>()) }
    var transcript by remember(reference) { mutableStateOf("") }
    var attemptFinished by remember(reference) { mutableStateOf(false) }
    var meaningRevealed by remember(reference) { mutableStateOf(false) }
    var hasMicrophonePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val expectedWords = remember(verseTokens) { verseTokens.map(QuranPageToken::arabic) }
    val verseMeaning = remember(verseWords) {
        buildAyahMeaning(verseWords)
    }
    val vocabulary = remember(verseWords) {
        verseWords.filterNotNull()
            .filter { it.meaning.isNotBlank() }
            .distinctBy(QuranWord::id)
            .take(MAX_VOCABULARY_WORDS)
    }
    val verseAudioWord = remember(verseWords) {
        verseWords.firstOrNull { word -> word?.audioLocation != null }
    }

    fun startRecognition() {
        pronouncer.stop()
        matchedWordIndexes = emptySet()
        transcript = ""
        attemptFinished = false
        meaningRevealed = false
        state = ReadAndUnderstandState.Listening
        recognizer.start(
            onState = { recognitionState ->
                state = when (recognitionState) {
                    RecitationRecognizerState.Loading -> ReadAndUnderstandState.Loading
                    RecitationRecognizerState.Listening -> ReadAndUnderstandState.Listening
                    RecitationRecognizerState.Processing -> ReadAndUnderstandState.Processing
                }
            },
            onTranscript = { recognizedText, final ->
                transcript = recognizedText
                val match = RecitationWordMatcher.evaluate(expectedWords, recognizedText)
                matchedWordIndexes = match.matchedWordIndexes
                if (final) {
                    attemptFinished = true
                    state = if (match.isComplete) {
                        ReadAndUnderstandState.Result
                    } else {
                        ReadAndUnderstandState.NoMatch
                    }
                } else if (match.isComplete) {
                    recognizer.stop()
                }
            },
            onError = {
                attemptFinished = true
                state = ReadAndUnderstandState.Failed
            },
        )
    }

    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasMicrophonePermission = granted
        if (granted) {
            startRecognition()
        } else {
            state = ReadAndUnderstandState.PermissionDenied
            attemptFinished = true
        }
    }

    DisposableEffect(recognizer) {
        onDispose(recognizer::destroy)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        scrimColor = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                stringResource(R.string.read_and_understand_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.read_and_understand_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                reference,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Start),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    verseTokens.forEachIndexed { index, token ->
                        Surface(
                            color = if (index in matchedWordIndexes) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            },
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                token.arabic,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                color = if (index in matchedWordIndexes) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            if (verseAudioWord != null) {
                VersePronunciationButton(
                    word = verseAudioWord,
                    pronouncer = pronouncer,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state != ReadAndUnderstandState.Loading &&
                        state != ReadAndUnderstandState.Listening &&
                        state != ReadAndUnderstandState.Processing,
                )
                Spacer(Modifier.height(10.dp))
            }
            RecitationStatus(
                state = state,
                matchedWordCount = matchedWordIndexes.size,
                totalWordCount = verseTokens.size,
                transcript = transcript,
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    when (state) {
                        ReadAndUnderstandState.Listening -> recognizer.stop()
                        ReadAndUnderstandState.Loading,
                        ReadAndUnderstandState.Processing,
                        -> Unit
                        else -> if (hasMicrophonePermission) {
                            startRecognition()
                        } else {
                            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                enabled = state != ReadAndUnderstandState.Loading &&
                    state != ReadAndUnderstandState.Processing,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    stringResource(
                        when (state) {
                            ReadAndUnderstandState.Listening -> R.string.finish_recitation
                            ReadAndUnderstandState.Ready -> R.string.start_recitation
                            else -> R.string.try_recitation_again
                        },
                    ),
                )
            }

            if (attemptFinished && state == ReadAndUnderstandState.Result) {
                Spacer(Modifier.height(18.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            stringResource(R.string.recall_ayah_meaning),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(R.string.recall_ayah_meaning_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        OutlinedButton(
                            onClick = { meaningRevealed = !meaningRevealed },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) {
                            Text(
                                stringResource(
                                    if (meaningRevealed) {
                                        R.string.hide_meaning
                                    } else {
                                        R.string.reveal_meaning
                                    },
                                ),
                            )
                        }
                    }
                }
            }

            if (meaningRevealed) {
                Spacer(Modifier.height(14.dp))
                Text(
                    verseMeaning.ifBlank {
                        stringResource(R.string.ayah_meaning_not_indexed)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (vocabulary.isNotEmpty()) {
                    Text(
                        stringResource(R.string.key_vocabulary),
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    vocabulary.forEach { word ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                word.arabic,
                                modifier = Modifier.weight(0.42f),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.End,
                            )
                            Text(
                                word.meaning,
                                modifier = Modifier.weight(0.58f).padding(start = 12.dp),
                            )
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.recitation_tracking_disclaimer),
                modifier = Modifier.padding(top = 18.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun RecitationStatus(
    state: ReadAndUnderstandState,
    matchedWordCount: Int,
    totalWordCount: Int,
    transcript: String,
) {
    if (state == ReadAndUnderstandState.Processing) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.padding(end = 10.dp))
            Text(stringResource(R.string.recitation_processing))
        }
        return
    }
    val status = when (state) {
        ReadAndUnderstandState.Ready -> R.string.recitation_ready
        ReadAndUnderstandState.Loading -> R.string.recitation_model_loading
        ReadAndUnderstandState.Listening -> R.string.recitation_listening
        ReadAndUnderstandState.Result -> R.string.recitation_words_heard
        ReadAndUnderstandState.NoMatch -> R.string.recitation_no_match
        ReadAndUnderstandState.PermissionDenied -> R.string.recitation_permission_denied
        ReadAndUnderstandState.Failed -> R.string.recitation_failed
        ReadAndUnderstandState.Processing -> error("Handled above")
    }
    Text(
        if (state == ReadAndUnderstandState.Result) {
            stringResource(status, matchedWordCount, totalWordCount)
        } else {
            stringResource(status)
        },
        color = if (state == ReadAndUnderstandState.Failed) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
    if (transcript.isNotBlank()) {
        Text(
            stringResource(R.string.recitation_heard, transcript),
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private const val MAX_VOCABULARY_WORDS = 8

internal fun buildAyahMeaning(verseWords: List<QuranWord?>): String = verseWords
    .mapNotNull { word -> word?.meaning?.trim()?.takeIf(String::isNotEmpty) }
    .joinToString(" ")
