package com.kalima.quran.ui

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.data.QuranWord

@Composable
fun WordCollectionActions(
    word: QuranWord,
    inCustomList: Boolean,
    alreadyKnown: Boolean,
    onToggleCustomList: (String) -> Unit,
    onToggleAlreadyKnown: (String) -> Unit,
    showAlreadyKnown: Boolean = true,
) {
    val customListDescription = stringResource(
        if (inCustomList) R.string.remove_custom_list else R.string.add_custom_list,
    )
    FilterChip(
        selected = inCustomList,
        onClick = { onToggleCustomList(word.id) },
        label = { Text(stringResource(R.string.custom_list_action)) },
        leadingIcon = { Text(if (inCustomList) "✓" else "+") },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = customListDescription },
    )
    if (!showAlreadyKnown) return
    Spacer(Modifier.height(8.dp))
    val alreadyKnownDescription = stringResource(
        if (alreadyKnown) R.string.restore_to_practice_description
        else R.string.mark_already_known_description,
    )
    FilterChip(
        selected = alreadyKnown,
        onClick = { onToggleAlreadyKnown(word.id) },
        label = {
            Text(
                stringResource(
                    if (alreadyKnown) R.string.restore_to_practice
                    else R.string.mark_already_known,
                ),
            )
        },
        leadingIcon = { Text(if (alreadyKnown) "✓" else "−") },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = alreadyKnownDescription },
    )
}

@Composable
fun EditorialReviewPanel(word: QuranWord) {
    val context = LocalContext.current
    var expanded by rememberSaveable(word.id) { mutableStateOf(false) }
    val chooserTitle = stringResource(R.string.report_card)
    val subject = stringResource(R.string.report_card_subject, word.id)
    val body = stringResource(
        R.string.report_card_body,
        word.id,
        word.reference,
        word.arabic,
    )
    val showDetailsDescription = listOf(
        stringResource(R.string.editorial_compact_title),
        stringResource(R.string.editorial_show_details),
    ).joinToString(". ")
    if (!expanded) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp),
            ) {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_content_review),
                        contentDescription = showDetailsDescription,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
        return
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.editorial_compact_title),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                )
                val detailsDescription = stringResource(R.string.editorial_hide_details)
                IconButton(onClick = { expanded = false }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_expand_more),
                        contentDescription = detailsDescription,
                        modifier = Modifier.rotate(180f),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
            if (expanded) {
                Text(
                    stringResource(R.string.editorial_status),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.editorial_details),
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.editorial_metadata, word.id, word.reference),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(
                    onClick = {
                        val report = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, subject)
                            putExtra(Intent.EXTRA_TEXT, body)
                        }
                        context.startActivity(Intent.createChooser(report, chooserTitle))
                    },
                ) {
                    Text(stringResource(R.string.report_card))
                }
            }
        }
    }
}
