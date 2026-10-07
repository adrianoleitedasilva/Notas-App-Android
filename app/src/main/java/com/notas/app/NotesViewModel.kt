package com.notas.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notas.app.data.FontSize
import com.notas.app.data.Note
import com.notas.app.data.NoteRepository
import com.notas.app.data.Settings
import com.notas.app.data.SettingsRepository
import com.notas.app.data.ThemeMode
import com.notas.app.reminders.ReminderPlanner
import com.notas.app.reminders.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime

@OptIn(FlowPreview::class)
class NotesViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = NoteRepository(File(app.filesDir, NoteRepository.FILE_NAME))
    private val settingsRepository = SettingsRepository(app)
    private val scheduler = ReminderScheduler(app)

    /** As notas já foram lidas do disco. */
    val loaded: StateFlow<Boolean> = repository.loaded

    val settings: StateFlow<Settings> = settingsRepository.settings

    val query = MutableStateFlow("")

    val notes: StateFlow<List<Note>> = combine(repository.notes, query) { notes, q ->
        if (q.isBlank()) notes
        else notes.filter { it.title.contains(q, ignoreCase = true) || it.body.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalCount: StateFlow<Int> = repository.notes.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private var pendingSave: Job? = null

    init {
        viewModelScope.launch {
            repository.load()
            // Reagenda os lembretes sempre que uma nota ou a opção de lembretes mudar
            combine(repository.notes, settings.map { it.reminders }.distinctUntilChanged()) { notes, enabled -> notes to enabled }
                .debounce(1_000)
                .collect { (notes, enabled) ->
                    withContext(Dispatchers.IO) { scheduler.sync(notes, enabled) }
                }
        }
    }

    fun get(id: Long): Note? = repository.get(id)

    fun setTheme(theme: ThemeMode) = settingsRepository.setTheme(theme)

    fun setFontSize(size: FontSize) = settingsRepository.setFontSize(size)

    fun setReminders(enabled: Boolean) = settingsRepository.setReminders(enabled)

    /** A nota tem alguma data futura que vai gerar lembrete? */
    fun hasUpcomingDate(title: String, body: String): Boolean =
        ReminderPlanner.plan(Note(0, title, body, 0), LocalDateTime.now()).isNotEmpty()

    /** Salva com um pequeno atraso para não escrever no disco a cada tecla. */
    fun save(id: Long, title: String, body: String) {
        pendingSave?.cancel()
        pendingSave = viewModelScope.launch {
            delay(400)
            write(id, title, body)
        }
    }

    /** Chamado ao sair do editor: grava na hora e descarta notas vazias. */
    fun close(id: Long, title: String, body: String) {
        pendingSave?.cancel()
        viewModelScope.launch { write(id, title, body) }
    }

    fun delete(id: Long) {
        pendingSave?.cancel()
        viewModelScope.launch { repository.delete(id) }
    }

    private suspend fun write(id: Long, title: String, body: String) {
        val existing = repository.get(id)
        val note = Note(id, title, body, System.currentTimeMillis())
        when {
            note.isBlank -> if (existing != null) repository.delete(id)
            existing?.title == title && existing.body == body -> Unit
            else -> repository.upsert(note)
        }
    }
}
