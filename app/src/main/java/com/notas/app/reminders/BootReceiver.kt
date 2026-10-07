package com.notas.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notas.app.data.NoteRepository
import com.notas.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * O Android apaga os alarmes ao reiniciar o aparelho e ao atualizar o app.
 * Este receptor agenda tudo de novo nesses dois momentos.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = NoteRepository(File(context.filesDir, NoteRepository.FILE_NAME))
                repository.load()
                val enabled = SettingsRepository(context).settings.value.reminders
                ReminderScheduler(context).sync(repository.notes.value, enabled)
            } finally {
                pending.finish()
            }
        }
    }
}
