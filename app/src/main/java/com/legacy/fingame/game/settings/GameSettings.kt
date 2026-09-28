package com.legacy.fingame.game.settings

/**
 * Настройки игры.
 *
 * @property soundEnabled включены ли звуковые эффекты (клики, реакции животных и т.д.).
 * @property musicEnabled включена ли фоновая музыка.
 * @property themeMode выбранный режим темы.
 */
data class GameSettings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.AUTO
)
