package com.legacy.fingame

import com.legacy.fingame.game.economy.MoneyEntry
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.utils.MoneyLogCodec
import org.junit.Assert.assertEquals
import org.junit.Test

/** Кодек журнала: журнал переживает превращение в строку и обратно без потерь. */
class MoneyLogCodecTest {

    private fun entry(
        reason: String,
        delta: Int,
        day: Long = 19_000L,
        timestamp: Long = 1_700_000_000_000L
    ) = MoneyEntry(reason = reason, delta = delta, gameDay = day, timestampMillis = timestamp)

    @Test
    fun `an empty log encodes to an empty string`() {
        assertEquals("", MoneyLogCodec.encode(MoneyLog.EMPTY))
    }

    @Test
    fun `an empty string decodes to an empty log`() {
        assertEquals(MoneyLog.EMPTY, MoneyLogCodec.decode(""))
    }

    @Test
    fun `null decodes to an empty log`() {
        assertEquals(MoneyLog.EMPTY, MoneyLogCodec.decode(null))
    }

    @Test
    fun `a blank string decodes to an empty log`() {
        assertEquals(MoneyLog.EMPTY, MoneyLogCodec.decode("   "))
    }

    @Test
    fun `several entries keep their order and all four fields`() {
        val log = MoneyLog(
            listOf(
                entry("Яблоко x4", -60, day = 19_001L, timestamp = 1_700_000_100_000L),
                entry(MoneyLog.REASON_DAILY_BONUS, 50, day = 19_000L, timestamp = 1_699_999_000_000L)
            )
        )

        val decoded = MoneyLogCodec.decode(MoneyLogCodec.encode(log))

        assertEquals(log.entries, decoded.entries)
    }

    @Test
    fun `a negative delta round-trips`() {
        val log = MoneyLog(listOf(entry("В сбережения", -100)))

        val decoded = MoneyLogCodec.decode(MoneyLogCodec.encode(log))

        assertEquals(-100, decoded.entries.single().delta)
    }

    @Test
    fun `a reason with Cyrillic and spaces round-trips`() {
        val log = MoneyLog(listOf(entry("Проценты по вкладу", 15)))

        val decoded = MoneyLogCodec.decode(MoneyLogCodec.encode(log))

        assertEquals("Проценты по вкладу", decoded.entries.single().reason)
    }

    @Test
    fun `a malformed record is skipped while the rest decode`() {
        val good1 = MoneyLogCodec.encode(MoneyLog(listOf(entry(MoneyLog.REASON_DAILY_BONUS, 50))))
        val good2 = MoneyLogCodec.encode(MoneyLog(listOf(entry("Яблоко x4", -60))))
        // Битая запись: не хватает поля (только причина и дельта).
        val bad = "Испорчено\u001Fnot-a-number"
        val raw = listOf(good1, bad, good2).joinToString("\u001E")

        val decoded = MoneyLogCodec.decode(raw)

        assertEquals(
            listOf(MoneyLog.REASON_DAILY_BONUS, "Яблоко x4"),
            decoded.entries.map { it.reason }
        )
    }
}
