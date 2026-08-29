package com.kalima.quran.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import java.util.Date
import kotlinx.coroutines.delay

@Composable
fun ArabicIndicClock(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = color.copy(alpha = 0.12f),
    style: TextStyle = MaterialTheme.typography.titleMedium,
) {
    val context = LocalContext.current
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            currentTimeMillis = now
            delay(MILLIS_PER_MINUTE - (now % MILLIS_PER_MINUTE))
        }
    }

    val deviceTime = remember(currentTimeMillis) {
        DateFormat.getTimeFormat(context).format(Date(currentTimeMillis))
    }
    val accessibilityLabel = stringResource(R.string.current_time, deviceTime)

    Surface(
        modifier = modifier.semantics { contentDescription = accessibilityLabel },
        color = containerColor,
        contentColor = color,
        shape = RoundedCornerShape(100.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_clock),
                contentDescription = null,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = deviceTime.toArabicIndicDigits(),
                style = style,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

internal fun String.toArabicIndicDigits(): String = map { character ->
    if (character in '0'..'9') ARABIC_INDIC_DIGITS[character - '0'] else character
}.joinToString("")

private const val ARABIC_INDIC_DIGITS = "\u0660\u0661\u0662\u0663\u0664\u0665\u0666\u0667\u0668\u0669"
private const val MILLIS_PER_MINUTE = 60_000L
