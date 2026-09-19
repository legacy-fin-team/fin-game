package com.legacy.fingame.game

import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.game.economy.Economy
import com.legacy.fingame.game.items.ItemSelection

/**
 * Everything about the player's game that has to outlive the app process, i.e. what the player
 * finds in place when coming back after the app was closed.
 *
 * Screen-level state is deliberately not part of it: which screen is open and what is picked in
 * the shop belong to a single visit and start over on the next launch. Money and the things bought
 * with it, on the other hand, are the player's own and are kept.
 *
 * @property selection the pet the player picked, or null while no pet has been picked yet — that
 * is, before the very first launch is over.
 * @property subLocationIndex index of the sub-location the pet was left in, within
 * [com.legacy.fingame.ui.DemoContent.subLocationTitles]. A saved index only means something for
 * the sub-locations that exist now, so [GameViewModel] coerces it into the current range when it
 * restores the state.
 * @property balance coins the player has to spend, never negative. A player who has never played
 * starts with [Economy.STARTING_BALANCE].
 * @property lastDailyBonusDay day the daily bonus was last claimed on, as days since the epoch, or
 * [Economy.NEVER_CLAIMED] when it was never claimed. Only whole days are kept, since the bonus is
 * given once per calendar day.
 * @property owned how many of each item the player bought, keyed by item and variant: the variant
 * is part of the key because the player owns the black hat, not "a hat". Food is bought over and
 * over, so its counter grows; everything else is owned once.
 */
data class PlayerState(
    val selection: AnimalSelection? = null,
    val subLocationIndex: Int = 0,
    val balance: Int = Economy.STARTING_BALANCE,
    val lastDailyBonusDay: Long = Economy.NEVER_CLAIMED,
    val owned: Map<ItemSelection, Int> = emptyMap()
)
