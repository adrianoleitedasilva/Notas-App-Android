package com.notas.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.notas.app.data.Note
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Mantém os alarmes do AlarmManager iguais aos avisos planejados para as notas.
 * A cada sincronização cancela os alarmes anteriores e agenda os atuais.
 */
class ReminderScheduler(private val context: Context) {

    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)

    @Synchronized
    fun sync(notes: List<Note>, enabled: Boolean) {
        prefs.getStringSet(KEY_CODES, emptySet()).orEmpty().forEach { cancel(it.toInt()) }

        val reminders = if (enabled) ReminderPlanner.plan(notes, LocalDateTime.now()) else emptyList()
        val codes = reminders.map { reminder ->
            val code = requestCode(reminder)
            val triggerAt = reminder.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            // Alarme inexato: não precisa da permissão de alarme exato e pode atrasar alguns minutos
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(code, reminder))
            code.toString()
        }
        prefs.edit().putStringSet(KEY_CODES, codes.toSet()).apply()
    }

    private fun cancel(code: Int) {
        PendingIntent.getBroadcast(
            context, code, baseIntent(code),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )?.let {
            alarms.cancel(it)
            it.cancel()
        }
    }

    private fun pendingIntent(code: Int, reminder: Reminder): PendingIntent {
        val intent = baseIntent(code)
            .putExtra(ReminderReceiver.EXTRA_NOTE_ID, reminder.noteId)
            .putExtra(ReminderReceiver.EXTRA_NOTE_TITLE, reminder.noteTitle)
            .putExtra(ReminderReceiver.EXTRA_LINE, reminder.event.line)
            .putExtra(ReminderReceiver.EXTRA_DATE, reminder.event.date.toEpochDay())
            .putExtra(ReminderReceiver.EXTRA_TIME, reminder.event.time?.toSecondOfDay() ?: -1)
            .putExtra(ReminderReceiver.EXTRA_KIND, reminder.kind.name)
        return PendingIntent.getBroadcast(
            context, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    // O Uri torna cada alarme único para o Android, mesmo com o mesmo receptor
    private fun baseIntent(code: Int) = Intent(context, ReminderReceiver::class.java)
        .setData(Uri.parse("notas://reminder/$code"))

    private fun requestCode(reminder: Reminder): Int =
        "${reminder.noteId}|${reminder.event.date}|${reminder.event.time}|${reminder.kind}".hashCode()

    private companion object {
        const val KEY_CODES = "scheduled_codes"
    }
}
