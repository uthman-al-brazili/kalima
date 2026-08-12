package com.kalima.quran.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.data.QuranWord

@Composable
fun WordCollectionActions(
    word: QuranWord,
    favorite: Boolean,
    inCustomList: Boolean,
    onToggleFavorite: (String) -> Unit,
    onToggleCustomList: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        TextButton(onClick = { onToggleFavorite(word.id) }) {
            Text(
                (if (favorite) "★ " else "☆ ") + stringResource(
                    if (favorite) R.string.remove_favorite else R.string.add_favorite,
                ),
            )
        }
        TextButton(onClick = { onToggleCustomList(word.id) }) {
            Text(
                (if (inCustomList) "✓ " else "+ ") + stringResource(
                    if (inCustomList) R.string.remove_custom_list else R.string.add_custom_list,
                ),
            )
        }
    }
}

@Composable
fun EditorialReviewPanel(word: QuranWord) {
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.report_card)
    val subject = stringResource(R.string.report_card_subject, word.id)
    val body = stringResource(
        R.string.report_card_body,
        word.id,
        word.reference,
        word.arabic,
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                stringResource(R.string.editorial_status),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                stringResource(R.string.editorial_details),
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodySmall,
            )
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
