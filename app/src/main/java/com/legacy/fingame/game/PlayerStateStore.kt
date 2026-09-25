package com.legacy.fingame.game

/**
 * Where the [PlayerState] is kept between app launches.
 *
 * The game layer only ever sees this interface, so it stays free of any knowledge about how and
 * where the state is actually stored; the storage the app itself runs on is
 * [com.legacy.fingame.utils.PlayerPreferences].
 */
interface PlayerStateStore {

    /**
     * @return The state saved by [save] during an earlier run, or a default [PlayerState] when
     * nothing was ever saved, i.e. this is the first launch.
     */
    fun load(): PlayerState

    /**
     * Saves the state as a whole, replacing whatever was saved before.
     *
     * @param state the state to remember for the next launch.
     */
    fun save(state: PlayerState)
}
