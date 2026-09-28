package com.legacy.fingame.game.settings

/**
 * Настройки игры.
 *
 * @property soundVolume уровень громкости звуковых эффектов (клики, реакции животных и т.д.) в % (0..100).
 * @property musicVolume уровень громкости фоновой музыки в % (0..100).
 * @property themeMode выбранный режим темы.
 */
data class GameSettings(
    val soundVolume: Int = 100,
    val musicVolume: Int = 100,
    val themeMode: ThemeMode = ThemeMode.AUTO
) {
    /** Включены ли звуковые эффекты (громкость больше 0%). */
    val soundEnabled: Boolean get() = soundVolume > 0

    /** Включена ли фоновая музыка (громкость больше 0%). */
    val musicEnabled: Boolean get() = musicVolume > 0
}
