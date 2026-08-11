package com.kalima.quran.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Forest = Color(0xFF12372A)
val ForestLight = Color(0xFF2E5A47)
val Gold = Color(0xFFE8C979)
val Cream = Color(0xFFFCFAF4)
val Sand = Color(0xFFF3EBDD)
val Ink = Color(0xFF17201C)
val Muted = Color(0xFF66736D)
val Coral = Color(0xFFC96A54)

private val KalimaColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E7DE),
    onPrimaryContainer = Forest,
    secondary = Gold,
    onSecondary = Forest,
    secondaryContainer = Color(0xFFFFEFBF),
    onSecondaryContainer = Ink,
    tertiary = Coral,
    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = Sand,
    onSurfaceVariant = Muted,
    outline = Color(0xFFBAC3BD),
)

@Composable
fun KalimaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KalimaColors,
        content = content,
    )
}
