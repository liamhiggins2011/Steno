package com.hirephaze.steno.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF2D6A6A)
private val TealLight = Color(0xFF4F9A9A)
private val Coral = Color(0xFFE2725B)

private val LightColors = lightColorScheme(
    primary = Teal,
    secondary = Coral,
    background = Color(0xFFFAF9FB),
    surface = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    secondary = Coral,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
)

@Composable
fun StenoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
