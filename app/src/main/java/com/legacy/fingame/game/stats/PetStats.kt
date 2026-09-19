package com.legacy.fingame.game.stats

import java.util.concurrent.TimeUnit

/**
 * The pet's stat bars as the player sees them: how healthy, how fed and how happy it is right now.
 *
 * The bars only ever move in whole points between [MIN_VALUE] and [MAX_VALUE], and they fall on
 * their own as time passes (see [decayedBy]), so a pet left alone gets hungry whether the app is
 * open or not. Using an item on the pet pushes them back up (see [changedBy]).
 *
 * Nothing here knows about the stats themselves beyond [StatKind]: a stat added to that enum is
 * carried by this class without a change, and a stat that is gone from it is simply dropped when the
 * saved values are read back.
 *
 * @property values value of every stat the pet has, keyed by stat. A stat that is missing from the
 * map counts as [MAX_VALUE]: a bar the game only just learned about starts full rather than putting
 * the pet at death's door on the update that introduced it.
 */
data class PetStats(val values: Map<StatKind, Int> = emptyMap()) {

    companion object {

        /** Value of an empty bar: the pet needs the player's attention right now. */
        const val MIN_VALUE = 0

        /** Value of a full bar: there is nothing to do for the player on this stat. */
        const val MAX_VALUE = 100

        /**
         * How long the pet keeps a stat before it loses [StatKind.decayPerTick] of it. Real time,
         * not app time: the pet gets hungry while the app is closed as well, so the whole ticks that
         * fit between two launches are applied at the next one.
         */
        val TICK_MILLIS: Long = TimeUnit.MINUTES.toMillis(5)

        /** Stats of a pet nobody has neglected yet, i.e. of a brand new pet. */
        val FULL: PetStats = PetStats(StatKind.entries.associateWith { MAX_VALUE })

        /**
         * Counts the whole decay ticks between two moments.
         *
         * Only whole ticks count, and the caller is expected to move its "last decayed at" mark
         * forward by exactly [TICK_MILLIS] per counted tick, so the leftover time is not thrown away
         * and a pet checked on twice a minute still gets hungry at the same pace.
         *
         * A moment that is *behind* the mark counts as no ticks at all: moving the device clock back
         * must not undo the pet's hunger, it just leaves the pet as it is until the clock catches up.
         *
         * @param since moment the stats were last decayed at, in milliseconds.
         * @param now current moment, in milliseconds.
         * @return Number of whole ticks that passed, never negative.
         */
        fun ticksBetween(since: Long, now: Long): Long {
            val elapsed = now - since
            if (elapsed < TICK_MILLIS) return 0
            return elapsed / TICK_MILLIS
        }
    }

    /**
     * @param kind stat to read.
     * @return Value of [kind], or [MAX_VALUE] when the pet carries no value for it yet.
     */
    operator fun get(kind: StatKind): Int = values[kind] ?: MAX_VALUE

    /**
     * @param kind stat to read.
     * @return Value of [kind] as a fraction of a full bar, in the `0f..1f` range, ready for a
     * progress bar.
     */
    fun fractionOf(kind: StatKind): Float = get(kind).toFloat() / MAX_VALUE

    /**
     * Applies signed changes to the bars, e.g. the effects of an item the pet was just given.
     *
     * @param changes how much to add to each stat; negative values take from it. Stats that are not
     * mentioned are left alone.
     * @return These stats with [changes] applied, every bar kept within `MIN_VALUE..MAX_VALUE`.
     */
    fun changedBy(changes: Map<StatKind, Int>): PetStats {
        if (changes.isEmpty()) return this
        val changed = StatKind.entries.associateWith { kind ->
            (get(kind) + (changes[kind] ?: 0)).coerceIn(MIN_VALUE, MAX_VALUE)
        }
        return PetStats(changed)
    }

    /**
     * Lets the given number of decay ticks pass over the bars.
     *
     * @param ticks whole ticks that passed, as counted by [ticksBetween]; zero or less leaves the
     * stats untouched.
     * @return These stats after the decay, every bar kept within `MIN_VALUE..MAX_VALUE`.
     */
    fun decayedBy(ticks: Long): PetStats {
        if (ticks <= 0) return this
        // A device clock that jumped years forward would overflow an Int, and the answer is the
        // same either way: an empty bar.
        val drop = StatKind.entries.associateWith { kind ->
            -(kind.decayPerTick * ticks).coerceAtMost(MAX_VALUE.toLong()).toInt()
        }
        return changedBy(drop)
    }
}
