package com.legacy.fingame

import com.legacy.fingame.game.economy.Budget
import com.legacy.fingame.game.economy.BudgetDraft
import com.legacy.fingame.game.economy.BudgetResult
import com.legacy.fingame.game.economy.BudgetState
import com.legacy.fingame.game.economy.Deposit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Раскладка денег на период и итог, который она даёт в конце. */
class BudgetTest {

    @Test
    fun `the starting draft plans nothing at all`() {
        val draft = Budget.startingDraft()

        assertEquals(0, draft.mustSpend)
        assertEquals(0, draft.wantSpend)
        assertEquals(0, draft.depositAmount)
        assertEquals(Deposit.MIN_TERM_DAYS, draft.depositTermDays)
    }

    @Test
    fun `what the plans and the deposit did not take is what the player saves`() {
        val draft = BudgetDraft(
            mustSpend = 200,
            wantSpend = 100,
            depositAmount = 300,
            depositTermDays = 4
        )

        assertEquals(400, Budget.savingsOf(draft, total = 1000))
    }

    @Test
    fun `the deposit is cut down first, and the plans are counted from what it left`() {
        val draft = Budget.normalize(
            draft = BudgetDraft(
                mustSpend = 900,
                wantSpend = 900,
                depositAmount = 5_000,
                depositTermDays = 5
            ),
            total = 1000,
            depositAllowed = true
        )

        assertEquals(1000, draft.depositAmount)
        assertEquals(0, draft.mustSpend)
        assertEquals(0, draft.wantSpend)
    }

    @Test
    fun `the must plan comes before the want plan when there is not enough for both`() {
        val draft = Budget.normalize(
            draft = BudgetDraft(
                mustSpend = 500,
                wantSpend = 500,
                depositAmount = 200,
                depositTermDays = 3
            ),
            total = 600,
            depositAllowed = true
        )

        assertEquals(200, draft.depositAmount)
        assertEquals(400, draft.mustSpend)
        assertEquals(0, draft.wantSpend)
        assertEquals(0, Budget.savingsOf(draft, total = 600))
    }

    @Test
    fun `a normalized draft never plans to save less than nothing`() {
        listOf(0, 1, 250, 1000).forEach { total ->
            val draft = Budget.normalize(
                draft = BudgetDraft(
                    mustSpend = 900,
                    wantSpend = 900,
                    depositAmount = 900,
                    depositTermDays = 7
                ),
                total = total,
                depositAllowed = true
            )

            assertTrue(Budget.savingsOf(draft, total) >= 0)
        }
    }

    @Test
    fun `no deposit is opened while one is already open`() {
        val draft = Budget.normalize(
            draft = BudgetDraft(
                mustSpend = 100,
                wantSpend = 0,
                depositAmount = 500,
                depositTermDays = 3
            ),
            total = 1000,
            depositAllowed = false
        )

        assertEquals(0, draft.depositAmount)
        assertEquals(100, draft.mustSpend)
        assertEquals(900, Budget.savingsOf(draft, total = 1000))
    }

    @Test
    fun `a term nobody offers becomes one that is offered`() {
        val tooShort = Budget.normalize(
            draft = BudgetDraft(
                mustSpend = 0,
                wantSpend = 0,
                depositAmount = 100,
                depositTermDays = 0
            ),
            total = 1000,
            depositAllowed = true
        )
        val tooLong = Budget.normalize(
            draft = BudgetDraft(
                mustSpend = 0,
                wantSpend = 0,
                depositAmount = 100,
                depositTermDays = 99
            ),
            total = 1000,
            depositAllowed = true
        )

        assertEquals(Deposit.MIN_TERM_DAYS, tooShort.depositTermDays)
        assertEquals(Deposit.MAX_TERM_DAYS, tooLong.depositTermDays)
    }

    @Test
    fun `negative amounts are read as nothing`() {
        val draft = Budget.normalize(
            draft = BudgetDraft(
                mustSpend = -100,
                wantSpend = -100,
                depositAmount = -50,
                depositTermDays = 3
            ),
            total = 400,
            depositAllowed = true
        )

        assertEquals(0, draft.mustSpend)
        assertEquals(0, draft.wantSpend)
        assertEquals(0, draft.depositAmount)
        assertEquals(400, Budget.savingsOf(draft, total = 400))
    }

    @Test
    fun `spending less than planned is a plus, spending more is a minus`() {
        val result = BudgetResult(
            plannedMust = 300,
            actualMust = 200,
            plannedWant = 100,
            actualWant = 140,
            plannedSavings = 150,
            actualSavings = 150,
            plannedDeposit = 0
        )

        assertEquals(100, result.mustDiff)
        assertEquals(-40, result.wantDiff)
        assertEquals(0, result.savingsDiff)
        assertEquals(0, result.copy(actualMust = 300).mustDiff)
        assertEquals(0, result.copy(actualWant = 100).wantDiff)
    }

    @Test
    fun `a running budget adds its categories up and shows what each one has left`() {
        val budget = BudgetState(
            plannedMust = 150,
            plannedWant = 50,
            plannedSavings = 100,
            plannedDeposit = 200,
            spentMust = 60,
            spentWant = 80,
            startDay = 19_000L
        )

        assertEquals(200, budget.plannedSpend)
        assertEquals(140, budget.spent)
        assertEquals(90, budget.mustLeft)
        assertEquals(-30, budget.wantLeft)
    }

    @Test
    fun `saving more than planned is a plus, saving less is a minus`() {
        val result = BudgetResult(
            plannedMust = 0,
            actualMust = 0,
            plannedWant = 0,
            actualWant = 0,
            plannedSavings = 100,
            actualSavings = 160,
            plannedDeposit = 0
        )

        assertEquals(60, result.savingsDiff)
        assertEquals(-40, result.copy(actualSavings = 60).savingsDiff)
    }
}
