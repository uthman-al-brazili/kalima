package com.kalima.quran.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import com.kalima.quran.data.AppThemeMode

val Forest = Color(0xFF12372A)
val ForestLight = Color(0xFF2E5A47)
val Gold = Color(0xFFE8C979)
val Cream = Color(0xFFFCFAF4)
val Sand = Color(0xFFF3EBDD)
val Ink = Color(0xFF17201C)
val Muted = Color(0xFF66736D)
val Coral = Color(0xFFC96A54)

private val KalimaLightColors = lightColorScheme(
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

private val KalimaDarkColors = darkColorScheme(
    primary = Color(0xFF9ED4B8),
    onPrimary = Color(0xFF073824),
    primaryContainer = Color(0xFF1D503A),
    onPrimaryContainer = Color(0xFFBAF1D2),
    secondary = Gold,
    onSecondary = Color(0xFF3D2F00),
    secondaryContainer = Color(0xFF594600),
    onSecondaryContainer = Color(0xFFFFE28A),
    tertiary = Color(0xFFFFB4A3),
    background = Color(0xFF101713),
    onBackground = Color(0xFFE0E5E0),
    surface = Color(0xFF171F1A),
    onSurface = Color(0xFFE0E5E0),
    surfaceVariant = Color(0xFF3F4943),
    onSurfaceVariant = Color(0xFFBFC9C1),
    outline = Color(0xFF89938C),
)

@Composable
fun KalimaTheme(
    themeMode: AppThemeMode = AppThemeMode.Auto,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.Auto -> isSystemInDarkTheme()
        AppThemeMode.Light -> false
        AppThemeMode.Dark -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) KalimaDarkColors else KalimaLightColors,
        content = content,
    )
}
