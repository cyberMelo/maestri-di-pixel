package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ScuolaRetroColorScheme = lightColorScheme(
    primary = DeepBlueAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF0B2F52),
    secondary = WarmAmber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = EmeraldSuccess,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    background = CreamBackground,
    onBackground = InkPrimary,
    surface = CreamBackground,
    onSurface = InkPrimary,
    surfaceVariant = CreamSurface,
    onSurfaceVariant = InkSecondary,
    outline = DarkNavyFrame,
    outlineVariant = Color(0xFFCBD5E1),
    error = WarmAmber,
    onError = Color.White
)

@Composable
fun ScuolaRetroTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ScuolaRetroColorScheme,
        typography = Typography,
        content = content
    )
}
