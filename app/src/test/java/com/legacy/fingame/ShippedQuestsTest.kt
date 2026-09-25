package com.legacy.fingame

import com.legacy.fingame.game.quests.Quest
import com.legacy.fingame.game.quests.QuestCatalog
import com.legacy.fingame.game.quests.QuestKind
import com.legacy.fingame.game.quests.QuestReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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
    fun `the piggy bank is taken by the player, has no progress and no waits`() {
        val piggy = readShippedQuests().single { it.id == "piggy_bank" }

        assertEquals(QuestKind.PLAYER, piggy.kind)
        assertFalse(piggy.hasProgress)
        assertEquals(0, piggy.minBalance)
        assertEquals(2, piggy.stepCount)
        assertTrue(piggy.nodes.values.all { it.delayMinutes == 0 })
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
    fun `the guests come by themselves, have progress and wait two minutes`() {
        val guests = readShippedQuests().single { it.id == "guests" }

        assertEquals(QuestKind.RANDOM, guests.kind)
        assertTrue(guests.hasProgress)
        assertEquals(2, guests.stepCount)
        assertEquals(2, guests.node(guests.firstNodeId)!!.delayMinutes)
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
    fun `a catalog finds a quest by its id`() {
        val quests = readShippedQuests()
        val catalog = QuestCatalog.of(quests)

        assertEquals(quests, catalog.quests)
        assertSame(quests[0], catalog.findQuestById(quests[0].id))
        assertNull(catalog.findQuestById("nowhere"))
        assertEquals(emptyList<Quest>(), QuestCatalog.EMPTY.quests)
    }
}
