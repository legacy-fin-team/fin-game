package com.legacy.fingame.game.economy

import java.util.concurrent.TimeUnit

/**
 * A [GameClock] that runs ahead of another one by as much as it has been pushed.
 *
 * It is how time is skipped without anything being faked: the game keeps reading one clock and keeps
 * its own rules — the stats decay per whole tick, the pet grows per stage, the bonus pays once a day
 * — only the clock is further along than the device's. Pushing it by twelve hours therefore leaves
 * the pet exactly where twelve hours of waiting would have left it.
 *
 * Until it is pushed, it is the clock it wraps, hour for hour and day for day; pushed, it keeps
 * running with it, so the time that passes while the demo is being shown still counts.
 *
 * @param source clock the current moment and the current day are read from.
 */
class FastForwardClock(private val source: GameClock) : GameClock {

    companion object {

        /** Shift of a clock nobody pushed yet, i.e. one that simply tells the device's time. */
        const val NO_SHIFT = 0L

        /** Length of the day the calendar is moved by, see [today]. */
        private val DAY_MILLIS: Long = TimeUnit.DAYS.toMillis(1)
    }

    /** How far ahead of [source] this clock is, in milliseconds; never negative. */
    var shiftMillis: Long = NO_SHIFT
        private set

    /**
     * @return The day [source] is on plus the whole days this clock has been pushed by. Whole days
     * only, and counted from the shift rather than from a calendar, so a day passes after a full
     * day's worth of pushing whatever time of day the demo happens to be shown at.
     */
    override fun today(): Long = source.today() + shiftMillis / DAY_MILLIS

    override fun nowMillis(): Long = source.nowMillis() + shiftMillis

    /**
     * Pushes this clock further ahead.
     *
     * @param millis how much to add to the shift; zero or less leaves the clock where it is, since a
     * clock that went backwards would only stall the game — the pet's hunger and the daily bonus both
     * refuse to be undone by a clock moving back.
     */
    fun fastForward(millis: Long) {
        if (millis <= 0) return
        shiftMillis += millis
    }
}
