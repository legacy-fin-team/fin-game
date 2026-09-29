package com.legacy.fingame.game.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * Сохраняет и загружает [GameSettings] через [SharedPreferences].
 *
 * Каждый вызов [save] немедленно записывает все поля в файл настроек;
 * [load] восстанавливает их (или возвращает значения по умолчанию, если файла ещё нет).
 */
class GameSettingsRepository(private val prefs: SharedPreferences) {

    /**
     * Хранит настройки в собственном файле приложения [PREFS_NAME].
     *
     * @param context любой контекст приложения.
     */
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    companion object {
        private const val PREFS_NAME = "fin_game_settings"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_MUSIC = "music_enabled"
        private const val KEY_SOUND_VOLUME = "sound_volume"
        private const val KEY_MUSIC_VOLUME = "music_volume"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_ANIMATIONS = "animations_enabled"
    }

    /**
     * Загружает настройки. Если файл ещё не существует — возвращает значения по умолчанию.
     */
    fun load(): GameSettings {
        val defaults = GameSettings()

        val soundVol = when {
            prefs.contains(KEY_SOUND_VOLUME) -> prefs.getInt(KEY_SOUND_VOLUME, defaults.soundVolume)
            prefs.contains(KEY_SOUND) -> if (prefs.getBoolean(KEY_SOUND, true)) 100 else 0
            else -> defaults.soundVolume
        }

        val musicVol = when {
            prefs.contains(KEY_MUSIC_VOLUME) -> prefs.getInt(KEY_MUSIC_VOLUME, defaults.musicVolume)
            prefs.contains(KEY_MUSIC) -> if (prefs.getBoolean(KEY_MUSIC, true)) 100 else 0
            else -> defaults.musicVolume
        }

        val themeMode = prefs.getString(KEY_THEME, defaults.themeMode.name)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
            ?: defaults.themeMode

        // Сохранения, сделанные до появления переключателя, ключа не знают: анимации в них включены,
        // как и были.
        val animationsEnabled = prefs.getBoolean(KEY_ANIMATIONS, defaults.animationsEnabled)

        return GameSettings(
            soundVolume = soundVol,
            musicVolume = musicVol,
            themeMode = themeMode,
            animationsEnabled = animationsEnabled
        )
    }

    /**
     * Сохраняет настройки синхронно (apply — асинхронно на диск, мгновенно в памяти).
     */
    fun save(settings: GameSettings) {
        prefs.edit()
            .putInt(KEY_SOUND_VOLUME, settings.soundVolume)
            .putInt(KEY_MUSIC_VOLUME, settings.musicVolume)
            .putBoolean(KEY_SOUND, settings.soundEnabled)
            .putBoolean(KEY_MUSIC, settings.musicEnabled)
            .putString(KEY_THEME, settings.themeMode.name)
            .putBoolean(KEY_ANIMATIONS, settings.animationsEnabled)
            .apply()
    }
}
