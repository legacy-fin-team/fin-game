package com.legacy.fingame

import com.legacy.fingame.game.economy.MoneyEntry
import com.legacy.fingame.game.economy.MoneyLog
import org.junit.Assert.assertEquals
import org.junit.Test

/** Журнал: что в нём оказывается, в каком порядке и сколько его хранится. */
class MoneyLogTest {

    private fun entry(reason: String, delta: Int, day: Long = 19_000L) = MoneyEntry(
        reason = reason,
        delta = delta,
        gameDay = day,
        timestampMillis = 1_700_000_000_000L + day
    )

    @Test
    fun `an empty log holds nothing`() {
        assertEquals(emptyList<MoneyEntry>(), MoneyLog.EMPTY.entries)
    }

    @Test
    fun `the newest record comes first`() {
        val log = MoneyLog.EMPTY
            .plus(entry(MoneyLog.REASON_DAILY_BONUS, 50))
            .plus(entry(MoneyLog.purchaseReason("Яблоко", 4), -60))

        assertEquals(
            listOf(MoneyLog.purchaseReason("Яблоко", 4), MoneyLog.REASON_DAILY_BONUS),
            log.entries.map { it.reason }
        )
        assertEquals(listOf(-60, 50), log.entries.map { it.delta })
    }

    @Test
    fun `a purchase names the item and how many of it were bought`() {
        assertEquals("Яблоко x4", MoneyLog.purchaseReason("Яблоко", 4))
        assertEquals("Шапка x1", MoneyLog.purchaseReason("Шапка", 1))
    }

    @Test
    fun `only the last records are kept, and the oldest one falls off`() {
        var log = MoneyLog.EMPTY
        repeat(MoneyLog.MAX_ENTRIES + 10) { index ->
            log = log.plus(entry("Операция $index", index))
        }

        assertEquals(MoneyLog.MAX_ENTRIES, log.entries.size)
        // Новейшая запись — последняя добавленная, старейшая из оставшихся — та, после которой
        // уже не осталось места ни для одной из более ранних.
        assertEquals("Операция ${MoneyLog.MAX_ENTRIES + 9}", log.entries.first().reason)
        assertEquals("Операция 10", log.entries.last().reason)
    }

    @Test
    fun `adding a record leaves the log it was added to alone`() {
        val before = MoneyLog.EMPTY.plus(entry(MoneyLog.REASON_DAILY_BONUS, 50))
        val after = before.plus(entry(MoneyLog.REASON_DEPOSIT_OPENED, -100))

        assertEquals(1, before.entries.size)
        assertEquals(2, after.entries.size)
    }
}
