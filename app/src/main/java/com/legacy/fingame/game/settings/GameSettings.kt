package com.legacy.fingame.game.settings

/**
 * Настройки игры.
 *
 * @property soundVolume уровень громкости звуковых эффектов (клики, реакции животных и т.д.) в % (0..100).
 * @property musicVolume уровень громкости фоновой музыки в % (0..100).
 * @property themeMode выбранный режим темы.
 * @property animationsEnabled включены ли анимации: покадровые спрайты (например, плавающая рыбка),
 *   сердечки над питомцем, плавная смена экранов и всплывание окон. Выключенные — всё то же самое
 *   показывается сразу и неподвижно: первый кадр спрайта, сердечки на месте, окно без всплывания.
 */
data class GameSettings(
    val soundVolume: Int = 100,
    val musicVolume: Int = 100,
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val animationsEnabled: Boolean = true
) {
    /** Включены ли звуковые эффекты (громкость больше 0%). */
    val soundEnabled: Boolean get() = soundVolume > 0

    /** Включена ли фоновая музыка (громкость больше 0%). */
    val musicEnabled: Boolean get() = musicVolume > 0
}
