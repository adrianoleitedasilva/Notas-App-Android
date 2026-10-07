package com.notas.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
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
import androidx.core.content.ContextCompat
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

    /** Nota pedida por uma notificação de lembrete, aguardando para ser aberta. */
    private var noteToOpen by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val vm: NotesViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            val loaded by vm.loaded.collectAsStateWithLifecycle()
            val askNotificationPermission = rememberLauncherForActivityResult(RequestPermission()) {}

            // Pede permissão de notificação só quando ela for necessária
            fun ensureNotificationPermission() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    askNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
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

                LaunchedEffect(noteToOpen) {
                    noteToOpen?.let { screen = NOTE + it }
                    noteToOpen = null
                }

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
                            onRemindersChange = { enabled ->
                                vm.setReminders(enabled)
                                if (enabled) ensureNotificationPermission()
                            },
                            onBack = { screen = LIST },
                        )

                        // Só abre o editor com as notas carregadas, senão ele começaria vazio
                        current.startsWith(NOTE) -> if (loaded) {
                            val id = current.removePrefix(NOTE).toLong()
                            NoteEditorScreen(
                                noteId = id,
                                initial = vm.get(id),
                                onChange = { title, body -> vm.save(id, title, body) },
                                onClose = { title, body ->
                                    vm.close(id, title, body)
                                    screen = LIST
                                    if (settings.reminders && vm.hasUpcomingDate(title, body)) ensureNotificationPermission()
                                },
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getLongExtra(EXTRA_OPEN_NOTE, -1)?.takeIf { it >= 0 }?.let { noteToOpen = it }
    }

    companion object {
        const val EXTRA_OPEN_NOTE = "open_note"
    }
}
