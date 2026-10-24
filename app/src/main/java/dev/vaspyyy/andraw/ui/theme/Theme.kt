package dev.vaspyyy.andraw.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AnDrawColors = lightColorScheme(
    primary = Color(0xFF8F587B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF4DCE8),
    onPrimaryContainer = Color(0xFF35172B),
    secondary = Color(0xFF70638F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8E1F7),
    onSecondaryContainer = Color(0xFF261D40),
    tertiary = Color(0xFF577D72),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDCEEE6),
    onTertiaryContainer = Color(0xFF153B32),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF30252D),
    surface = Color(0xFFFFFBF9),
    onSurface = Color(0xFF30252D),
    surfaceVariant = Color(0xFFF2E6EC),
    onSurfaceVariant = Color(0xFF685962),
    outline = Color(0xFFB9A8B1),
    outlineVariant = Color(0xFFE5D8DE),
    error = Color(0xFFB3261E)
)

@Composable
fun AnDrawTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AnDrawColors,
        typography = Typography(),
        content = content
    )
}
