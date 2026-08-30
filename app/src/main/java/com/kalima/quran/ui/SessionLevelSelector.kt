package com.kalima.quran.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.data.SessionLevel

@Composable
internal fun SessionLevelSelector(
    selected: SessionLevel,
    onSelected: (SessionLevel) -> Unit,
    modifier: Modifier = Modifier,
    showDescription: Boolean = true,
    guided: Boolean = false,
) {
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SessionLevel.entries.forEach { level ->
                val name = stringResource(sessionLevelNameRes(level))
                val description = stringResource(sessionLevelDescriptionRes(level, guided))
                FilterChip(
                    selected = selected == level,
                    onClick = { onSelected(level) },
                    label = { Text(name, maxLines = 1) },
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = "$name. $description"
                        },
                )
            }
        }
        if (showDescription) {
            Text(
                stringResource(sessionLevelDescriptionRes(selected, guided)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@StringRes
internal fun sessionLevelNameRes(level: SessionLevel): Int = when (level) {
    SessionLevel.Quick -> R.string.session_level_quick
    SessionLevel.Steady -> R.string.session_level_steady
    SessionLevel.Deep -> R.string.session_level_deep
}

@StringRes
private fun sessionLevelDescriptionRes(level: SessionLevel, guided: Boolean): Int =
    if (guided) when (level) {
        SessionLevel.Quick -> R.string.session_level_quick_guided_description
        SessionLevel.Steady -> R.string.session_level_steady_guided_description
        SessionLevel.Deep -> R.string.session_level_deep_guided_description
    } else when (level) {
        SessionLevel.Quick -> R.string.session_level_quick_description
        SessionLevel.Steady -> R.string.session_level_steady_description
        SessionLevel.Deep -> R.string.session_level_deep_description
    }
