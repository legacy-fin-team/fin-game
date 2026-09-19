package com.legacy.fingame.game

import com.legacy.fingame.game.animals.AnimalSelection

/**
 * Everything about the player's game that has to outlive the app process, i.e. what the player
 * finds in place when coming back after the app was closed.
 *
 * Screen-level state is deliberately not part of it: which screen is open and what is picked in
 * the shop belong to a single visit and start over on the next launch.
 *
 * @property selection the pet the player picked, or null while no pet has been picked yet — that
 * is, before the very first launch is over.
 * @property subLocationIndex index of the sub-location the pet was left in, within
 * [com.legacy.fingame.ui.DemoContent.subLocationTitles]. A saved index only means something for
 * the sub-locations that exist now, so [GameViewModel] coerces it into the current range when it
 * restores the state.
 */
data class PlayerState(
    val selection: AnimalSelection? = null,
    val subLocationIndex: Int = 0
)
