package com.legacy.fingame.game.economy

import java.time.LocalDate

/**
 * Where the game learns what time it is: which calendar day it is, for everything that happens once
 * a day (the daily bonus, for one), and what the moment is, for everything that happens gradually
 * (the pet's stats falling and the pet growing up).
 *
 * The game logic only ever sees this interface, so a test can play any day and any moment it likes
 * instead of waiting for midnight; the app itself runs on [DEVICE].
 */
interface GameClock {

    /**
     * @return Today as the number of days since the epoch. A day — not a timestamp — because the
     * game cares about the calendar day the player is in, not about the time on the clock.
     */
    fun today(): Long

    /**
     * @return The current moment as milliseconds since the epoch. What the pet's hunger and its
     * growth are measured against, since both of them happen between days as well as across them.
     */
    fun nowMillis(): Long

    companion object {

        /** The clock the running app uses: the calendar day and the time of the device. */
        val DEVICE: GameClock = object : GameClock {

            override fun today(): Long = LocalDate.now().toEpochDay()

            override fun nowMillis(): Long = System.currentTimeMillis()
        }
    }
}
