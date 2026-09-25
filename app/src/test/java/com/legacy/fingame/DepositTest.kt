package com.legacy.fingame

import com.legacy.fingame.game.economy.Deposit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Вклад: сколько он приносит, когда созревает и что делает со странными сроками. */
class DepositTest {

    @Test
    fun `the rate grows with the term, and the longest term pays the most`() {
        assertEquals(4, Deposit.rateOf(2))
        assertEquals(6, Deposit.rateOf(3))
        assertEquals(8, Deposit.rateOf(4))
        assertEquals(10, Deposit.rateOf(5))
        assertEquals(12, Deposit.rateOf(6))
        assertEquals(15, Deposit.rateOf(7))
    }

    @Test
    fun `a term outside the allowed range is pulled back into it`() {
        assertEquals(Deposit.rateOf(Deposit.MIN_TERM_DAYS), Deposit.rateOf(0))
        assertEquals(Deposit.rateOf(Deposit.MIN_TERM_DAYS), Deposit.rateOf(-3))
        assertEquals(Deposit.rateOf(Deposit.MAX_TERM_DAYS), Deposit.rateOf(30))
    }

    @Test
    fun `the interest is the rate of the amount, rounded`() {
        assertEquals(40, Deposit.interestOf(amount = 1000, ratePercent = 4))
        assertEquals(15, Deposit.interestOf(amount = 100, ratePercent = 15))
        // 33 * 15% = 4.95 -> 5
        assertEquals(5, Deposit.interestOf(amount = 33, ratePercent = 15))
        // 10 * 4% = 0.4 -> 0: a deposit too small to earn anything earns nothing.
        assertEquals(0, Deposit.interestOf(amount = 10, ratePercent = 4))
    }

    @Test
    fun `a deposit opened today matures after its term and not a day earlier`() {
        val deposit = Deposit.openedOn(amount = 200, termDays = 3, day = 19_000L)

        assertEquals(200, deposit.amount)
        assertEquals(3, deposit.termDays)
        assertEquals(6, deposit.ratePercent)
        assertEquals(19_003L, deposit.maturityDay)
        assertFalse(deposit.isMatureOn(19_002L))
        assertTrue(deposit.isMatureOn(19_003L))
        assertTrue(deposit.isMatureOn(19_010L))
    }

    @Test
    fun `the payout is the body plus the interest`() {
        val deposit = Deposit.openedOn(amount = 200, termDays = 7, day = 19_000L)

        assertEquals(30, deposit.interest)
        assertEquals(230, deposit.payout)
    }

    @Test
    fun `a deposit opened with an impossible term is opened with an allowed one`() {
        val deposit = Deposit.openedOn(amount = 100, termDays = 99, day = 19_000L)

        assertEquals(Deposit.MAX_TERM_DAYS, deposit.termDays)
        assertEquals(Deposit.rateOf(Deposit.MAX_TERM_DAYS), deposit.ratePercent)
    }
}
