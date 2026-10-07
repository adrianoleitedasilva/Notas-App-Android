package com.notas.app.data

data class Note(
    val id: Long,
    val title: String,
    val body: String,
    val updatedAt: Long,
) {
    val isBlank: Boolean get() = title.isBlank() && body.isBlank()
}
