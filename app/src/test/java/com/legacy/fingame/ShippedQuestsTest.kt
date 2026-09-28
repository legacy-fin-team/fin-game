package com.legacy.fingame

import com.legacy.fingame.game.quests.Quest
import com.legacy.fingame.game.quests.QuestCatalog
import com.legacy.fingame.game.quests.QuestKind
import com.legacy.fingame.game.quests.QuestReader
import com.legacy.fingame.game.quests.QuestTopic
import com.legacy.fingame.game.stats.StatKind
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Квесты, с которыми игра поставляется: файл из assets читается целиком, и в нём ровно то, что
 * обещает спека.
 */
class ShippedQuestsTest {

    /**
     * Самая длинная надпись на кнопке варианта, которая помещается в одну строку на 360 dp при
     * шрифте 1.3: карточка даёт кнопке около 270 dp, моноширинный пиксельный шрифт — 17 dp на букву.
     */
    private val maxOptionLabelLength = 16

    /** @return Квесты из `src/main/assets/data/quests.xml`, прочитанные так же, как в приложении. */
    private fun readShippedQuests(): List<Quest> {
        // Тесты идут из каталога модуля, но запуск из корня сборки должен найти тот же файл.
        val file = listOf(
            File("src/main/assets/data/quests.xml"),
            File("app/src/main/assets/data/quests.xml")
        ).firstOrNull { it.exists() }
        assertTrue("data/quests.xml is not in the assets", file != null)
        return file!!.inputStream().use { QuestReader().readQuests(it) }
    }

    @Test
    fun `the four quests of the spec are all read`() {
        val quests = readShippedQuests()

        assertEquals(
            listOf("picnic", "piggy_bank", "lost_wallet", "guests"),
            quests.map { it.id }
        )
    }

    @Test
    fun `every shipped quest has a topic`() {
        val topics = readShippedQuests().associate { it.id to it.topic }

        assertEquals(
            mapOf(
                "picnic" to QuestTopic.PLANNING,
                "piggy_bank" to QuestTopic.SAVING,
                "lost_wallet" to QuestTopic.HONESTY,
                "guests" to QuestTopic.NEEDS_WANTS
            ),
            topics
        )
    }

    @Test
    fun `the picnic needs 100 coins, has progress and waits a minute between steps`() {
        val picnic = readShippedQuests().single { it.id == "picnic" }

        assertEquals("Пикник", picnic.title)
        assertEquals(QuestKind.PLAYER, picnic.kind)
        assertTrue(picnic.hasProgress)
        assertEquals(100, picnic.minBalance)
        assertEquals(3, picnic.stepCount)
        assertEquals(listOf(1, 1, 0), picnic.stepOrder.map { picnic.node(it)!!.delayMinutes })
        assertTrue(picnic.nodes.values.flatMap { it.options }.any { it.moneyDelta < 0 })
    }

    @Test
    fun `the piggy bank is taken by the player, has no progress and waits after saving`() {
        val piggy = readShippedQuests().single { it.id == "piggy_bank" }

        assertEquals(QuestKind.PLAYER, piggy.kind)
        assertFalse(piggy.hasProgress)
        assertEquals(0, piggy.minBalance)
        assertEquals(2, piggy.stepCount)
        // «Подождём!» после «В копилку» — правда: следующий шаг открывается через две минуты.
        assertEquals(listOf(2, 0), piggy.stepOrder.map { piggy.node(it)!!.delayMinutes })
        assertTrue(piggy.nodes.values.flatMap { it.options }.any { it.moneyDelta > 0 })
    }

    @Test
    fun `the lost wallet comes by itself with one situation and two ways out`() {
        val wallet = readShippedQuests().single { it.id == "lost_wallet" }

        assertEquals(QuestKind.RANDOM, wallet.kind)
        assertFalse(wallet.hasProgress)
        assertEquals(1, wallet.stepCount)
        assertEquals(2, wallet.node(wallet.firstNodeId)!!.options.size)
    }

    @Test
    fun `returning the wallet pays less but feels better than keeping it`() {
        val wallet = readShippedQuests().single { it.id == "lost_wallet" }
        val (giveBack, keep) = wallet.node(wallet.firstNodeId)!!.options

        assertEquals("Вернуть", giveBack.label)
        assertEquals(20, giveBack.moneyDelta)
        assertEquals(mapOf(StatKind.PLEASURE to 15), giveBack.statEffects)
        assertEquals("Оставить себе", keep.label)
        assertEquals(30, keep.moneyDelta)
        assertEquals(mapOf(StatKind.PLEASURE to -20), keep.statEffects)
    }

    @Test
    fun `the guests come by themselves, have progress and wait two minutes`() {
        val guests = readShippedQuests().single { it.id == "guests" }

        assertEquals(QuestKind.RANDOM, guests.kind)
        assertTrue(guests.hasProgress)
        assertEquals(2, guests.stepCount)
        assertEquals(2, guests.node(guests.firstNodeId)!!.delayMinutes)
        val tea = guests.node(guests.firstNodeId)!!.options.single { it.label == "Позвать на чай" }
        assertEquals(0, tea.moneyDelta)
        assertEquals(10, tea.progressDelta)
        assertEquals(mapOf(StatKind.PLEASURE to 5), tea.statEffects)
        assertEquals("tidy", tea.nextNodeId)
    }

    @Test
    fun `every step has an option that costs nothing`() {
        readShippedQuests().forEach { quest ->
            quest.nodes.values.forEach { node ->
                assertTrue(
                    "${quest.id}/${node.id} has no free option",
                    node.options.any { it.moneyDelta >= 0 }
                )
            }
        }
    }

    @Test
    fun `every node can be reached and every label fits one line`() {
        readShippedQuests().forEach { quest ->
            assertEquals("${quest.id} has unreachable nodes", quest.nodes.size, quest.stepCount)
            assertEquals("quests/${quest.id}.webp", quest.imagePath)
            quest.nodes.values.flatMap { it.options }.forEach { option ->
                assertTrue(
                    "'${option.label}' of ${quest.id} is too long for one line",
                    option.label.length <= maxOptionLabelLength
                )
            }
        }
    }

    @Test
    fun `the best choices of a quest with progress add up to exactly 100 percent`() {
        readShippedQuests().filter { it.hasProgress }.forEach { quest ->
            val best = quest.stepOrder.sumOf { nodeId ->
                quest.node(nodeId)!!.options.maxOf { it.progressDelta }
            }
            assertEquals(quest.id, Quest.MAX_PROGRESS, best)
        }
    }

    @Test
    fun `every quest is repeatable with at least the default cooldown`() {
        readShippedQuests().forEach { quest ->
            assertTrue("${quest.id} should stay repeatable", quest.repeatable)
            assertTrue(
                "${quest.id} cooldown is below the default",
                quest.cooldownMinutes >= Quest.DEFAULT_COOLDOWN_MINUTES
            )
        }
    }

    @Test
    fun `the piggy bank has a noticeable cooldown so it cannot be farmed`() {
        val piggy = readShippedQuests().single { it.id == "piggy_bank" }

        assertTrue(piggy.cooldownMinutes > Quest.DEFAULT_COOLDOWN_MINUTES)
    }

    @Test
    fun `a catalog finds a quest by its id`() {
        val quests = readShippedQuests()
        val catalog = QuestCatalog.of(quests)

        assertEquals(quests, catalog.quests)
        assertSame(quests[0], catalog.findQuestById(quests[0].id))
        assertNull(catalog.findQuestById("nowhere"))
        assertEquals(emptyList<Quest>(), QuestCatalog.EMPTY.quests)
    }
}
