package com.legacy.fingame

import com.legacy.fingame.game.economy.Deposit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Вклад: сколько он приносит, когда созревает и что делает со странными сроками. */
class DepositTest {

    @Test
    fun `the terms are 3, 5 and 7 days at 15, 30 and 50 percent`() {
        assertEquals(listOf(3, 5, 7), Deposit.TERM_DAYS)
        assertEquals(15, Deposit.rateOf(3))
        assertEquals(30, Deposit.rateOf(5))
        assertEquals(50, Deposit.rateOf(7))
    }

    @Test
    fun `the shortest term pays at least 15 percent`() {
        assertEquals(3, Deposit.MIN_TERM_DAYS)
        assertTrue(Deposit.TERM_DAYS.all { Deposit.rateOf(it) >= 15 })
        // Положил 100 — получил 115.
        assertEquals(115, Deposit.openedOn(amount = 100, termDays = Deposit.MIN_TERM_DAYS, day = 0L).payout)
    }

    @Test
    fun `a term between the offered ones is rounded up to the next one`() {
        assertEquals(3, Deposit.termOf(2))
        assertEquals(5, Deposit.termOf(4))
        assertEquals(7, Deposit.termOf(6))
        assertEquals(15, Deposit.rateOf(2))
        assertEquals(30, Deposit.rateOf(4))
        assertEquals(50, Deposit.rateOf(6))
    }

    @Test
    fun `every day in the bank pays more, the longer the term`() {
        // Рост за день с процентами на проценты: (1 + ставка) ^ (1 / срок).
        val daily = Deposit.TERM_DAYS.map { term ->
            Math.pow(1 + Deposit.rateOf(term) / 100.0, 1.0 / term)
        }
        daily.zipWithNext().forEach { (shorter, longer) -> assertTrue(longer > shorter) }
    }

    @Test
    fun `a longer deposit beats rolling shorter ones over the same days, coins rounded`() {
        // Для каждой суммы и каждого срока: лучшее, что можно выжать, перекладывая деньги со
        // всеми процентами на более короткие сроки подряд (без пропусков между ними) за то же
        // число дней, — не больше, чем даёт сам длинный срок; с 10 монет — строго меньше.
        for (term in Deposit.TERM_DAYS) {
            for (amount in 1..3000) {
                val long = amount + Deposit.interestOf(amount, Deposit.rateOf(term))
                val rolled = bestRolled(amount, term, shorterThan = term)
                assertTrue("$amount на $term дн: $rolled > $long", rolled <= long)
                if (amount >= 10 && term > Deposit.MIN_TERM_DAYS) {
                    assertTrue("$amount на $term дн: $rolled == $long", rolled < long)
                }
            }
        }
    }

    @Test
    fun `a week in the bank pays more than two short deposits in a row`() {
        val week = Deposit.openedOn(amount = 100, termDays = 7, day = 0L).payout
        val first = Deposit.openedOn(amount = 100, termDays = 3, day = 0L).payout
        val second = Deposit.openedOn(amount = first, termDays = 3, day = 3L).payout

        assertEquals(150, week)
        assertEquals(132, second) // 100 -> 115 -> 132 (15% от 115 = 17,25 -> 17)
        assertTrue(week > second)
    }

    /** Лучшая сумма после цепочки вкладов сроками короче [shorterThan], уложенной в [days] дней. */
    private fun bestRolled(amount: Int, days: Int, shorterThan: Int): Int {
        var best = amount
        for (term in Deposit.TERM_DAYS) {
            if (term < shorterThan && term <= days) {
                val grown = amount + Deposit.interestOf(amount, Deposit.rateOf(term))
                best = maxOf(best, bestRolled(grown, days - term, shorterThan))
            }
        }
        return best
    }

    @Test
    fun `a term outside the allowed range is pulled back into it`() {
        assertEquals(Deposit.rateOf(Deposit.MIN_TERM_DAYS), Deposit.rateOf(0))
        assertEquals(Deposit.rateOf(Deposit.MIN_TERM_DAYS), Deposit.rateOf(-3))
        assertEquals(Deposit.rateOf(Deposit.MAX_TERM_DAYS), Deposit.rateOf(30))
    }

    @Test
    fun `the interest is the rate of the amount, rounded`() {
        assertEquals(150, Deposit.interestOf(amount = 1000, ratePercent = 15))
        assertEquals(15, Deposit.interestOf(amount = 100, ratePercent = 15))
        // 33 * 15% = 4.95 -> 5
        assertEquals(5, Deposit.interestOf(amount = 33, ratePercent = 15))
        // 3 * 15% = 0.45 -> 0: a deposit too small to earn anything earns nothing.
        assertEquals(0, Deposit.interestOf(amount = 3, ratePercent = 15))
    }

    @Test
    fun `a deposit opened today matures after its term and not a day earlier`() {
        val deposit = Deposit.openedOn(amount = 200, termDays = 3, day = 19_000L)

        assertEquals(200, deposit.amount)
        assertEquals(3, deposit.termDays)
        assertEquals(15, deposit.ratePercent)
        assertEquals(19_003L, deposit.maturityDay)
        assertFalse(deposit.isMatureOn(19_002L))
        assertTrue(deposit.isMatureOn(19_003L))
        assertTrue(deposit.isMatureOn(19_010L))
    }

    @Test
    fun `the payout is the body plus the interest`() {
        val deposit = Deposit.openedOn(amount = 200, termDays = 7, day = 19_000L)

        assertEquals(100, deposit.interest)
        assertEquals(300, deposit.payout)
    }

    @Test
    fun `a deposit opened with an impossible term is opened with an allowed one`() {
        val deposit = Deposit.openedOn(amount = 100, termDays = 99, day = 19_000L)

        assertEquals(Deposit.MAX_TERM_DAYS, deposit.termDays)
        assertEquals(Deposit.rateOf(Deposit.MAX_TERM_DAYS), deposit.ratePercent)
    }
}
