package com.legacy.fingame.game.scene

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Where one heart of a [HeartBurst] is at some moment and how visible it is.
 *
 * Counted on the pet's own grid: in pixels of the artwork from the middle of the pet sprite, the
 * way the pet itself is drawn, so the UI multiplies both by [SceneViewport.scale] and the hearts
 * stay on the pet when the room is pinched larger or smaller.
 *
 * @property x how far the heart is to the right of the middle of the pet.
 * @property y how far it is below the middle of the pet; negative is above it.
 * @property alpha how opaque the heart is, from 1 (fully) down to 0 (gone).
 */
data class HeartFrame(val x: Float, val y: Float, val alpha: Float)

/**
 * One wave of hearts rising over the pet after the player patted it.
 *
 * The wave is [HEARTS] hearts, each starting a little later than the one before it
 * ([HEART_STAGGER_MILLIS]) right on top of the pet's head ([startY]), rising [HEART_RISE_PX] pixels
 * of the artwork over [HEART_RISE_MILLIS], swaying slightly sideways and fading out on the way up.
 * The whole wave is over [LIFE_MILLIS] after it was set off.
 *
 * @property id tells waves apart, e.g. as a key of the list the UI draws them from; unique within
 * one [PetTouchController].
 * @property startMillis when the wave was set off, on the clock the taps were counted on.
 * @property spreads where each heart starts sideways, in pixels of the artwork from the middle of
 * the pet: one entry per heart, each on its own lane ([PetTouchController.LANES_PX]) so the wave
 * reads as three hearts rather than one repeated in the same spot, and each within
 * [PetTouchController.SPREAD_PX] either way. Neighbours never overlap regardless of the lanes they
 * land on, since [HEART_STAGGER_MILLIS] keeps them a whole sprite's height apart (see [heartAt]).
 * @property startY where the middle of each heart comes out, in pixels of the artwork below the
 * middle of the pet (negative is above it): on top of the head of the pet at the age it has grown
 * to, see [heartStartY]. Defaults to the youngest pet.
 */
data class HeartBurst(
    val id: Long,
    val startMillis: Long,
    val spreads: List<Int>,
    val startY: Float = heartStartY(age = 0)
) {

    /**
     * @param index which heart of the wave, from 0 to [HEARTS] - 1.
     * @param elapsedMillis how long ago the wave was set off.
     * @return Where that heart is and how visible it is by then, or `null` while it has not come
     * out yet or is already gone.
     */
    fun heartAt(index: Int, elapsedMillis: Long): HeartFrame? {
        val own = elapsedMillis - index * HEART_STAGGER_MILLIS
        if (own < 0 || own >= HEART_RISE_MILLIS) return null
        val progress = own.toFloat() / HEART_RISE_MILLIS
        // Neighbouring hearts sway opposite ways, so the wave does not move as one block.
        val side = if (index % 2 == 0) 1f else -1f
        val sway = side * HEART_SWAY_PX * sin(progress * HEART_SWAYS * 2f * PI.toFloat())
        return HeartFrame(
            x = spreads[index] + sway,
            y = startY - HEART_RISE_PX * progress,
            alpha = if (progress < HEART_FADE_IN_FRACTION) {
                // Grown from nothing rather than popping in at full strength, so the heart does
                // not flash into view right on the pet's ears.
                progress / HEART_FADE_IN_FRACTION
            } else {
                // Fully visible for the first half of the way up, then fading out to nothing.
                ((1f - progress) * 2f).coerceIn(0f, 1f)
            }
        )
    }

    companion object {

        /** How many hearts one pat sends up. */
        const val HEARTS = 3

        /** Side of the heart sprite, in pixels of the artwork. */
        const val HEART_PIXELS = 8

        /** Side of the pet sprite, in pixels of the artwork. */
        private const val PET_PIXELS = 32

        /**
         * Top row of the pet's own pixels in its 32×32 sprite, one entry per age stage from the
         * youngest: the kitten's head is 11 rows down the sprite, the grown-up cat's is at the very
         * top of it. Measured on the cat's sprites; once other animals are drawn, move this into
         * animals.xml next to the rest of what each animal is.
         */
        private val HEAD_TOP_ROWS = intArrayOf(11, 3, 0)

        /**
         * @param age age stage the pet has grown to, counted from 0; a stage past the last one
         * known to [HEAD_TOP_ROWS] is taken as the last one, as the pet's own sprite is.
         * @return Where the middle of a heart comes out for a pet of that age, in pixels of the
         * artwork below the middle of the pet: half of [HEART_PIXELS] above the top of its head,
         * so the heart's bottom edge sits right on the head.
         */
        fun heartStartY(age: Int): Float {
            val headTop = HEAD_TOP_ROWS[age.coerceIn(0, HEAD_TOP_ROWS.lastIndex)]
            return (-PET_PIXELS / 2 + headTop - HEART_PIXELS / 2).toFloat()
        }

        /**
         * How much later each heart of a wave comes out than the one before it: 400 ms is exactly
         * a sprite's height of the rise — 12·(400/600) = [HEART_PIXELS] pixels ([HEART_RISE_PX]
         * over [HEART_RISE_MILLIS]) — so by the time a heart is born the one before it has
         * already risen a whole sprite's height further up — the two can never overlap, whatever
         * their lanes happen to land on sideways.
         */
        const val HEART_STAGGER_MILLIS = 400L

        /** How long one heart takes to rise and fade out. */
        const val HEART_RISE_MILLIS = 600L

        /** How long a whole wave lasts: until its last heart is gone. */
        const val LIFE_MILLIS = (HEARTS - 1) * HEART_STAGGER_MILLIS + HEART_RISE_MILLIS

        /**
         * How far a heart rises before it is gone, in pixels of the artwork: 12, so a heart risen
         * the whole way tops out 20 pixels over the head of a full-grown pet ([HEART_PIXELS] of
         * its own and the rise), which stays within the room the card shows over it once the
         * scene is framed [SceneViewport.PET_HEAD_ROOM_PX] higher, instead of being cut off.
         */
        const val HEART_RISE_PX = 12f

        /** How far a heart sways either way on the way up, in pixels of the artwork. */
        const val HEART_SWAY_PX = 1.5f

        /** How many times a heart sways back and forth on the way up. */
        const val HEART_SWAYS = 1.5f

        /**
         * Part of the rise a heart spends growing in rather than popping into view at once, so it
         * fades into the room instead of appearing right on the pet's ears at full strength.
         */
        const val HEART_FADE_IN_FRACTION = 0.1f
    }
}

