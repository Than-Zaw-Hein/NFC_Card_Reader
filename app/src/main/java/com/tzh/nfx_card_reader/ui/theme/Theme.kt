package com.tzh.nfx_card_reader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NxpDarkColorScheme = darkColorScheme(
    primary = NxpCyan,
    onPrimary = NxpBlack,
    primaryContainer = NxpCyanDark,
    onPrimaryContainer = NxpCyanBright,
    secondary = NxpCyanBright,
    onSecondary = NxpBlack,
    tertiary = NxpOrange,
    onTertiary = NxpBlack,
    background = NxpBlack,
    onBackground = NxpWhite,
    surface = NxpDarkSlate,
    onSurface = NxpWhite,
    surfaceVariant = NxpCardBackground,
    onSurfaceVariant = NxpMutedGray,
    outline = NxpMutedGray,
    outlineVariant = NxpDivider,
    error = NxpRed,
    onError = NxpWhite
)

@Composable
fun NFX_Card_ReaderTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NxpDarkColorScheme,
        typography = Typography,
        content = content
    )
}
