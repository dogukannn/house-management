package com.dunettrpg.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DesertSand,
    secondary = SpiceOrange,
    tertiary = AtredesGreen,
    background = DeepBlue,
    surface = Midnight,
    onPrimary = Midnight,
    onSecondary = Midnight,
    onTertiary = SandWhite,
    onBackground = SandWhite,
    onSurface = SandWhite,
    error = HarkonnenRed
)

@Composable
fun DuneTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
