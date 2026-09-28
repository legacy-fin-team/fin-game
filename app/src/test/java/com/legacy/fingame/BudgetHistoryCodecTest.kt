package com.legacy.fingame

import com.legacy.fingame.game.economy.BudgetHistory
import com.legacy.fingame.game.economy.BudgetResult
import com.legacy.fingame.utils.BudgetHistoryCodec
import org.junit.Assert.assertEquals
import org.junit.Test

/** История бюджета: пополнение с лимитом и превращение в строку и обратно. */
class BudgetHistoryCodecTest {

    private fun result(day: Long, actualMust: Int = 30) = BudgetResult(
        plannedMust = 40,
        actualMust = actualMust,
        plannedWant = 20,
        actualWant = 25,
        plannedSavings = 90,
        actualSavings = 85,
        plannedDeposit = 50,
        startDay = day
    )

    @Test
    fun `empty and missing strings decode to an empty history`() {
        assertEquals(emptyList<BudgetResult>(), BudgetHistoryCodec.decode(null))
        assertEquals(emptyList<BudgetResult>(), BudgetHistoryCodec.decode(""))
        assertEquals(emptyList<BudgetResult>(), BudgetHistoryCodec.decode("  "))
        assertEquals("", BudgetHistoryCodec.encode(emptyList()))
    }

    @Test
    fun `a history round-trips with every field`() {
        val history = listOf(result(19_002L, actualMust = 55), result(19_001L), result(19_000L))

        assertEquals(history, BudgetHistoryCodec.decode(BudgetHistoryCodec.encode(history)))
    }

    @Test
    fun `a shuffled history decodes with the newest period first`() {
        val shuffled = listOf(result(19_000L), result(19_002L), result(19_001L))

        val decoded = BudgetHistoryCodec.decode(BudgetHistoryCodec.encode(shuffled))

        assertEquals(listOf(19_002L, 19_001L, 19_000L), decoded.map { it.startDay })
    }

    @Test
    fun `a malformed record is skipped while the rest decode`() {
        val good = BudgetHistoryCodec.encode(listOf(result(19_000L)))
        val bad = "19001\u001F40\u001Fмного"

        val decoded = BudgetHistoryCodec.decode(listOf(bad, good).joinToString("\u001E"))

        assertEquals(listOf(result(19_000L)), decoded)
    }

    @Test
    fun `the history keeps the newest periods first and at most the limit`() {
        val history = (0 until BudgetHistory.MAX + 5).fold(emptyList<BudgetResult>()) { acc, i ->
            BudgetHistory.plus(acc, result(19_000L + i))
        }

        assertEquals(BudgetHistory.MAX, history.size)
        assertEquals(19_000L + BudgetHistory.MAX + 4, history.first().startDay)
        assertEquals(19_005L, history.last().startDay)
    }

    @Test
    fun `an overlong saved history is cut to the limit`() {
        val history = (0 until BudgetHistory.MAX + 3).map { result(19_000L + it) }

        val decoded = BudgetHistoryCodec.decode(BudgetHistoryCodec.encode(history))

        assertEquals(BudgetHistory.MAX, decoded.size)
        assertEquals(19_000L + BudgetHistory.MAX + 2, decoded.first().startDay)
    }
}
