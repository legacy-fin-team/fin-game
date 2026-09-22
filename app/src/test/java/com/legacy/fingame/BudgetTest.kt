package com.legacy.fingame

import com.legacy.fingame.game.economy.Budget
import com.legacy.fingame.game.economy.BudgetDraft
import com.legacy.fingame.game.economy.BudgetResult
import com.legacy.fingame.game.economy.Deposit
import org.junit.Assert.assertEquals
import org.junit.Test

/** Раскладка денег на период и итог, который она даёт в конце. */
class BudgetTest {

    @Test
    fun `the starting draft leaves the savings where they are and opens no deposit`() {
        val draft = Budget.startingDraft(savings = 120)

        assertEquals(120, draft.savings)
        assertEquals(0, draft.depositAmount)
        assertEquals(Deposit.MIN_TERM_DAYS, draft.depositTermDays)
    }

    @Test
    fun `the current money is whatever the savings and the deposit did not take`() {
        val draft = BudgetDraft(savings = 200, depositAmount = 300, depositTermDays = 4)

        assertEquals(500, Budget.currentOf(draft, total = 1000))
    }

    @Test
    fun `savings past everything the player has are cut down to it`() {
        val draft = Budget.normalize(
            draft = BudgetDraft(savings = 5_000, depositAmount = 0, depositTermDays = 2),
            total = 400,
            depositAllowed = true
        )

        assertEquals(400, draft.savings)
        assertEquals(0, Budget.currentOf(draft, total = 400))
    }

    @Test
    fun `a deposit past what the savings left is cut down to it`() {
        val draft = Budget.normalize(
            draft = BudgetDraft(savings = 300, depositAmount = 900, depositTermDays = 5),
            total = 1000,
            depositAllowed = true
        )

        assertEquals(300, draft.savings)
        assertEquals(700, draft.depositAmount)
        assertEquals(0, Budget.currentOf(draft, total = 1000))
    }

    @Test
    fun `no deposit is opened while one is already open`() {
        val draft = Budget.normalize(
            draft = BudgetDraft(savings = 100, depositAmount = 500, depositTermDays = 3),
            total = 1000,
            depositAllowed = false
        )

        assertEquals(0, draft.depositAmount)
        assertEquals(900, Budget.currentOf(draft, total = 1000))
    }

    @Test
    fun `a term nobody offers becomes one that is offered`() {
        val tooShort = Budget.normalize(
            draft = BudgetDraft(savings = 0, depositAmount = 100, depositTermDays = 0),
            total = 1000,
            depositAllowed = true
        )
        val tooLong = Budget.normalize(
            draft = BudgetDraft(savings = 0, depositAmount = 100, depositTermDays = 99),
            total = 1000,
            depositAllowed = true
        )

        assertEquals(Deposit.MIN_TERM_DAYS, tooShort.depositTermDays)
        assertEquals(Deposit.MAX_TERM_DAYS, tooLong.depositTermDays)
    }

    @Test
    fun `negative amounts are read as nothing`() {
        val draft = Budget.normalize(
            draft = BudgetDraft(savings = -100, depositAmount = -50, depositTermDays = 3),
            total = 400,
            depositAllowed = true
        )

        assertEquals(0, draft.savings)
        assertEquals(0, draft.depositAmount)
        assertEquals(400, Budget.currentOf(draft, total = 400))
    }

    @Test
    fun `spending less than planned is a plus, spending more is a minus`() {
        val saved = BudgetResult(
            planned = 300,
            plannedSavings = 100,
            plannedDeposit = 0,
            actual = 200
        )
        val overspent = saved.copy(actual = 400)
        val exact = saved.copy(actual = 300)

        assertEquals(100, saved.diff)
        assertEquals(-100, overspent.diff)
        assertEquals(0, exact.diff)
    }
}
