package com.notas.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Cores próprias da folha de caderno (pautas e margem). */
@Immutable
data class NotebookColors(val rule: Color, val margin: Color)

val LocalNotebookColors = staticCompositionLocalOf { NotebookColors(RuleLight, MarginLight) }

private val LightColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    onSurfaceVariant = InkMuted,
    outlineVariant = RuleLight,
    error = Danger,
)

private val DarkColorScheme = darkColorScheme(
    primary = Chalk,
    onPrimary = Night,
    background = Night,
    onBackground = Chalk,
    surface = Night,
    onSurface = Chalk,
    onSurfaceVariant = ChalkMuted,
    outlineVariant = RuleDark,
    error = Danger,
)

@Composable
fun NotasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val notebook = if (darkTheme) NotebookColors(RuleDark, MarginDark) else NotebookColors(RuleLight, MarginLight)
    CompositionLocalProvider(LocalNotebookColors provides notebook) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content
        )
    }
}
