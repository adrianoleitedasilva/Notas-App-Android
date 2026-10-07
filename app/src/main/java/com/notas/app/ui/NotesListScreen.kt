package com.notas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notas.app.data.Note
import com.notas.app.reminders.ReminderPlanner
import java.time.LocalDate

@Composable
fun NotesListScreen(
    notes: List<Note>,
    totalCount: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpen: (Long) -> Unit,
    onCreate: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val listState = rememberLazyListState()

    Box(Modifier.fillMaxSize().paperMargin()) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // Cabeçalho, busca: cada linha ocupa uma pauta
            Column(Modifier.paperRules()) {
                NotebookLine()
                NotebookLine {
                    Text(
                        "notas",
                        style = type.headlineSmall.onRule(),
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground,
                    )
                    Text(
                        "opções",
                        style = type.labelMedium.onRule(),
                        color = colors.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenSettings)
                            .padding(horizontal = 8.dp),
                    )
                }
                NotebookLine {
                    Text(countLabel(totalCount), style = type.labelMedium.onRule(), color = colors.onSurfaceVariant)
                }
                NotebookLine(
                    margin = { Text("/", style = type.bodyMedium.onRule(), color = colors.onSurfaceVariant) },
                ) {
                    if (query.isEmpty()) {
                        Text("buscar", style = type.bodyMedium.onRule(), color = colors.onSurfaceVariant)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = type.bodyMedium.onRule().copy(color = colors.onBackground),
                        cursorBrush = SolidColor(colors.onBackground),
                        modifier = Modifier.fillMaxWidth().padding(end = 28.dp),
                    )
                    if (query.isNotEmpty()) {
                        Text(
                            "×",
                            style = type.bodyLarge.onRule(),
                            color = colors.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onQueryChange("") }
                                .padding(horizontal = 6.dp),
                        )
                    }
                }
                NotebookLine()
            }

            // Notas: uma pauta para cada título
            Box(
                Modifier
                    .fillMaxSize()
                    .paperRules { listState.firstVisibleItemScrollOffset }
            ) {
                if (notes.isEmpty()) {
                    NotebookLine {
                        Text(
                            if (query.isEmpty()) "folha em branco. toque em + para escrever" else "nada encontrado",
                            style = type.bodyMedium.onRule(),
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(bottom = ruleDp(4)),
                    ) {
                        items(notes, key = { it.id }) { note ->
                            NoteRow(note, onClick = { onOpen(note.id) })
                        }
                    }
                }
            }
        }

        // Botão de nova nota
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .systemBarsPadding()
                .padding(24.dp)
                .size(56.dp)
                .shadow(6.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(colors.primary)
                .clickable(onClick = onCreate),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = colors.onPrimary, fontSize = 26.sp, style = type.titleLarge)
        }
    }
}

@Composable
private fun NoteRow(note: Note, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    // Sem título: usa a primeira linha do texto
    val title = note.title.ifBlank { note.body.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty() }
    val upcoming = remember(note) { ReminderPlanner.nextEvent(note, LocalDate.now()) }

    NotebookLine(
        modifier = Modifier.clickable(onClick = onClick),
        margin = {
            Text(formatDate(note.updatedAt), style = type.labelSmall.onRule(), color = colors.onSurfaceVariant)
        },
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                title.ifBlank { "sem título" },
                style = type.bodyLarge.copy(fontSize = type.bodyLarge.fontSize * 1.2f).onRule(),
                fontWeight = FontWeight.Medium,
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            // Data próxima reconhecida na nota (vai gerar lembrete)
            if (upcoming != null) {
                Text(
                    "  → " + formatDay(upcoming.date),
                    style = type.labelSmall.onRule(),
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun countLabel(count: Int) = when (count) {
    0 -> "nenhuma nota"
    1 -> "1 nota"
    else -> "$count notas"
}
