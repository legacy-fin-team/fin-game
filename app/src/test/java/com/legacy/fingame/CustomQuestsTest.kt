package com.legacy.fingame

import com.legacy.fingame.game.quests.CompositeQuestCatalog
import com.legacy.fingame.game.quests.CustomQuestDraft
import com.legacy.fingame.game.quests.CustomQuestOptionDraft
import com.legacy.fingame.game.quests.CustomQuestStepDraft
import com.legacy.fingame.game.quests.CustomQuests
import com.legacy.fingame.game.quests.CustomQuestsCodec
import com.legacy.fingame.game.quests.Quest
import com.legacy.fingame.game.quests.QuestCatalog
import com.legacy.fingame.game.quests.QuestKind
import com.legacy.fingame.game.quests.QuestNode
import com.legacy.fingame.game.quests.QuestOption
import com.legacy.fingame.game.stats.StatKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Свои квесты взрослого: проверка черновика, сборка квеста, кодек, общий каталог. */
class CustomQuestsTest {

    private val cleaning = CustomQuestDraft(
        title = "Уборка",
        description = "Помоги дома",
        minBalance = 10,
        steps = listOf(
            CustomQuestStepDraft(
                text = "Комната в беспорядке",
                delayMinutes = 30,
                options = listOf(
                    CustomQuestOptionDraft("Убрать сейчас", "Чисто", progressDelta = 25),
                    CustomQuestOptionDraft("Потом", "Беспорядок")
                )
            ),
            CustomQuestStepDraft(
                text = "Мама предлагает 20 монет за помощь",
                options = listOf(
                    CustomQuestOptionDraft("Помочь", "Мама довольна", moneyDelta = 20, progressDelta = 50),
                    CustomQuestOptionDraft("Отказаться", "Грустно", moodDelta = -5)
                )
            )
        )
    )

    @Test
    fun `a filled draft is valid`() {
        assertEquals(emptyList<String>(), cleaning.validate())
    }

    @Test
    fun `an empty draft names every missing text`() {
        val errors = CustomQuestDraft().validate()

        assertTrue("Впиши название" in errors)
        assertTrue("Впиши описание" in errors)
        assertTrue("Шаг 1: опиши ситуацию" in errors)
        assertTrue("Шаг 1, вариант 1: впиши текст кнопки" in errors)
        assertTrue("Шаг 1, вариант 2: впиши результат" in errors)
    }

    @Test
    fun `limits are checked`() {
        val long = cleaning.copy(title = "а".repeat(CustomQuests.TITLE_MAX + 1))
        assertTrue(long.validate().any { it.startsWith("Название") })

        val broke = cleaning.copy(minBalance = 1000)
        assertTrue(broke.validate().any { it.startsWith("Минимум") })

        val oneOption = cleaning.copy(
            steps = listOf(cleaning.steps[0].copy(options = cleaning.steps[0].options.take(1)))
        )
        assertTrue(oneOption.validate().any { it.contains("варианта") })

        val fourSteps = cleaning.copy(steps = List(4) { cleaning.steps[0] })
        assertTrue(fourSteps.validate().any { it.startsWith("Шагов") })

        val oddMoney = cleaning.copy(
            steps = listOf(
                cleaning.steps[0].copy(
                    options = listOf(cleaning.steps[0].options[0].copy(moneyDelta = 15), cleaning.steps[0].options[1])
                )
            )
        )
        assertTrue(oddMoney.validate().any { it.contains("монеты") })

        val badDelay = cleaning.copy(steps = listOf(cleaning.steps[0].copy(delayMinutes = 7)))
        assertTrue(badDelay.validate().any { it.contains("задержку") })

        val badProgress = cleaning.copy(
            steps = listOf(
                cleaning.steps[0].copy(
                    options = listOf(cleaning.steps[0].options[0].copy(progressDelta = 30), cleaning.steps[0].options[1])
                )
            )
        )
        assertTrue(badProgress.validate().any { it.contains("прогресс") })

        assertTrue(cleaning.validate(existingCount = CustomQuests.MAX).any { it.contains("не больше") })
    }

