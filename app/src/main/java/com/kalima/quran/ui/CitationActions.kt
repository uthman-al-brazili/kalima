package com.kalima.quran.ui

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
    IconButton(
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
        Icon(
            painter = painterResource(R.drawable.ic_share),
            contentDescription = stringResource(R.string.share_citation),
            modifier = Modifier.size(24.dp),
        )
    }
}
