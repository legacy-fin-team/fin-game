package com.legacy.fingame

import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.game.quests.QuestCatalog
import com.legacy.fingame.game.quests.QuestCheck
import com.legacy.fingame.game.quests.QuestCheckEvent
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.game.stats.StatKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Квест с проверкой взрослым в модели: сдача, «Засчитать», «Не засчитано», история и сохранение. */
class GameViewModelAdultCheckTest {

    private val clock = FakeGameClock()
    private val checked = TestQuests.PICNIC.copy(requiresAdultCheck = true)

    private fun store() = FakePlayerStateStore(
        PlayerState(
            selection = AnimalSelection(animalId = "cat", variantId = "white"),
            balance = 150,
            stats = PetStats(StatKind.entries.associateWith { 50 }),
            statsUpdatedAtMillis = clock.millis,
            petBornAtMillis = clock.millis
        )
    )

    private fun vmOver(store: FakePlayerStateStore) =
        testGameViewModel(store = store, clock = clock, questCatalog = QuestCatalog.of(listOf(checked)))

    @Test
    fun `the child's choice waits for the adult and pays nothing yet`() {
        val store = store()
        val vm = vmOver(store)
        vm.startQuest("picnic")

        assertTrue(vm.chooseQuestOption("picnic", 0))

        val state = vm.state.value
        assertEquals(150, state.balance)
        assertEquals(50, state.stats[StatKind.HUNGER])
        assertEquals(QuestCheck.WAITING, state.questProgressOf("picnic")?.check)
        assertFalse(vm.advanceQuest("picnic"))
        val sent = state.questLog.entries.single()
        assertEquals(QuestCheckEvent.SENT, sent.check)
        assertEquals(0, sent.moneyDelta)
        // Ожидание проверки переживает перезапуск.
        assertEquals(QuestCheck.WAITING, vmOver(store).state.value.questProgressOf("picnic")?.check)
    }

    @Test
    fun `only the adult approves, and the reward comes with the approval`() {
        val vm = vmOver(store())
        vm.startQuest("picnic")
        vm.chooseQuestOption("picnic", 0)

        assertFalse(vm.approveQuestCheck("picnic"))
        vm.enterAdultMode()
        assertTrue(vm.approveQuestCheck("picnic"))
        assertFalse(vm.approveQuestCheck("picnic"))
        vm.exitAdultMode()

        val state = vm.state.value
        assertEquals(110, state.balance)
        assertEquals(70, state.stats[StatKind.HUNGER])
        assertEquals(60, state.questProgressOf("picnic")?.progress)
        assertEquals(QuestCheck.NONE, state.questProgressOf("picnic")?.check)
        val approved = state.questLog.entries.first()
        assertEquals(QuestCheckEvent.APPROVED, approved.check)
        assertEquals(-40, approved.moneyDelta)
        assertEquals(-40, state.moneyLog.entries.first().delta)
        // Дальше — как обычно, после задержки узла.
        clock.millis += TestQuests.PICNIC.node("food")!!.delayMinutes * 60_000L
        assertTrue(vm.advanceQuest("picnic"))
        assertEquals("games", vm.state.value.questProgressOf("picnic")?.nodeId)
    }

    @Test
    fun `a rejected step goes back to work without a reward`() {
        val vm = vmOver(store())
        vm.startQuest("picnic")
        vm.chooseQuestOption("picnic", 0)

        assertFalse(vm.rejectQuestCheck("picnic"))
        clock.millis += 60_000L
        vm.enterAdultMode()
        assertTrue(vm.rejectQuestCheck("picnic"))
        vm.exitAdultMode()

        val state = vm.state.value
        assertEquals(150, state.balance)
        val progress = state.questProgressOf("picnic")!!
        assertEquals(QuestCheck.REJECTED, progress.check)
        assertNull(progress.lastChoice)
        assertEquals(QuestCheckEvent.REJECTED, state.questLog.entries.first().check)
        assertTrue(state.hasUnseenQuestStep)
        assertTrue(vm.chooseQuestOption("picnic", 1))
        assertEquals(QuestCheck.WAITING, vm.state.value.questProgressOf("picnic")?.check)
    }
}
