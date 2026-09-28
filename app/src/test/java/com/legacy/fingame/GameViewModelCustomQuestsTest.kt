package com.legacy.fingame

import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.game.quests.CustomQuestDraft
import com.legacy.fingame.game.quests.CustomQuestOptionDraft
import com.legacy.fingame.game.quests.CustomQuestStepDraft
import com.legacy.fingame.game.quests.QuestBoard
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.game.stats.StatKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Свои квесты взрослого в модели: добавление, прохождение ребёнком, удаление. */
class GameViewModelCustomQuestsTest {

    private val clock = FakeGameClock()

    private val cleaning = CustomQuestDraft(
        title = "Уборка",
        description = "Помоги дома",
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
        stageDelayMinutes = 5
    )

    private fun store(balance: Int = 100) = FakePlayerStateStore(
        PlayerState(
            selection = AnimalSelection(animalId = "cat", variantId = "white"),
            balance = balance,
            stats = PetStats(StatKind.entries.associateWith { 50 }),
            statsUpdatedAtMillis = clock.millis,
            petBornAtMillis = clock.millis
        )
    )

    @Test
    fun `only the adult can add or remove a quest`() {
        val vm = testGameViewModel(store = store(), clock = clock)

        assertFalse(vm.addCustomQuest(cleaning))
        assertEquals(emptyList<Any>(), vm.state.value.customQuests)

        vm.enterAdultMode()
        assertTrue(vm.addCustomQuest(cleaning))
        val id = vm.state.value.customQuests.single().id
        vm.exitAdultMode()
        assertFalse(vm.removeCustomQuest(id))
        assertEquals(1, vm.state.value.customQuests.size)
    }

    @Test
    fun `an invalid draft is not added`() {
        val vm = testGameViewModel(store = store(), clock = clock)
        vm.enterAdultMode()

        assertFalse(vm.addCustomQuest(cleaning.copy(title = "")))
        assertEquals(emptyList<Any>(), vm.state.value.customQuests)
    }

    @Test
    fun `an added quest is on the board, saved and restored`() {
        val store = store()
        val vm = testGameViewModel(store = store, clock = clock)
        vm.enterAdultMode()

        assertTrue(vm.addCustomQuest(cleaning))
        assertTrue(vm.addCustomQuest(cleaning.copy(title = "Ещё")))
        val ids = vm.state.value.customQuests.map { it.id }
        assertEquals(2, ids.distinct().size)
        assertTrue(ids.all { it.startsWith("custom-") })

        val board = QuestBoard.entriesOf(vm.questCatalog, vm.state.value.quests)
        assertEquals(listOf("Уборка", "Ещё"), board.map { it.quest.title })
        assertEquals(ids, store.state.customQuests.map { it.id })

        val restored = testGameViewModel(store = store, clock = clock)
        assertEquals(ids, restored.state.value.customQuests.map { it.id })
        assertNotNull(restored.questCatalog.findQuestById(ids[0]))
    }

    @Test
    fun `the child takes and finishes the custom quest`() {
        val store = store()
        val vm = testGameViewModel(store = store, clock = clock)
        vm.enterAdultMode()
        vm.addCustomQuest(cleaning)
        vm.exitAdultMode()
        val id = vm.state.value.customQuests.single().id

        assertTrue(vm.startQuest(id))
        assertTrue(vm.chooseQuestOption(id, 0))
        assertEquals(25, vm.state.value.questProgressOf(id)?.progress)
        // Задержка 5 минут: раньше дальше не пускает.
        assertFalse(vm.advanceQuest(id))
        clock.millis += TimeUnit.MINUTES.toMillis(5)
        assertTrue(vm.advanceQuest(id))
        assertEquals("s2", vm.state.value.questProgressOf(id)?.nodeId)

        assertTrue(vm.chooseQuestOption(id, 0))
        assertTrue(vm.advanceQuest(id))
        val progress = vm.state.value.questProgressOf(id)!!
        assertTrue(progress.isFinished)
        assertEquals(75, progress.progress)
        assertEquals(120, vm.state.value.balance)
        assertEquals(2, vm.state.value.questLog.entries.size)
    }

    @Test
    fun `a custom quest feeds and heals the pet`() {
        val vm = testGameViewModel(store = store(), clock = clock)
        val meal = cleaning.copy(
            steps = listOf(
                CustomQuestStepDraft(
                    text = "Время обеда",
                    options = listOf(
                        CustomQuestOptionDraft("Суп", "Сытно", hungerDelta = 20, healthDelta = 10),
                        CustomQuestOptionDraft("Конфеты", "Сладко", healthDelta = -10, moodDelta = 5)
                    )
                )
            )
        )
        vm.enterAdultMode()
        assertTrue(vm.addCustomQuest(meal))
        vm.exitAdultMode()
        val id = vm.state.value.customQuests.single().id

        vm.startQuest(id)
        assertTrue(vm.chooseQuestOption(id, 0))

        val stats = vm.state.value.stats
        assertEquals(70, stats[StatKind.HUNGER])
        assertEquals(60, stats[StatKind.HEALTH])
        assertEquals(50, stats[StatKind.PLEASURE])
    }

    @Test
    fun `removing a quest drops its run but keeps the choices`() {
        val store = store()
        val vm = testGameViewModel(store = store, clock = clock)
        vm.enterAdultMode()
        vm.addCustomQuest(cleaning)
        vm.exitAdultMode()
        val id = vm.state.value.customQuests.single().id
        vm.startQuest(id)
        vm.chooseQuestOption(id, 1)

        vm.enterAdultMode()
        assertTrue(vm.removeCustomQuest(id))
        vm.exitAdultMode()

        assertNull(vm.state.value.questProgressOf(id))
        assertNull(vm.questCatalog.findQuestById(id))
        assertEquals(1, vm.state.value.questLog.entries.size)
        assertEquals(emptyList<Any>(), store.state.customQuests)
        assertEquals(emptyList<Any>(), store.state.quests)
        assertFalse(vm.startQuest(id))
        assertEquals(emptyList<Any>(), QuestBoard.entriesOf(vm.questCatalog, vm.state.value.quests))
    }

    @Test
    fun `a new game keeps the custom quests`() {
        val vm = testGameViewModel(store = store(), clock = clock)
        vm.enterAdultMode()
        vm.addCustomQuest(cleaning)
        vm.exitAdultMode()

        vm.resetProgress()

        assertEquals(1, vm.state.value.customQuests.size)
    }
}
