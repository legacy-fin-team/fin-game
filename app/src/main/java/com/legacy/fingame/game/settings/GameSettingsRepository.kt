package com.legacy.fingame.game.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * Сохраняет и загружает [GameSettings] через [SharedPreferences].
 *
 * Каждый вызов [save] немедленно записывает все поля в файл настроек;
 * [load] восстанавливает их (или возвращает значения по умолчанию, если файла ещё нет).
 */
class GameSettingsRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "fin_game_settings"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_MUSIC = "music_enabled"
        private const val KEY_THEME = "theme_mode"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Загружает настройки. Если файл ещё не существует — возвращает значения по умолчанию.
     */
    fun load(): GameSettings {
        val defaults = GameSettings()
        return GameSettings(
            soundEnabled = prefs.getBoolean(KEY_SOUND, defaults.soundEnabled),
            musicEnabled = prefs.getBoolean(KEY_MUSIC, defaults.musicEnabled),
            themeMode = prefs.getString(KEY_THEME, defaults.themeMode.name)
                ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                ?: defaults.themeMode
        )
    }

    /**
     * Сохраняет настройки синхронно (apply — асинхронно на диск, мгновенно в памяти).
     */
    fun save(settings: GameSettings) {
        prefs.edit()
            .putBoolean(KEY_SOUND, settings.soundEnabled)
            .putBoolean(KEY_MUSIC, settings.musicEnabled)
            .putString(KEY_THEME, settings.themeMode.name)
            .apply()
    }
}
