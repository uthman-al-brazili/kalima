package com.kalima.quran.lockscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kalima.quran.R
import com.kalima.quran.data.QuranWord
import com.kalima.quran.ui.ArabicText
import com.kalima.quran.ui.PronunciationButton
import com.kalima.quran.ui.rememberArabicPronouncer
import com.kalima.quran.ui.theme.Cream
import com.kalima.quran.ui.theme.Forest
import com.kalima.quran.ui.theme.Gold
import com.kalima.quran.ui.theme.KalimaTheme
import com.kalima.quran.ui.theme.Muted

@Composable
fun LockScreenStudyScreen(
    word: QuranWord,
    spacedRepetitionEnabled: Boolean,
    initialRememberedSelection: Boolean?,
    onSelect: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit,
) {
    val pronouncer = rememberArabicPronouncer()
    var rememberedSelection by rememberSaveable(word.id) {
        mutableStateOf(initialRememberedSelection)
    }
    KalimaTheme {
        Surface(color = Forest, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("كَلِمَة", color = Gold, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.quick_study), color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.not_now), color = Color.White)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(24.dp))
                    Surface(
                        color = Gold.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(100.dp),
                    ) {
                        Text(
                            word.category,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            color = Gold,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    ArabicText(
                        text = word.arabic,
                        modifier = Modifier.fillMaxWidth(),
                        size = 56,
                        color = Gold,
                    )
                    Text(
                        word.transliteration,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    PronunciationButton(
                        arabic = word.arabic,
                        pronouncer = pronouncer,
                        contentColor = Gold,
                        borderColor = Gold.copy(alpha = 0.65f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        word.meaning,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                            Text(
                                stringResource(R.string.root_value, word.root),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                            Text(
                                word.grammar,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Cream,
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(word.reference, color = Forest, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            ArabicText(
                                word.verseArabic,
                                modifier = Modifier.fillMaxWidth(),
                                size = 25,
                                color = Forest,
                                align = TextAlign.End,
                            )
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = Muted.copy(alpha = 0.2f))
                            Spacer(Modifier.height(12.dp))
                            Text("💡 ${word.insight}", color = Forest, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    OutlinedButton(
                        onClick = onOpenApp,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold),
                    ) {
                        Text(stringResource(R.string.open_app), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.lock_screen_security_note),
                        color = Color.White.copy(alpha = 0.58f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            rememberedSelection = false
                            onSelect(false)
                        },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White,
                            containerColor = if (rememberedSelection == false) {
                                Color.White.copy(alpha = 0.16f)
                            } else {
                                Color.Transparent
                            },
                        ),
                    ) {
                        Text(
                            stringResource(
                                if (spacedRepetitionEnabled) {
                                    R.string.review_later
                                } else {
                                    R.string.review_again_no_schedule
                                },
                            ),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Button(
                        onClick = {
                            rememberedSelection = true
                            onSelect(true)
                        },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (rememberedSelection == true) Gold else Gold.copy(alpha = 0.62f),
                            contentColor = Forest,
                        ),
                    ) {
                        Text(stringResource(R.string.already_learned), fontWeight = FontWeight.Bold)
                    }
                }
                if (rememberedSelection != null) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Cream,
                            contentColor = Forest,
                        ),
                    ) {
                        Text(stringResource(R.string.confirm_and_continue), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
