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
import com.legacy.fingame.game.quests.QuestTopic
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
        ),
        stageDelayMinutes = 30
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

        val badDelay = cleaning.copy(stageDelayMinutes = CustomQuests.STAGE_DELAY_MAX + 1)
        assertTrue(badDelay.validate().any { it.startsWith("Пауза") })

        val badCooldown = cleaning.copy(cooldownMinutes = -1)
        assertTrue(badCooldown.validate().any { it.startsWith("Кулдаун") })
        // У одноразового квеста кулдаун не используется и не проверяется.
        assertEquals(emptyList<String>(), badCooldown.copy(repeatable = false).validate())

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
        assertEquals(30, quest.stageDelayMinutes)
        assertTrue(quest.repeatable)
        assertEquals(Quest.DEFAULT_COOLDOWN_MINUTES, quest.cooldownMinutes)
        val help = quest.node("s2")!!.options[0]
        assertEquals(20, help.moneyDelta)
        assertEquals(50, help.progressDelta)
        assertEquals(emptyMap<StatKind, Int>(), help.statEffects)
        assertEquals(mapOf(StatKind.PLEASURE to -5), quest.node("s2")!!.options[1].statEffects)
    }

    @Test
    fun `an option changes every bar of the pet and the codec keeps them`() {
        val meal = cleaning.copy(
            steps = listOf(
                cleaning.steps[0].copy(
                    options = listOf(
                        CustomQuestOptionDraft("Суп", "Сытно", hungerDelta = 20, healthDelta = 10, moodDelta = -5),
                        CustomQuestOptionDraft("Потом", "Голодно", hungerDelta = -10)
                    )
                )
            )
        )
        val quest = meal.toQuest("custom-5")

        assertEquals(
            mapOf(StatKind.HUNGER to 20, StatKind.HEALTH to 10, StatKind.PLEASURE to -5),
            quest.node("s1")!!.options[0].statEffects
        )
        assertEquals(mapOf(StatKind.HUNGER to -10), quest.node("s1")!!.options[1].statEffects)
        val decoded = CustomQuestsCodec.decode(CustomQuestsCodec.encode(listOf(quest))).single()
        assertEquals(quest, decoded)
        assertEquals(meal, CustomQuestDraft.of(decoded))
    }

    @Test
    fun `every bar is checked against its limits`() {
        fun withOption(option: CustomQuestOptionDraft) = cleaning.copy(
            steps = listOf(cleaning.steps[0].copy(options = listOf(option, cleaning.steps[0].options[1])))
        )
        val ok = cleaning.steps[0].options[0]

        assertTrue(withOption(ok.copy(hungerDelta = CustomQuests.STAT_MAX + 5)).validate().any { it.contains("сытость") })
        assertTrue(withOption(ok.copy(healthDelta = 3)).validate().any { it.contains("здоровье") })
        assertTrue(withOption(ok.copy(moodDelta = CustomQuests.STAT_MIN - 5)).validate().any { it.contains("настроение") })
        assertEquals(emptyList<String>(), withOption(ok.copy(hungerDelta = -20, healthDelta = 20)).validate())
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
            good.replace("custom-1", "custom-2").replace("\u001F10\u001F", "\u001Fдесять\u001F")
        ).joinToString("\u001E")

        val decoded = CustomQuestsCodec.decode(raw)

        assertEquals(listOf("custom-1"), decoded.map { it.id })
        assertEquals(emptyList<Quest>(), CustomQuestsCodec.decode(null))
        assertEquals(emptyList<Quest>(), CustomQuestsCodec.decode(""))
    }

    @Test
    fun `the codec keeps the rules of the quest`() {
        val oneTime = cleaning.copy(repeatable = false, stageDelayMinutes = 90, requiresAdultCheck = true)
            .toQuest("custom-1")
        val slow = cleaning.copy(cooldownMinutes = 1440, stageDelayMinutes = 0, topic = QuestTopic.SAVING)
            .toQuest("custom-2")

        val decoded = CustomQuestsCodec.decode(CustomQuestsCodec.encode(listOf(oneTime, slow)))

        assertEquals(listOf(oneTime, slow), decoded)
        assertFalse(decoded[0].repeatable)
        assertTrue(decoded[0].requiresAdultCheck)
        assertFalse(decoded[1].requiresAdultCheck)
        assertEquals(90, decoded[0].stageDelayMinutes)
        assertEquals(1440, decoded[1].cooldownMinutes)
        assertEquals(QuestTopic.SAVING, decoded[1].topic)
        assertNull(decoded[0].topic)
    }

    @Test
    fun `a record of the first version is read and migrated`() {
        // Запись до правил квеста: у каждого шага своя задержка, у варианта — только настроение.
        val legacy = listOf(
            "custom-7", "Уборка", "Помоги дома", "10", "2",
            "Комната в беспорядке", "30", "2",
            "Убрать сейчас", "Чисто", "0", "0", "25",
            "Потом", "Беспорядок", "0", "0", "0",
            "Мама предлагает 20 монет", "5", "2",
            "Помочь", "Мама довольна", "20", "0", "50",
            "Отказаться", "Грустно", "0", "-5", "0"
        ).joinToString("\u001F")

        val quest = CustomQuestsCodec.decode(legacy).single()

        assertEquals("custom-7", quest.id)
        assertEquals(listOf("s1", "s2"), quest.stepOrder)
        assertTrue(quest.repeatable)
        assertEquals(Quest.DEFAULT_COOLDOWN_MINUTES, quest.cooldownMinutes)
        // Пауза — задержка шагов, после которых что-то ещё есть; у последнего она ни на что не влияла.
        assertEquals(30, quest.stageDelayMinutes)
        assertEquals(mapOf(StatKind.PLEASURE to -5), quest.node("s2")!!.options[1].statEffects)
        // Перезапись идёт уже в нынешнем виде и читается так же.
        assertEquals(listOf(quest), CustomQuestsCodec.decode(CustomQuestsCodec.encode(listOf(quest))))
    }

    @Test
    fun `presets step through the row and stop at its ends`() {
        val row = listOf(0, 5, 30)

        assertEquals(5, CustomQuests.nextPreset(row, 0))
        assertEquals(30, CustomQuests.nextPreset(row, 7))
        assertEquals(30, CustomQuests.nextPreset(row, 30))
        assertEquals(5, CustomQuests.previousPreset(row, 7))
        assertEquals(0, CustomQuests.previousPreset(row, 0))
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
