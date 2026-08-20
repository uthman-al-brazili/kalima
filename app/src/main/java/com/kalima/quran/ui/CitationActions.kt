package com.kalima.quran.ui

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kalima.quran.R
import com.kalima.quran.data.QuranWord
import com.kalima.quran.data.WordCitationFormatter
import com.kalima.quran.data.WordRepository

@Composable
fun CitationActions(word: QuranWord) {
    val context = LocalContext.current
    val citation = WordCitationFormatter.format(word, WordRepository.corpusIdentity())
    val chooserTitle = stringResource(R.string.share_citation)
    TextButton(
        onClick = {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, word.reference)
                putExtra(Intent.EXTRA_TEXT, citation)
            }
            context.startActivity(Intent.createChooser(share, chooserTitle))
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.share_citation))
    }
}
