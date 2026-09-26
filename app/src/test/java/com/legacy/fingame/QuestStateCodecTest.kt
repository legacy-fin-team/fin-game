package com.legacy.fingame

import com.legacy.fingame.game.quests.Quest
import com.legacy.fingame.game.quests.QuestOutcome
import com.legacy.fingame.game.quests.QuestProgress
import com.legacy.fingame.game.quests.QuestStatus
import com.legacy.fingame.game.stats.StatKind
import com.legacy.fingame.utils.QuestStateCodec
import org.junit.Assert.assertEquals
import org.junit.Test

/** Кодек квестов: состояние квестов переживает превращение в строку и обратно без потерь. */
class QuestStateCodecTest {

    private val now = 1_700_000_000_000L

    private val waiting = QuestProgress(
        questId = "picnic",
        nodeId = "food",
        availableAtMillis = now + 60_000L,
        progress = 40,
        status = QuestStatus.ACTIVE,
        lastChoice = QuestOutcome(
            optionLabel = "Фрукты и сок",
            resultText = "Вкусно и полезно. Все сыты и довольны.",
            nextNodeId = "place",
            statEffects = mapOf(StatKind.HUNGER to 20, StatKind.HEALTH to -5),
            moneyDelta = -40,
            progressDelta = 40
        )
    )

    private val atChoice = QuestProgress(questId = "lost_wallet", nodeId = "found", availableAtMillis = now)

    private val finished = QuestProgress(
        questId = "guests",
        nodeId = Quest.END_NODE,
        availableAtMillis = now,
        progress = 80,
        status = QuestStatus.FINISHED
    )

    @Test
    fun `no quests encode to an empty string and back`() {
        assertEquals("", QuestStateCodec.encode(emptyList()))
        assertEquals(emptyList<QuestProgress>(), QuestStateCodec.decode(""))
        assertEquals(emptyList<QuestProgress>(), QuestStateCodec.decode(null))
        assertEquals(emptyList<QuestProgress>(), QuestStateCodec.decode("   "))
    }

    @Test
    fun `quests in every state round-trip with all their fields`() {
        val quests = listOf(waiting, atChoice, finished)

        assertEquals(quests, QuestStateCodec.decode(QuestStateCodec.encode(quests)))
    }

    @Test
    fun `a choice without effects round-trips`() {
        val quiet = atChoice.copy(
            lastChoice = QuestOutcome(optionLabel = "Ок", resultText = "", nextNodeId = Quest.END_NODE)
        )

        assertEquals(listOf(quiet), QuestStateCodec.decode(QuestStateCodec.encode(listOf(quiet))))
    }

    @Test
    fun `separators inside the texts are dropped instead of breaking the record`() {
        val tricky = waiting.copy(
            lastChoice = waiting.lastChoice!!.copy(
                optionLabel = "Мяч\u001Fи\u001Eмяч",
                resultText = "Всё\u001Dхорошо"
            )
        )

        val decoded = QuestStateCodec.decode(QuestStateCodec.encode(listOf(tricky))).single()

        assertEquals("Мячимяч", decoded.lastChoice!!.optionLabel)
        assertEquals("Всёхорошо", decoded.lastChoice!!.resultText)
        assertEquals(tricky.lastChoice!!.statEffects, decoded.lastChoice!!.statEffects)
    }

    @Test
    fun `a malformed record is skipped while the rest decode`() {
        val good = QuestStateCodec.encode(listOf(atChoice, finished))
        val raw = listOf(
            "picnic\u001Ffood\u001Fnot-a-number",
            good,
            "picnic\u001Ffood\u001F1\u001F0\u001FLOST\u001F0\u001F\u001F\u001F\u001F\u001F0\u001F0"
        ).joinToString("\u001E")

        assertEquals(listOf(atChoice, finished), QuestStateCodec.decode(raw))
    }

    @Test
    fun `a broken choice drops its record`() {
        val broken = QuestStateCodec.encode(listOf(waiting)).replace("-40", "много")

        assertEquals(emptyList<QuestProgress>(), QuestStateCodec.decode(broken))
    }

    @Test
    fun `an effect on a stat the game no longer has is dropped, the record is kept`() {
        val raw = QuestStateCodec.encode(listOf(waiting)).replace("health=", "luck=")

        val decoded = QuestStateCodec.decode(raw).single()

        assertEquals(mapOf(StatKind.HUNGER to 20), decoded.lastChoice!!.statEffects)
    }

    @Test
    fun `the same quest twice keeps the first record`() {
        val again = atChoice.copy(nodeId = "other")
        val raw = QuestStateCodec.encode(listOf(atChoice, again))

        assertEquals(listOf(atChoice), QuestStateCodec.decode(raw))
    }

    @Test
    fun `a saved progress out of range is brought back into it`() {
        val raw = QuestStateCodec.encode(listOf(finished)).replace("\u001F80\u001F", "\u001F150\u001F")

        assertEquals(Quest.MAX_PROGRESS, QuestStateCodec.decode(raw).single().progress)
    }
}
