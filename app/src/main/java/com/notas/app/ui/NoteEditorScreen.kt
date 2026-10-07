package com.notas.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.notas.app.data.Note
import kotlinx.coroutines.delay

@Composable
fun NoteEditorScreen(
    noteId: Long,
    initial: Note?,
    onChange: (title: String, body: String) -> Unit,
    onClose: (title: String, body: String) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    var title by rememberSaveable(noteId) { mutableStateOf(initial?.title.orEmpty()) }
    var body by rememberSaveable(noteId) { mutableStateOf(initial?.body.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }
    val bodyFocus = remember { FocusRequester() }
    val scroll = rememberScrollState()

    BackHandler { onClose(title, body) }

    // Nota nova: já abre com o cursor no título
    LaunchedEffect(noteId) {
        if (initial == null) titleFocus.requestFocus()
    }
    // Volta o botão "excluir" ao normal se o usuário não confirmar
    LaunchedEffect(confirmDelete) {
        if (confirmDelete) {
            delay(3_000)
            confirmDelete = false
        }
    }

    Box(Modifier.fillMaxSize().paperMargin()) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
        ) {
            // Barra superior: voltar na margem, data e excluir na linha
            NotebookLine(
                modifier = Modifier.paperRules().height(ruleDp(2)),
                margin = { TextAction("←", colors.onBackground, style = type.titleLarge) { onClose(title, body) } },
            ) {
                Text(
                    formatDate(initial?.updatedAt ?: System.currentTimeMillis(), long = true),
                    style = type.labelSmall.onRule(),
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
                TextAction(
                    if (confirmDelete) "confirmar" else "excluir",
                    if (confirmDelete) colors.error else colors.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.BottomEnd),
                ) {
                    if (confirmDelete) onDelete() else confirmDelete = true
                }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .paperRules { scroll.value }
                    // Tocar em qualquer pauta vazia leva o cursor para o texto
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        bodyFocus.requestFocus()
                    }
            ) {
                Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                    PlainField(
                        value = title,
                        onValueChange = { title = it; onChange(it, body) },
                        placeholder = "título",
                        style = type.headlineSmall.copy(fontWeight = FontWeight.Bold).onRule(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(onNext = { bodyFocus.requestFocus() }),
                        modifier = Modifier.afterMargin().height(ruleDp()).focusRequester(titleFocus),
                    )
                    Spacer(Modifier.height(ruleDp()))
                    PlainField(
                        value = body,
                        onValueChange = { body = it; onChange(title, it) },
                        placeholder = "escreva algo…",
                        style = type.bodyLarge.onRule(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier
                            .afterMargin()
                            .heightIn(min = ruleDp(10))
                            .focusRequester(bodyFocus),
                    )
                    Spacer(Modifier.height(ruleDp(3)))
                }
            }
        }
    }
}

@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth()) {
        if (value.isEmpty()) {
            Text(placeholder, style = style, color = colors.onSurfaceVariant.copy(alpha = 0.6f))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = style.copy(color = colors.onBackground),
            cursorBrush = SolidColor(colors.onBackground),
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun TextAction(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelLarge,
    onClick: () -> Unit,
) {
    Text(
        label,
        style = style.onRule(),
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
    )
}
