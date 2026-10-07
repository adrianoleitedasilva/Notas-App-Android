package com.notas.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notas.app.BuildConfig
import com.notas.app.data.FontSize
import com.notas.app.data.Settings
import com.notas.app.data.ThemeMode

@Composable
fun SettingsScreen(
    settings: Settings,
    onThemeChange: (ThemeMode) -> Unit,
    onFontSizeChange: (FontSize) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val scroll = rememberScrollState()

    BackHandler(onBack = onBack)

    Box(Modifier.fillMaxSize().paperMargin()) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            NotebookLine(
                modifier = Modifier.paperRules(),
                margin = {
                    Text(
                        "←",
                        style = type.titleLarge.onRule(),
                        color = colors.onBackground,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onBack)
                            .padding(horizontal = 8.dp),
                    )
                },
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .paperRules { scroll.value }
                    .verticalScroll(scroll)
            ) {
                NotebookLine {
                    Text("opções", style = type.headlineSmall.onRule(), fontWeight = FontWeight.Bold, color = colors.onBackground)
                }
                NotebookLine()

                SectionTitle("tema")
                ThemeMode.entries.forEach { mode ->
                    OptionLine(
                        label = mode.label,
                        hint = mode.hint,
                        selected = settings.theme == mode,
                        onClick = { onThemeChange(mode) },
                    )
                }
                NotebookLine()

                SectionTitle("tamanho da fonte")
                FontSize.entries.forEach { size ->
                    OptionLine(
                        label = size.label,
                        hint = "${(size.scale * 100).toInt()}%",
                        selected = settings.fontSize == size,
                        onClick = { onFontSizeChange(size) },
                    )
                }
                NotebookLine()
                NotebookLine {
                    Text(
                        "o rato roeu a roupa do rei de roma",
                        style = type.bodyLarge.onRule(),
                        color = colors.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                NotebookLine()
                NotebookLine()
                NotebookLine {
                    Text("notas v${BuildConfig.VERSION_NAME}", style = type.labelSmall.onRule(), color = colors.onSurfaceVariant)
                }
                NotebookLine()
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    NotebookLine(margin = {
        Text("#", style = MaterialTheme.typography.labelMedium.onRule(), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }) {
        Text(text, style = MaterialTheme.typography.labelMedium.onRule(), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Uma opção marcável: `[x] claro`, ocupando uma pauta. */
@Composable
private fun OptionLine(label: String, hint: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    NotebookLine(Modifier.clickable(onClick = onClick)) {
        Text(
            (if (selected) "[x] " else "[ ] ") + label,
            style = type.bodyLarge.onRule(),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) colors.onBackground else colors.onSurfaceVariant,
        )
        Text(
            hint,
            style = type.labelSmall.onRule(),
            color = colors.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

private val ThemeMode.label
    get() = when (this) {
        ThemeMode.SYSTEM -> "sistema"
        ThemeMode.LIGHT -> "claro"
        ThemeMode.DARK -> "escuro"
    }

private val ThemeMode.hint
    get() = when (this) {
        ThemeMode.SYSTEM -> "segue o aparelho"
        ThemeMode.LIGHT -> "papel"
        ThemeMode.DARK -> "capa preta"
    }

private val FontSize.label
    get() = when (this) {
        FontSize.SMALL -> "pequena"
        FontSize.NORMAL -> "normal"
        FontSize.LARGE -> "grande"
        FontSize.HUGE -> "enorme"
    }
