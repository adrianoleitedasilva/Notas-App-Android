package com.notas.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notas.app.data.Note
import com.notas.app.data.NoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = NoteRepository(File(app.filesDir, "notes.json"))

    val query = MutableStateFlow("")

    val notes: StateFlow<List<Note>> = combine(repository.notes, query) { notes, q ->
        if (q.isBlank()) notes
        else notes.filter { it.title.contains(q, ignoreCase = true) || it.body.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalCount: StateFlow<Int> = repository.notes.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private var pendingSave: Job? = null

    init {
        viewModelScope.launch { repository.load() }
    }

    fun get(id: Long): Note? = repository.get(id)

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
