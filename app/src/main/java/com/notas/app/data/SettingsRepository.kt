package com.notas.app.data

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class FontSize(val scale: Float) {
    SMALL(0.85f),
    NORMAL(1f),
    LARGE(1.15f),
    HUGE(1.3f),
}

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val fontSize: FontSize = FontSize.NORMAL,
    val reminders: Boolean = true,
)

/** Preferências do usuário, guardadas em SharedPreferences. */
class SettingsRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(
        Settings(
            theme = enumOrDefault(prefs.getString(KEY_THEME, null), ThemeMode.SYSTEM),
            fontSize = enumOrDefault(prefs.getString(KEY_FONT_SIZE, null), FontSize.NORMAL),
            reminders = prefs.getBoolean(KEY_REMINDERS, true),
        )
    )
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    fun setTheme(theme: ThemeMode) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
        _settings.update { it.copy(theme = theme) }
        applyNightMode(theme)
    }

    fun setFontSize(size: FontSize) {
        prefs.edit().putString(KEY_FONT_SIZE, size.name).apply()
        _settings.update { it.copy(fontSize = size) }
    }

    fun setReminders(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDERS, enabled).apply()
        _settings.update { it.copy(reminders = enabled) }
    }

    /**
     * No Android 12+ avisa o sistema do tema escolhido, para que a tela de abertura
     * do app já apareça na cor certa (sem piscar no tema errado).
     */
    private fun applyNightMode(theme: ThemeMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val mode = when (theme) {
            ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
            ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
            ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
        }
        context.getSystemService(UiModeManager::class.java)?.setApplicationNightMode(mode)
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_REMINDERS = "reminders"
    }
}
