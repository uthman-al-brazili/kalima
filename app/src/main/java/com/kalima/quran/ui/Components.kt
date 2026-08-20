package com.kalima.quran.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kalima.quran.R
import com.kalima.quran.data.WordStatus

@Composable
fun ArabicText(
    text: String,
    modifier: Modifier = Modifier,
    size: Int = 38,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    align: TextAlign = TextAlign.Center,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        lineHeight = (size * 1.55).sp,
        textAlign = align,
        style = MaterialTheme.typography.headlineLarge.copy(
            textDirection = TextDirection.Rtl,
            localeList = LocaleList(Locale("ar")),
        ),
    )
}

@Composable
fun WordStatusPill(status: WordStatus) {
    val (label, container, content) = when (status) {
        WordStatus.New -> Triple(
            stringResource(R.string.status_new),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WordStatus.Reviewing -> Triple(
            stringResource(R.string.status_reviewing),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        WordStatus.Learned -> Triple(
            stringResource(R.string.status_learned),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        WordStatus.AlreadyKnown -> Triple(
            stringResource(R.string.status_already_known),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(100.dp)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun StatBlock(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun RootAndGrammar(root: String, grammar: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
            shape = RoundedCornerShape(10.dp),
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    stringResource(R.string.root_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(root, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(8.dp))
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
            Text(
                grammar,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
fun LearningLimitEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.learning_limit_reached_title),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.learning_limit_reached_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AllCaughtUpState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.all_caught_up_title),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.all_caught_up_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AllWordsAlreadyKnownState(
    modifier: Modifier = Modifier,
    onOpenExcludedWords: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.all_words_already_known_title),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.all_words_already_known_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onOpenExcludedWords != null) {
            Spacer(Modifier.height(18.dp))
            OutlinedButton(onClick = onOpenExcludedWords) {
                Text(stringResource(R.string.excluded_words))
            }
        }
    }
}

@Composable
fun EmptyCollectionState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.empty_collection),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