/**
 * What patting the pet sets off: a wave of hearts, and nothing more often than every
 * [minIntervalMillis] — a player drumming on the pet gets one wave (and one sound) per
 * [minIntervalMillis], so the sound pool is never flooded and the pet not buried in hearts.
 *
 * Knows nothing about Compose or any clock: every call is told what time it is, on whatever
 * monotonic clock the caller counts in, so the whole of it is checked by plain unit tests.
 *
 * @param random where the hearts' sideways spread comes from, i.e. the jitter within a lane
 * (see [LANES_PX]).
 * @param minIntervalMillis the shortest time between two waves; a tap sooner than that after the
 * last wave is ignored.
 */
class PetTouchController(
    private val random: Random = Random.Default,
    private val minIntervalMillis: Long = MIN_TAP_INTERVAL_MILLIS
) {

    /** When the last wave was set off, or `null` before the first one. */
    private var lastBurstMillis: Long? = null

    /** Id the next wave gets. */
    private var nextId = 0L

    /** Waves set off and not yet known to be over, oldest first. */
    private val bursts = mutableListOf<HeartBurst>()

    /**
     * The player patted the pet.
     *
     * @param nowMillis what time it is.
     * @param age age stage the pet has grown to, so the hearts come out on top of its head
     *   ([HeartBurst.heartStartY]).
     * @return The new wave of hearts, or `null` when the tap came sooner than [minIntervalMillis]
     * after the last wave and is ignored — then there is no sound to play either.
     */
    fun onTap(nowMillis: Long, age: Int = 0): HeartBurst? {
        val last = lastBurstMillis
        if (last != null && nowMillis - last < minIntervalMillis) return null
        lastBurstMillis = nowMillis
        val burst = HeartBurst(
            id = nextId++,
            startMillis = nowMillis,
            // Each heart keeps to its own lane instead of an independent random spread, so they
            // never start at the same spot sideways; what keeps two of them from ever overlapping
            // is [HeartBurst.HEART_STAGGER_MILLIS] separating them vertically instead.
            spreads = LANES_PX.map { lane ->
                lane + random.nextInt(-LANE_JITTER_PX, LANE_JITTER_PX + 1)
            },
            startY = HeartBurst.heartStartY(age)
        )
        bursts += burst
        return burst
    }

    /**
     * @param nowMillis what time it is.
     * @return The waves still in the air at [nowMillis], oldest first; the ones that are over are
     * forgotten.
     */
    fun alive(nowMillis: Long): List<HeartBurst> {
        bursts.removeAll { nowMillis - it.startMillis >= HeartBurst.LIFE_MILLIS }
        return bursts.toList()
    }

    companion object {

        /**
         * Shortest time between two waves of hearts, i.e. between two pats that count: as long as
         * [HeartBurst.HEART_STAGGER_MILLIS], so by the time a new wave comes out the heart of the
         * last wave in the same lane has risen a whole sprite's height out of its way.
         */
        const val MIN_TAP_INTERVAL_MILLIS = 400L

        /** How far a heart may start from the middle of the pet sideways, in pixels of the art. */
        const val SPREAD_PX = 8

        /**
         * Where each heart of a wave starts sideways before jitter, in pixels of the artwork from
         * the middle of the pet: heart 0 to the left, heart 1 to the right, heart 2 in the middle,
         * so the three never crowd the same spot.
         */
        internal val LANES_PX = listOf(-7, 7, 0)

        /** How far a heart's lane may jitter either way, in pixels of the artwork. */
        private const val LANE_JITTER_PX = 1
    }
}
