package com.legacy.fingame

import com.legacy.fingame.game.animals.Animal
import com.legacy.fingame.game.animals.Growth
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.game.stats.StatKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * The pet's stat bars and the way it grows up: the rules that have to give the same answer wherever
 * they are asked, checked without a pet and without a screen.
 */
class PetStatsTest {

    @Test
    fun `a brand new pet has every bar full`() {
        StatKind.entries.forEach { stat ->
            assertEquals(PetStats.MAX_VALUE, PetStats.FULL[stat])
            assertEquals(1f, PetStats.FULL.fractionOf(stat), 0f)
        }
    }

    @Test
    fun `a bar the pet carries no value for counts as full`() {
        // This is what a save written before the stat existed reads back as: the update that added
        // the bar must not hand the player a pet at death's door.
        val stats = PetStats(mapOf(StatKind.HUNGER to 10))

        assertEquals(10, stats[StatKind.HUNGER])
        assertEquals(PetStats.MAX_VALUE, stats[StatKind.HEALTH])
    }

    @Test
    fun `using an item moves the bars it names and leaves the rest alone`() {
        val stats = PetStats(StatKind.entries.associateWith { 50 })

        val fed = stats.changedBy(mapOf(StatKind.HUNGER to 20, StatKind.HEALTH to -10))

        assertEquals(70, fed[StatKind.HUNGER])
        assertEquals(40, fed[StatKind.HEALTH])
        assertEquals(50, fed[StatKind.PLEASURE])
    }

    @Test
    fun `a bar never goes past full or past empty`() {
        val stats = PetStats(StatKind.entries.associateWith { 95 })

        val overfed = stats.changedBy(mapOf(StatKind.HUNGER to 50, StatKind.HEALTH to -500))

        assertEquals(PetStats.MAX_VALUE, overfed[StatKind.HUNGER])
        assertEquals(PetStats.MIN_VALUE, overfed[StatKind.HEALTH])
    }

    @Test
    fun `an item that does nothing leaves the stats as they are`() {
        val stats = PetStats(mapOf(StatKind.HUNGER to 42))

        assertSame(stats, stats.changedBy(emptyMap()))
    }

    @Test
    fun `every bar falls at its own pace`() {
        val fallen = PetStats.FULL.decayedBy(ticks = 3)

        StatKind.entries.forEach { stat ->
            assertEquals(PetStats.MAX_VALUE - stat.decayPerTick * 3, fallen[stat])
        }
    }

    @Test
    fun `a pet left alone for ages ends up with empty bars, not with negative ones`() {
        val fallen = PetStats.FULL.decayedBy(ticks = Long.MAX_VALUE / PetStats.TICK_MILLIS)

        StatKind.entries.forEach { stat ->
            assertEquals(PetStats.MIN_VALUE, fallen[stat])
        }
    }

    @Test
    fun `only whole ticks count and the leftover time is not lost`() {
        val start = 1_000_000L

        assertEquals(0L, PetStats.ticksBetween(start, start))
        assertEquals(0L, PetStats.ticksBetween(start, start + PetStats.TICK_MILLIS - 1))
        assertEquals(1L, PetStats.ticksBetween(start, start + PetStats.TICK_MILLIS))
        assertEquals(2L, PetStats.ticksBetween(start, start + PetStats.TICK_MILLIS * 2 + 1))
    }

    @Test
    fun `moving the clock back does not undo the pet's hunger`() {
        val start = 1_000_000L

        assertEquals(0L, PetStats.ticksBetween(start, start - PetStats.TICK_MILLIS * 10))
    }

    @Test
    fun `a pet grows one stage per stage worth of growing`() {
        assertEquals(Animal.FIRST_AGE, Growth.ageOf(0L))
        assertEquals(Animal.FIRST_AGE, Growth.ageOf(Growth.STAGE_MILLIS - 1))
        assertEquals(Animal.FIRST_AGE + 1, Growth.ageOf(Growth.STAGE_MILLIS))
        assertEquals(Animal.FIRST_AGE + 1, Growth.ageOf(Growth.STAGE_MILLIS * 3 / 2))
        assertEquals(Animal.FIRST_AGE + 4, Growth.ageOf(Growth.STAGE_MILLIS * 4))
    }

    @Test
    fun `a pet that has not grown at all is at the youngest stage`() {
        assertEquals(Animal.FIRST_AGE, Growth.ageOf(-Growth.STAGE_MILLIS))
        assertEquals(Animal.FIRST_AGE, Growth.ageOf(Long.MIN_VALUE))
    }

    @Test
    fun `a pet grown beyond any stage count does not overflow`() {
        assertEquals(Int.MAX_VALUE, Growth.ageOf(Long.MAX_VALUE))
    }
}
