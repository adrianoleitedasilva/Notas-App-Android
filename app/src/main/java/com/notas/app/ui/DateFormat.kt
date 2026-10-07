package com.notas.app.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ptBr = Locale.forLanguageTag("pt-BR")
private val time = DateTimeFormatter.ofPattern("HH:mm", ptBr)
private val shortDate = DateTimeFormatter.ofPattern("dd MMM", ptBr)
private val shortDateYear = DateTimeFormatter.ofPattern("MM/yy", ptBr)
private val longDate = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", ptBr)

/** "14:32" se for hoje, "07 out" se for este ano, "10/25" se for antes (cabe na margem). */
fun formatDate(millis: Long, long: Boolean = false): String {
    val dateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    if (long) return dateTime.format(longDate).replace(".", "")
    val today = LocalDate.now()
    val formatter = when {
        dateTime.toLocalDate() == today -> time
        dateTime.year == today.year -> shortDate
        else -> shortDateYear
    }
    return dateTime.format(formatter).replace(".", "")
}