    @Test
    fun `the quest is a linear chain of the steps`() {
        val quest = cleaning.toQuest("custom-1")

        assertEquals("custom-1", quest.id)
        assertEquals(QuestKind.PLAYER, quest.kind)
        assertEquals("s1", quest.firstNodeId)
        assertEquals(listOf("s1", "s2"), quest.stepOrder)
        assertEquals(10, quest.minBalance)
        assertTrue(quest.hasProgress)
        assertTrue(quest.node("s1")!!.options.all { it.nextNodeId == "s2" })
        assertTrue(quest.node("s2")!!.options.all { it.nextNodeId == Quest.END_NODE })
        assertEquals(30, quest.node("s1")!!.delayMinutes)
        val help = quest.node("s2")!!.options[0]
        assertEquals(20, help.moneyDelta)
        assertEquals(50, help.progressDelta)
        assertEquals(emptyMap<StatKind, Int>(), help.statEffects)
        assertEquals(mapOf(StatKind.PLEASURE to -5), quest.node("s2")!!.options[1].statEffects)
    }

    @Test
    fun `without progress in any option the quest has none`() {
        val flat = cleaning.copy(
            steps = cleaning.steps.map { step ->
                step.copy(options = step.options.map { it.copy(progressDelta = 0) })
            }
        )
        assertFalse(flat.toQuest("custom-2").hasProgress)
    }

    @Test
    fun `texts are cleaned of separators and trimmed`() {
        val dirty = cleaning.copy(title = "  Убо\u001Eрка\u001F ", description = "Помоги\nдома")
        val quest = dirty.toQuest("custom-3")

        assertEquals("Уборка", quest.title)
        assertEquals("Помогидома", quest.description)
    }

    @Test
    fun `the codec reads back what it wrote`() {
        val quests = listOf(cleaning.toQuest("custom-1"), cleaning.copy(title = "Вторая").toQuest("custom-2"))

        val decoded = CustomQuestsCodec.decode(CustomQuestsCodec.encode(quests))

        assertEquals(quests, decoded)
        assertEquals(cleaning, CustomQuestDraft.of(decoded[0]))
    }

    @Test
    fun `broken records are dropped, the rest survive`() {
        val good = CustomQuestsCodec.encode(listOf(cleaning.toQuest("custom-1")))
        val raw = listOf(
            "custom-x\u001Fобрывок",
            good,
            good, // повтор id
            "\u001F" + good.substringAfter('\u001F'), // без id
            good.replace("custom-1", "custom-2").replace("\u001F30\u001F", "\u001Fдесять\u001F")
        ).joinToString("\u001E")

        val decoded = CustomQuestsCodec.decode(raw)

        assertEquals(listOf("custom-1"), decoded.map { it.id })
        assertEquals(emptyList<Quest>(), CustomQuestsCodec.decode(null))
        assertEquals(emptyList<Quest>(), CustomQuestsCodec.decode(""))
    }

    @Test
    fun `the codec keeps at most the limit`() {
        val many = List(CustomQuests.MAX + 5) { cleaning.toQuest("custom-$it") }

        assertEquals(CustomQuests.MAX, CustomQuestsCodec.decode(CustomQuestsCodec.encode(many)).size)
    }

    @Test
    fun `a half-typed draft survives the saver as is`() {
        val half = CustomQuestDraft(title = "Убо ", steps = listOf(CustomQuestStepDraft(text = "")))

        assertEquals(half, CustomQuestsCodec.decodeDraft(CustomQuestsCodec.encodeDraft(half)))
    }

    @Test
    fun `the composite catalog lists the game quests first and finds both`() {
        val base = Quest(
            id = "piggy",
            title = "Копилка",
            description = "",
            kind = QuestKind.PLAYER,
            firstNodeId = "a",
            nodes = mapOf("a" to QuestNode("a", "", listOf(QuestOption("Ок", "", Quest.END_NODE))))
        )
        var custom = listOf(cleaning.toQuest("custom-1"))
        val catalog = CompositeQuestCatalog(QuestCatalog.of(listOf(base))) { custom }

        assertEquals(listOf("piggy", "custom-1"), catalog.quests.map { it.id })
        assertSame(base, catalog.findQuestById("piggy"))
        assertEquals("Уборка", catalog.findQuestById("custom-1")?.title)

        custom = emptyList()
        assertNull(catalog.findQuestById("custom-1"))
        assertEquals(listOf("piggy"), catalog.quests.map { it.id })
    }
}
