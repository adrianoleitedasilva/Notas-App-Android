package com.notas.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notas.app.ui.NoteEditorScreen
import com.notas.app.ui.NotesListScreen
import com.notas.app.ui.theme.NotasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotasTheme {
                val vm: NotesViewModel = viewModel()
                val notes by vm.notes.collectAsStateWithLifecycle()
                val total by vm.totalCount.collectAsStateWithLifecycle()
                val query by vm.query.collectAsStateWithLifecycle()
                var openId by rememberSaveable { mutableStateOf<Long?>(null) }

                AnimatedContent(
                    targetState = openId,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                    label = "screen",
                ) { id ->
                    if (id == null) {
                        NotesListScreen(
                            notes = notes,
                            totalCount = total,
                            query = query,
                            onQueryChange = { vm.query.value = it },
                            onOpen = { openId = it },
                            onCreate = { openId = System.currentTimeMillis() },
                        )
                    } else {
                        NoteEditorScreen(
                            noteId = id,
                            initial = vm.get(id),
                            onChange = { title, body -> vm.save(id, title, body) },
                            onClose = { title, body -> vm.close(id, title, body); openId = null },
                            onDelete = { vm.delete(id); openId = null },
                        )
                    }
                }
            }
        }
    }
}
