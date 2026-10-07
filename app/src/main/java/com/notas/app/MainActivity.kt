package com.notas.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notas.app.data.ThemeMode
import com.notas.app.ui.NoteEditorScreen
import com.notas.app.ui.NotesListScreen
import com.notas.app.ui.SettingsScreen
import com.notas.app.ui.theme.NotasTheme

private const val LIST = "list"
private const val SETTINGS = "settings"
private const val NOTE = "note:"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: NotesViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            val dark = when (settings.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Ícones da barra de status/navegação no contraste certo para o tema escolhido
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            NotasTheme(darkTheme = dark, fontScale = settings.fontSize.scale) {
                val notes by vm.notes.collectAsStateWithLifecycle()
                val total by vm.totalCount.collectAsStateWithLifecycle()
                val query by vm.query.collectAsStateWithLifecycle()
                var screen by rememberSaveable { mutableStateOf(LIST) }

                AnimatedContent(
                    targetState = screen,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                    label = "screen",
                ) { current ->
                    when {
                        current == SETTINGS -> SettingsScreen(
                            settings = settings,
                            onThemeChange = vm::setTheme,
                            onFontSizeChange = vm::setFontSize,
                            onBack = { screen = LIST },
                        )

                        current.startsWith(NOTE) -> {
                            val id = current.removePrefix(NOTE).toLong()
                            NoteEditorScreen(
                                noteId = id,
                                initial = vm.get(id),
                                onChange = { title, body -> vm.save(id, title, body) },
                                onClose = { title, body -> vm.close(id, title, body); screen = LIST },
                                onDelete = { vm.delete(id); screen = LIST },
                            )
                        }

                        else -> NotesListScreen(
                            notes = notes,
                            totalCount = total,
                            query = query,
                            onQueryChange = { vm.query.value = it },
                            onOpen = { screen = NOTE + it },
                            onCreate = { screen = NOTE + System.currentTimeMillis() },
                            onOpenSettings = { screen = SETTINGS },
                        )
                    }
                }
            }
        }
    }
}
