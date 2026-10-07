package com.notas.app.reminders

import com.notas.app.data.Note
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class ReminderKind { DAY_BEFORE, SAME_DAY }

/** Um aviso a ser disparado em [at] sobre a data [event] da nota [noteId]. */
data class Reminder(
    val noteId: Long,
    val noteTitle: String,
    val event: DetectedDate,
    val kind: ReminderKind,
    val at: LocalDateTime,
)

/**
 * Decide quando avisar sobre cada data:
 * - na véspera, às 9h;
 * - no dia, 1 hora antes do horário escrito, ou às 8h se não houver horário.
 */
object ReminderPlanner {

    private val DAY_BEFORE_TIME: LocalTime = LocalTime.of(9, 0)
    private val SAME_DAY_TIME: LocalTime = LocalTime.of(8, 0)

    /** Limite de alarmes agendados (o Android aceita no máximo 500 por app). */
    const val MAX_REMINDERS = 100

    fun plan(notes: List<Note>, now: LocalDateTime): List<Reminder> =
        notes.asSequence()
            .flatMap { note -> plan(note, now) }
            .sortedBy { it.at }
            .take(MAX_REMINDERS)
            .toList()

    fun plan(note: Note, now: LocalDateTime): List<Reminder> {
        val title = note.title.ifBlank { note.body.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty() }
        return DateDetector.detect(note.title + "\n" + note.body, now.toLocalDate()).flatMap { event ->
            val dayBefore = event.date.minusDays(1).atTime(DAY_BEFORE_TIME)
            val sameDay = event.time?.let { event.date.atTime(it).minusHours(1) }
                ?: event.date.atTime(SAME_DAY_TIME)
            listOf(
                Reminder(note.id, title, event, ReminderKind.DAY_BEFORE, dayBefore),
                Reminder(note.id, title, event, ReminderKind.SAME_DAY, sameDay),
            ).filter { it.at.isAfter(now) }
        }
    }

    /** Próxima data (hoje ou depois) citada na nota, para mostrar na lista. */
    fun nextEvent(note: Note, today: LocalDate): DetectedDate? =
        DateDetector.detect(note.title + "\n" + note.body, today)
            .filter { !it.date.isBefore(today) }
            .minByOrNull { it.date }
}
