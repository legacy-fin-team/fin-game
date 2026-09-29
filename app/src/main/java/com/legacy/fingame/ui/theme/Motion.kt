package com.legacy.fingame.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Включены ли анимации в игре — настройка «Анимации»
 * ([com.legacy.fingame.game.settings.GameSettings.animationsEnabled]).
 *
 * Задаётся один раз в корне приложения, а читают её те, кто что-то двигает: спрайты (покадровые
 * анимации показываются первым кадром), сердечки над питомцем (стоят на месте), смена экранов
 * (без растворения) и окна ([DialogWindowMotion]: без всплывания). Всё, что анимация показывала,
 * при выключенной настройке всё равно показывается — сразу и неподвижно.
 *
 * Меняется редко, только из настроек, поэтому static: при переключении перерисовывается всё
 * дерево, и спрайты перезагружаются в нужном виде.
 */
val LocalAnimationsEnabled = staticCompositionLocalOf { true }

/**
 * Включает или выключает анимацию появления и исчезновения окна, в котором вызван: у окон
 * платформы она своя (окно всплывает и растворяется), и Compose её не видит. Вызывается внутри
 * содержимого `Dialog`; вне окна ничего не делает.
 *
 * При выключенных [LocalAnimationsEnabled] окно появляется и пропадает сразу, при включённых — так,
 * как задумано темой окна.
 */
@Composable
fun DialogWindowMotion() {
    val animationsEnabled = LocalAnimationsEnabled.current
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    // Анимации, которые окну дала тема, запоминаются, чтобы их можно было вернуть.
    val themed = remember(window) { window?.attributes?.windowAnimations ?: 0 }
    SideEffect {
        // 0 — «никакой анимации»: у окна нет стиля, из которого её брать.
        window?.setWindowAnimations(if (animationsEnabled) themed else 0)
    }
}
