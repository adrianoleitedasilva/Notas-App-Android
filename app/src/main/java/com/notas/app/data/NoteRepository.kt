package com.notas.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Guarda as notas num arquivo JSON no armazenamento interno do app. */
class NoteRepository(private val file: File) {

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val writeLock = Mutex()

    suspend fun load() = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext
        val array = runCatching { JSONArray(file.readText()) }.getOrNull() ?: return@withContext
        _notes.value = (0 until array.length())
            .map { array.getJSONObject(it).toNote() }
            .sortedByDescending { it.updatedAt }
    }

    fun get(id: Long): Note? = _notes.value.firstOrNull { it.id == id }

    suspend fun upsert(note: Note) {
        _notes.update { list ->
            (listOf(note) + list.filterNot { it.id == note.id }).sortedByDescending { it.updatedAt }
        }
        persist()
    }

    suspend fun delete(id: Long) {
        _notes.update { list -> list.filterNot { it.id == id } }
        persist()
    }

    private suspend fun persist() = withContext(Dispatchers.IO) {
        writeLock.withLock {
            val array = JSONArray()
            _notes.value.forEach { array.put(it.toJson()) }
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(array.toString())
            tmp.renameTo(file)
        }
    }

    private fun Note.toJson() = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("body", body)
        .put("updatedAt", updatedAt)

    private fun JSONObject.toNote() = Note(
        id = getLong("id"),
        title = optString("title"),
        body = optString("body"),
        updatedAt = optLong("updatedAt"),
    )
}
