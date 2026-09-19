package com.legacy.fingame.game.economy

import java.time.LocalDate

/**
 * Where the game learns which day it is, for everything that happens once a day (the daily bonus,
 * for one).
 *
 * The game logic only ever sees this interface, so a test can play any day it likes instead of
 * waiting for midnight; the app itself runs on [DEVICE].
 */
fun interface GameClock {

    /**
     * @return Today as the number of days since the epoch. A day — not a timestamp — because the
     * game cares about the calendar day the player is in, not about the time on the clock.
     */
    fun today(): Long

    companion object {

        /** The clock the running app uses: the calendar day of the device, in its own time zone. */
        val DEVICE: GameClock = GameClock { LocalDate.now().toEpochDay() }
    }
}
