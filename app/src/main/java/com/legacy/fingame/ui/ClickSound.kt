@file:OptIn(ExperimentalFoundationApi::class)

package com.legacy.fingame.ui

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi

/**
 * Системный щелчок, который Compose сам играет при каждом нажатии `clickable`-элемента (кнопки,
 * чекбоксы, карточки магазина).
 *
 * Выключается одним глобальным флагом Compose, который проверяется при каждом нажатии, поэтому
 * переключение действует сразу и во всех окнах — в главном, в диалогах и во всплывающих окнах.
 * Флаг `View.isSoundEffectsEnabled` для этого не годится: щелчок играет не тот view, на котором его
 * обычно выключают, а у каждого диалога вообще свой.
 */
object ClickSound {

    /**
     * Звучит ли щелчок при нажатии. `true` — звучит (если звук касания включён и в самом Android),
     * `false` — молчит.
     */
    var enabled: Boolean
        get() = ComposeFoundationFlags.isInteractionSoundEffectOnClickEnabled
        set(value) {
            ComposeFoundationFlags.isInteractionSoundEffectOnClickEnabled = value
        }
}
