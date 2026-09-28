package com.legacy.fingame

import com.legacy.fingame.game.adult.RewardUsage
import com.legacy.fingame.game.adult.RewardUsageLog
import com.legacy.fingame.ui.screens.rewardUsageDetailText
import com.legacy.fingame.utils.RewardUsageLogCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Журнал «Использованные награды»: предел, новые записи, кодек. */
class RewardUsageLogTest {

    private fun usage(millis: Long, name: String = "Поход в кино") = RewardUsage(
        itemId = "custom-1",
        itemName = name,
        petName = "Мурка",
        gameDay = 20_000L,
        timestampMillis = millis
    )

    @Test
    fun `the newest usage comes first and the log is capped`() {
        var log = RewardUsageLog.EMPTY
        repeat(RewardUsageLog.MAX_ENTRIES + 5) { log = log.plus(usage(it.toLong())) }

        assertEquals(RewardUsageLog.MAX_ENTRIES, log.entries.size)
        assertEquals((RewardUsageLog.MAX_ENTRIES + 4).toLong(), log.entries.first().timestampMillis)
    }

    @Test
    fun `usages after the last look are new`() {
        val log = RewardUsageLog(listOf(usage(300), usage(200), usage(100)))

        assertEquals(3, log.unseenCount(RewardUsageLog.NEVER_SEEN))
        assertEquals(2, log.unseenCount(100))
        assertEquals(0, log.unseenCount(300))
    }

    @Test
    fun `the codec reads back what it wrote and drops broken records`() {
        val log = RewardUsageLog(listOf(usage(200, "Час\u001Fмульт\u001Eиков"), usage(100)))

        val decoded = RewardUsageLogCodec.decode(RewardUsageLogCodec.encode(log))

        assertEquals(listOf("Часмультиков", "Поход в кино"), decoded.entries.map { it.itemName })
        assertEquals(usage(100), decoded.entries[1])
        assertEquals(RewardUsageLog.EMPTY, RewardUsageLogCodec.decode(null))
        assertEquals(RewardUsageLog.EMPTY, RewardUsageLogCodec.decode(""))
        val broken = "custom-1\u001Fкино\u001FМурка\u001Fдень\u001F100"
        val good = RewardUsageLogCodec.encode(RewardUsageLog(listOf(usage(100))))
        assertEquals(listOf(usage(100)), RewardUsageLogCodec.decode("$broken\u001E$good").entries)
    }

    @Test
    fun `the adult reads whose pet and which day`() {
        val text = rewardUsageDetailText(usage(0L), firstDay = 19_998L)

        assertTrue(text.startsWith("Питомец Мурка"))
        assertTrue(text.contains("день 3"))
        assertTrue(rewardUsageDetailText(usage(0L).copy(petName = ""), 19_998L).startsWith("день"))
    }
}
