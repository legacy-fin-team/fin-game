package com.legacy.fingame

import com.legacy.fingame.game.economy.SpendKind
import com.legacy.fingame.game.rules.PetCareRules
import com.legacy.fingame.game.rules.PetCareTuning
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.game.stats.StatKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Формулы правил ухода: индекс, рост, штрафы и подсказка — на границах порогов. */
class PetCareRulesTest {

    private fun stats(health: Int, hunger: Int, pleasure: Int) = PetStats(
        mapOf(StatKind.HEALTH to health, StatKind.HUNGER to hunger, StatKind.PLEASURE to pleasure)
    )

    /** Все шкалы на одном уровне — индекс ухода ровно `value / 100`. */
    private fun level(value: Int) = stats(value, value, value)

    @Test
    fun `care index is the average of the bars`() {
        assertEquals(1.0, PetCareRules.careIndex(PetStats.FULL), 1e-9)
        assertEquals(0.0, PetCareRules.careIndex(level(0)), 1e-9)
        assertEquals(0.5, PetCareRules.careIndex(stats(0, 100, 50)), 1e-9)
    }

    @Test
    fun `growth stops below the critical threshold and slows below the bad one`() {
        assertEquals(0.0, PetCareRules.growthMultiplier(level(0)), 0.0)
        assertEquals(0.0, PetCareRules.growthMultiplier(level(24)), 0.0)
        assertEquals(0.5, PetCareRules.growthMultiplier(level(25)), 0.0)
        assertEquals(0.5, PetCareRules.growthMultiplier(level(49)), 0.0)
        assertEquals(1.0, PetCareRules.growthMultiplier(level(50)), 0.0)
        assertEquals(1.0, PetCareRules.growthMultiplier(PetStats.FULL), 0.0)
    }

    @Test
    fun `a day below the neglect threshold is neglected`() {
        assertTrue(PetCareRules.isNeglected(0.49))
        assertFalse(PetCareRules.isNeglected(0.5))
    }

    @Test
    fun `income falls ten percent a day down to sixty percent`() {
        assertEquals(1.0, PetCareRules.incomeMultiplier(0), 1e-9)
        assertEquals(0.9, PetCareRules.incomeMultiplier(1), 1e-9)
        assertEquals(0.6, PetCareRules.incomeMultiplier(4), 1e-9)
        assertEquals(0.6, PetCareRules.incomeMultiplier(40), 1e-9)
        assertEquals(1.0, PetCareRules.incomeMultiplier(-3), 1e-9)

        assertEquals(50, PetCareRules.dailyIncome(50, 0))
        assertEquals(45, PetCareRules.dailyIncome(50, 1))
        assertEquals(35, PetCareRules.dailyIncome(50, 3))
        assertEquals(30, PetCareRules.dailyIncome(50, 4))
        assertEquals(30, PetCareRules.dailyIncome(50, Int.MAX_VALUE))
    }

    @Test
    fun `optional goods get ten percent dearer a day up to half again`() {
        assertEquals(1.0, PetCareRules.optionalPriceMultiplier(0), 1e-9)
        assertEquals(1.1, PetCareRules.optionalPriceMultiplier(1), 1e-9)
        assertEquals(1.5, PetCareRules.optionalPriceMultiplier(5), 1e-9)
        assertEquals(1.5, PetCareRules.optionalPriceMultiplier(99), 1e-9)

        assertEquals(100, PetCareRules.priceOf(100, SpendKind.WANT, 0))
        assertEquals(110, PetCareRules.priceOf(100, SpendKind.WANT, 1))
        assertEquals(150, PetCareRules.priceOf(100, SpendKind.WANT, 7))
        // 15 * 1.1 = 16.5 — половина округляется вверх.
        assertEquals(17, PetCareRules.priceOf(15, SpendKind.WANT, 1))
    }

    @Test
    fun `necessary goods never get dearer`() {
        assertEquals(15, PetCareRules.priceOf(15, SpendKind.MUST, 0))
        assertEquals(15, PetCareRules.priceOf(15, SpendKind.MUST, 10))
    }

    @Test
    fun `tuning changes the rule in one place`() {
        val strict = PetCareTuning(
            stopGrowthBelow = 0.6,
            slowGrowthBelow = 0.9,
            slowGrowthMultiplier = 0.25,
            incomePenaltyPercentPerDay = 20,
            maxIncomePenaltyPercent = 50,
            optionalMarkupPercentPerDay = 25,
            maxOptionalMarkupPercent = 100
        )

        assertEquals(0.0, PetCareRules.growthMultiplier(level(59), strict), 0.0)
        assertEquals(0.25, PetCareRules.growthMultiplier(level(60), strict), 0.0)
        assertEquals(1.0, PetCareRules.growthMultiplier(level(90), strict), 0.0)
        assertEquals(25, PetCareRules.dailyIncome(50, 5, strict))
        assertEquals(200, PetCareRules.priceOf(100, SpendKind.WANT, 9, strict))
    }

    @Test
    fun `a pet that is doing fine gets no hint`() {
        assertNull(PetCareRules.explain(PetStats.FULL, dayBestCare = 1.0, neglectStreak = 0))
        assertNull(PetCareRules.explain(level(10), dayBestCare = 0.5, neglectStreak = 0))
    }

    @Test
    fun `the hint names what the pet lacks most`() {
        assertEquals(
            "Питомец голодный — он растёт медленнее.",
            PetCareRules.explain(stats(40, 10, 60), dayBestCare = 0.3, neglectStreak = 0)
        )
        assertEquals(
            "Питомец скучает и перестал расти. Позаботься о нём!",
            PetCareRules.explain(stats(20, 30, 5), dayBestCare = 0.1, neglectStreak = 0)
        )
        // Пустые шкалы поровну: называется то, что ребёнок исправит сам, — голод.
        assertEquals(
            "Питомец голодный и перестал расти. Позаботься о нём!",
            PetCareRules.explain(level(0), dayBestCare = 0.0, neglectStreak = 0)
        )
    }

    @Test
    fun `the hint tells why the bonus is smaller and goods dearer`() {
        assertEquals(
            "Питомец 2 дня без заботы — бонус дня меньше, а наряды и декор дороже.",
            PetCareRules.explain(PetStats.FULL, dayBestCare = 1.0, neglectStreak = 2)
        )
        assertEquals(
            "Питомец голодный — он растёт медленнее. " +
                "Питомец 1 день без заботы — бонус дня меньше, а наряды и декор дороже.",
            PetCareRules.explain(stats(40, 10, 60), dayBestCare = 0.3, neglectStreak = 1)
        )
    }

    @Test
    fun `days are counted in proper Russian`() {
        assertEquals("1 день", PetCareRules.daysText(1))
        assertEquals("3 дня", PetCareRules.daysText(3))
        assertEquals("5 дней", PetCareRules.daysText(5))
        assertEquals("11 дней", PetCareRules.daysText(11))
        assertEquals("21 день", PetCareRules.daysText(21))
        assertEquals("22 дня", PetCareRules.daysText(22))
    }
}
