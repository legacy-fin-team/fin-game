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

    @Test
    fun `a log built at once lays its records out newest first`() {
        val log = MoneyLog.of(
            listOf(
                entry("День 1, запись а", 1, day = 19_000L),
                entry("День 3", 3, day = 19_002L),
                entry("День 1, запись б", 2, day = 19_000L),
                entry("День 2", 4, day = 19_001L)
            )
        )

        assertEquals(
            listOf("День 3", "День 2", "День 1, запись а", "День 1, запись б"),
            log.entries.map { it.reason }
        )
    }

    @Test
    fun `records of one and the same moment keep the order they came in`() {
        val first = MoneyEntry("Первая", 1, gameDay = 19_000L, timestampMillis = 1_700_000_000_000L)
        val second = MoneyEntry("Вторая", 2, gameDay = 19_000L, timestampMillis = 1_700_000_000_000L)

        val log = MoneyLog.of(listOf(first, second))

        assertEquals(listOf("Первая", "Вторая"), log.entries.map { it.reason })
    }

    @Test
    fun `the newest MAX_ENTRIES are kept, whatever place they had in the list`() {
        val shuffled = (0 until MoneyLog.MAX_ENTRIES + 1)
            .map { day -> entry("День $day", day, day = day.toLong()) }
            .shuffled()

        val log = MoneyLog.of(shuffled)

        assertEquals(MoneyLog.MAX_ENTRIES, log.entries.size)
        // Самая старая запись (день 0) выпадает, даже если в исходном списке она была не последней.
        assertEquals(false, log.entries.any { it.reason == "День 0" })
        assertEquals("День ${MoneyLog.MAX_ENTRIES}", log.entries.first().reason)
    }
}
