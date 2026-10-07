package com.notas.app.reminders

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.notas.app.MainActivity
import com.notas.app.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Recebe o alarme agendado e mostra a notificação do lembrete. */
class ReminderReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission") // checado por areNotificationsEnabled()
    override fun onReceive(context: Context, intent: Intent) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1)
        val noteTitle = intent.getStringExtra(EXTRA_NOTE_TITLE).orEmpty()
        val line = intent.getStringExtra(EXTRA_LINE).orEmpty()
        val date = LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_DATE, 0))
        val time = intent.getIntExtra(EXTRA_TIME, -1).takeIf { it >= 0 }?.let { LocalTime.ofSecondOfDay(it.toLong()) }
        val kind = ReminderKind.valueOf(intent.getStringExtra(EXTRA_KIND) ?: ReminderKind.SAME_DAY.name)

        createChannel(context)

        val open = PendingIntent.getActivity(
            context,
            noteId.hashCode(),
            Intent(context, MainActivity::class.java)
                .setData(Uri.parse("notas://note/$noteId"))
                .putExtra(MainActivity.EXTRA_OPEN_NOTE, noteId)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(headline(kind, date, time))
            .setContentText(noteTitle.ifBlank { line })
            .setStyle(NotificationCompat.BigTextStyle().bigText(if (noteTitle == line) line else "$noteTitle\n$line"))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(intent.data.toString().hashCode(), notification)
    }

    /** "amanhã · 15 out às 14:00", "hoje às 14:00", "hoje · 15 out". */
    private fun headline(kind: ReminderKind, date: LocalDate, time: LocalTime?): String {
        val day = if (kind == ReminderKind.DAY_BEFORE) "amanhã" else "hoje"
        val at = time?.let { " às " + it.format(TIME) }.orEmpty()
        return if (kind == ReminderKind.SAME_DAY && time != null) "$day$at"
        else "$day · ${date.format(DATE).replace(".", "")}$at"
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Lembretes", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Avisos sobre datas escritas nas notas" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "lembretes"
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_NOTE_TITLE = "note_title"
        const val EXTRA_LINE = "line"
        const val EXTRA_DATE = "date"
        const val EXTRA_TIME = "time"
        const val EXTRA_KIND = "kind"

        private val ptBr = Locale.forLanguageTag("pt-BR")
        private val DATE = DateTimeFormatter.ofPattern("dd MMM", ptBr)
        private val TIME = DateTimeFormatter.ofPattern("HH:mm", ptBr)
    }
}
