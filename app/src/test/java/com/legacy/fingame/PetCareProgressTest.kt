package com.legacy.fingame

import com.legacy.fingame.game.animals.Animal
import com.legacy.fingame.game.animals.Growth
import com.legacy.fingame.game.rules.PetCare
import com.legacy.fingame.game.rules.PetCareRules
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.game.stats.StatKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Как дни питомца закрываются: рост за день и серия дней без заботы. */
class PetCareProgressTest {

    private val born = 1_000_000_000L
    private val day = Growth.DAY_MILLIS

    /** Шкалы, которые не падают: у этой проверки не распад, а суд над днём. */
    private val empty = PetStats(StatKind.entries.associateWith { PetStats.MIN_VALUE })

    @Test
    fun `nothing is judged before the pet's first day is over`() {
        val care = PetCare()

        assertSame(care, care.lived(born, PetStats.FULL, born, born + day - 1))
        assertSame(care, care.lived(Growth.NOT_BORN, PetStats.FULL, born, born + day * 5))
    }

    @Test
    fun `a well cared for day grows the pet a full day of growth`() {
        val care = PetCare().lived(born, PetStats.FULL, born, born + day)

        assertEquals(1L, care.judgedDays)
        assertEquals(day, care.growthMillis)
        assertEquals(0, care.neglectStreak)
    }

    @Test
    fun `a so-so day grows the pet half a day of growth and a bad one not at all`() {
        val soSo = PetCare(dayBestCare = 0.3).lived(born, empty, born, born + day)
        val bad = PetCare(dayBestCare = 0.1).lived(born, empty, born, born + day)

        assertEquals(day / 2, soSo.growthMillis)
        assertEquals(0L, bad.growthMillis)
        assertEquals(1, soSo.neglectStreak)
        assertEquals(1, bad.neglectStreak)
    }

    @Test
    fun `a pet left alone starts every next day empty and the streak builds up`() {
        // Сытый питомец, которого бросили на три дня: первый день ещё хороший — он начался сытым,
        // а к началу второго шкалы уже пустые.
        val care = PetCare().lived(born, PetStats.FULL, born, born + day * 3)

        assertEquals(3L, care.judgedDays)
        assertEquals(day, care.growthMillis)
        assertEquals(2, care.neglectStreak)
        assertEquals(0.0, care.dayBestCare, 0.0)
    }

    @Test
    fun `a good day wipes the streak out`() {
        val neglected = PetCare(judgedDays = 3, neglectStreak = 3, dayBestCare = 0.0)
        val cared = neglected.noticed(PetStats.FULL)

        val next = cared.lived(born, PetStats.FULL, born + day * 3, born + day * 4)

        assertEquals(0, next.neglectStreak)
        assertEquals(day, next.growthMillis)
    }

    @Test
    fun `only the best moment of the day counts`() {
        val care = PetCare(dayBestCare = 0.2)

        assertEquals(1.0, care.noticed(PetStats.FULL).dayBestCare, 0.0)
        assertEquals(0.2, care.noticed(empty).dayBestCare, 0.0)
    }

    @Test
    fun `years without the app are judged at once and the streak stays bounded`() {
        val care = PetCare().lived(born, PetStats.FULL, born, born + day * 365_000)

        assertEquals(365_000L, care.judgedDays)
        assertEquals(day, care.growthMillis)
        assertEquals(PetCare.MAX_STREAK, care.neglectStreak)
    }

    @Test
    fun `an old save keeps the age its pet already had`() {
        val care = PetCare.migrated(born, born + day * 2 + 5, PetStats.FULL)

        assertEquals(2L, care.judgedDays)
        assertEquals(day * 2, care.growthMillis)
        assertEquals(0, care.neglectStreak)
        assertEquals(PetCareRules.FULL_CARE, care.dayBestCare, 0.0)
        assertEquals(0L, PetCare.migrated(Growth.NOT_BORN, born, PetStats.FULL).judgedDays)
    }

    /** Ставит питомцу полные шкалы и прожитый день: так ухаживает ребёнок, который заходит каждый день. */
    private fun perfectDays(days: Int): PetCare {
        var care = PetCare()
        repeat(days) { d ->
            care = care.noticed(PetStats.FULL)
                .lived(born, PetStats.FULL, born + day * d, born + day * (d + 1))
        }
        return care
    }

    @Test
    fun `growing up takes no less than 21 days even with perfect care`() {
        assertEquals(Growth.FULL_GROWTH_DAYS.toLong() * day, Growth.FULL_GROWTH_MILLIS)

        assertEquals(Animal.FIRST_AGE, Growth.ageOf(perfectDays(10).growthMillis))
        assertEquals(Animal.FIRST_AGE + 1, Growth.ageOf(perfectDays(11).growthMillis))
        assertEquals(Animal.FIRST_AGE + 1, Growth.ageOf(perfectDays(20).growthMillis))
        assertTrue(Growth.ageOf(perfectDays(20).growthMillis) < Growth.ADULT_AGE)
        assertEquals(Growth.ADULT_AGE, Growth.ageOf(perfectDays(21).growthMillis))
        assertEquals(0, perfectDays(21).neglectStreak)
    }
}
