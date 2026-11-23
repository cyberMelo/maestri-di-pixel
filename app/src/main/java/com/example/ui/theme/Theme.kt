package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalDarkTheme = staticCompositionLocalOf { true }

private val LightRetroColorScheme = lightColorScheme(
    primary = DeepBlueAccent,
    onPrimary = IceWhiteText,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF0B2F52),
    secondary = WarmAmber,
    onSecondary = IceWhiteText,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = EmeraldSuccess,
    onTertiary = IceWhiteText,
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
    onError = IceWhiteText
)

private val DarkRetroColorScheme = darkColorScheme(
    primary = SkyHighlight,
    onPrimary = HudNightBg,
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = IceWhiteText,
    secondary = StarYellow,
    onSecondary = HudNightBg,
    secondaryContainer = Color(0xFF422C08),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = EmeraldSuccess,
    onTertiary = IceWhiteText,
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = HudNightBg,
    onBackground = IceWhiteText,
    surface = DarkCardBg,
    onSurface = IceWhiteText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = SlateMutedText,
    outline = DarkBorderSlate,
    outlineVariant = DarkBorderSlate,
    error = WarmAmber,
    onError = IceWhiteText
)

@Composable
fun ScuolaRetroTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkRetroColorScheme else LightRetroColorScheme,
            typography = Typography,
            content = content
        )
    }
}

/** Colore delle parole chiave (**grassetto**) nelle domande: blu scuro sul chiaro, azzurro sullo scuro. */
@Composable
fun keywordHighlightColor(): Color =
    if (LocalDarkTheme.current) Color(0xFF7DD3FC) else com.example.core.GameBalance.KEYWORD_HIGHLIGHT_COLOR
