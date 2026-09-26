package com.legacy.fingame

import com.legacy.fingame.game.quests.QuestChoice
import com.legacy.fingame.game.quests.QuestLog
import com.legacy.fingame.utils.QuestLogCodec
import org.junit.Assert.assertEquals
import org.junit.Test

/** Журнал выборов в квестах: лимит и превращение в строку и обратно. */
class QuestLogCodecTest {

    private fun choice(
        label: String = "Фрукты",
        millis: Long = 1_700_000_000_000L,
        money: Int = -40
    ) = QuestChoice(
        questId = "picnic",
        nodeId = "food",
        optionLabel = label,
        moneyDelta = money,
        progressDelta = 60,
        gameDay = 19_000L,
        timestampMillis = millis
    )

    @Test
    fun `empty and missing strings decode to an empty log`() {
        assertEquals(QuestLog.EMPTY, QuestLogCodec.decode(null))
        assertEquals(QuestLog.EMPTY, QuestLogCodec.decode(""))
        assertEquals("", QuestLogCodec.encode(QuestLog.EMPTY))
    }

    @Test
    fun `a log round-trips with every field`() {
        val log = QuestLog(
            listOf(
                choice("Мяч", millis = 1_700_000_100_000L, money = 0),
                choice("Фрукты", millis = 1_700_000_000_000L, money = -40)
            )
        )

        assertEquals(log, QuestLogCodec.decode(QuestLogCodec.encode(log)))
    }

    @Test
    fun `separators in a label do not break the format`() {
        val log = QuestLog(listOf(choice("Сыр\u001Fи\u001Eхлеб")))

        val decoded = QuestLogCodec.decode(QuestLogCodec.encode(log))

        assertEquals("Сырихлеб", decoded.entries.single().optionLabel)
    }

    @Test
    fun `a malformed record is skipped while the rest decode`() {
        val good = QuestLogCodec.encode(QuestLog(listOf(choice())))
        val bad = "picnic\u001Ffood\u001FФрукты\u001Fnot-a-number"

        val decoded = QuestLogCodec.decode(listOf(bad, good).joinToString("\u001E"))

        assertEquals(listOf(choice()), decoded.entries)
    }

    @Test
    fun `the log keeps the newest choice first and at most the limit`() {
        val log = (0 until QuestLog.MAX_ENTRIES + 7).fold(QuestLog.EMPTY) { acc, i ->
            acc.plus(choice(label = "$i", millis = 1_700_000_000_000L + i))
        }

        assertEquals(QuestLog.MAX_ENTRIES, log.entries.size)
        assertEquals("${QuestLog.MAX_ENTRIES + 6}", log.entries.first().optionLabel)
        assertEquals("7", log.entries.last().optionLabel)
    }
}
