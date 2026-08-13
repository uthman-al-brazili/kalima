package com.kalima.quran.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.WordCitationFormatter
import com.kalima.quran.data.WordRepository

@Composable
fun CitationActions(word: QuranWord) {
    val context = LocalContext.current
    val citation = WordCitationFormatter.format(word, WordRepository.corpusIdentity())
    val chooserTitle = stringResource(R.string.share_citation)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(word.reference, citation))
                Toast.makeText(context, R.string.citation_copied, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.copy_citation))
        }
        TextButton(
            onClick = {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, word.reference)
                    putExtra(Intent.EXTRA_TEXT, citation)
                }
                context.startActivity(Intent.createChooser(share, chooserTitle))
            },
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.share_citation))
        }
    }
}
