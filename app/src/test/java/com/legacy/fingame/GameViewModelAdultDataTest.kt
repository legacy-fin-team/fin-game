package com.legacy.fingame

import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.game.economy.BudgetHistory
import com.legacy.fingame.game.economy.BudgetResult
import com.legacy.fingame.game.economy.BudgetState
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.game.economy.SpendKind
import com.legacy.fingame.game.quests.QuestChoice
import com.legacy.fingame.game.quests.QuestLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Что игра запоминает для взрослого режима: покупки с товаром, итоги периодов и выборы в квестах. */
class GameViewModelAdultDataTest {

    private fun runningBudget(startDay: Long) = BudgetState(
        plannedMust = 200,
        plannedWant = 100,
        plannedSavings = 100,
        plannedDeposit = 0,
        spentMust = 230,
        spentWant = 40,
        startDay = startDay
    )

    private fun oldResult(startDay: Long) = BudgetResult(
        plannedMust = 1,
        actualMust = 1,
        plannedWant = 1,
        actualWant = 1,
        plannedSavings = 1,
        actualSavings = 1,
        plannedDeposit = 0,
        startDay = startDay
    )

    @Test
    fun `a purchase is logged with the item id and quantity`() {
        val store = FakePlayerStateStore()
        val vm = testGameViewModel(store = store)
        vm.pickVariant(TestItems.APPLE.id, "green")
        vm.increaseQty(TestItems.APPLE.id)
        vm.increaseQty(TestItems.APPLE.id)
        vm.increaseQty(TestItems.HAT.id)
        val balance = vm.state.value.balance

        assertTrue(vm.buyCart())

        val purchases = vm.state.value.moneyLog.entries.filter { it.itemId != null }
        assertEquals(2, purchases.size)
        val apples = purchases.single { it.itemId == TestItems.APPLE.id }
        assertEquals("green", apples.variantId)
        assertEquals(2, apples.quantity)
        assertEquals(SpendKind.MUST, apples.spendKind)
        assertEquals(-TestItems.APPLE.price * 2, apples.delta)
        assertEquals(MoneyLog.purchaseReason(TestItems.APPLE.name, 2), apples.reason)
        val hat = purchases.single { it.itemId == TestItems.HAT.id }
        assertEquals(1, hat.quantity)
        assertEquals(SpendKind.WANT, hat.spendKind)
        // Сумма в журнале — ровно то, что ушло со счёта.
        assertEquals(balance - vm.state.value.balance, -purchases.sumOf { it.delta })
        assertEquals(vm.state.value.moneyLog, store.state.moneyLog)
    }

    @Test
    fun `closing a period appends to the budget history and keeps at most 60`() {
        val today = FakeGameClock.DEFAULT_DAY
        val full = (1..BudgetHistory.MAX).map { oldResult(today - 1 - it) }
        val store = FakePlayerStateStore(
            PlayerState(balance = 70, budget = runningBudget(today - 1), budgetHistory = full)
        )
        val vm = testGameViewModel(store = store)

        assertTrue(vm.claimDailyBonus())

        val history = vm.state.value.budgetHistory
        assertEquals(BudgetHistory.MAX, history.size)
        val closed = history.first()
        assertEquals(today - 1, closed.startDay)
        assertEquals(230, closed.actualMust)
        assertEquals(70, closed.actualSavings)
        assertEquals(closed, vm.state.value.previousBudgetResult)
        // Самый старый период ушёл, чтобы уступить место новому.
        assertEquals(today - 1 - (BudgetHistory.MAX - 1), history.last().startDay)
        assertEquals(history, store.state.budgetHistory)
    }

    @Test
    fun `a bonus with no running period adds nothing to the history`() {
        val vm = testGameViewModel()

        assertTrue(vm.claimDailyBonus())

        assertEquals(emptyList<BudgetResult>(), vm.state.value.budgetHistory)
    }

    @Test
    fun `a quest choice is logged`() {
        val clock = FakeGameClock()
        val store = FakePlayerStateStore(
            PlayerState(
                selection = AnimalSelection(animalId = "cat", variantId = "white"),
                balance = 150,
                statsUpdatedAtMillis = clock.millis,
                petBornAtMillis = clock.millis
            )
        )
        val vm = testGameViewModel(store = store, clock = clock, questCatalog = TestQuests.CATALOG)
        assertTrue(vm.startQuest(TestQuests.PICNIC.id))

        assertTrue(vm.chooseQuestOption(TestQuests.PICNIC.id, 0))

        val expected = QuestChoice(
            questId = TestQuests.PICNIC.id,
            nodeId = "food",
            optionLabel = "Фрукты",
            moneyDelta = -40,
            progressDelta = 60,
            gameDay = clock.day,
            timestampMillis = clock.millis
        )
        assertEquals(listOf(expected), vm.state.value.questLog.entries)
        assertEquals(listOf(expected), store.state.questLog.entries)
    }

    @Test
    fun `the histories are there on the next launch`() {
        val store = FakePlayerStateStore(
            PlayerState(
                budgetHistory = listOf(oldResult(FakeGameClock.DEFAULT_DAY - 1)),
                questLog = QuestLog(
                    listOf(QuestChoice("picnic", "food", "Фрукты", -40, 60, 19_000L, 1L))
                )
            )
        )

        val state = testGameViewModel(store = store).state.value

        assertEquals(store.state.budgetHistory, state.budgetHistory)
        assertEquals(store.state.questLog, state.questLog)
    }

    @Test
    fun `reset wipes the history too`() {
        val store = FakePlayerStateStore(
            PlayerState(
                budgetHistory = listOf(oldResult(FakeGameClock.DEFAULT_DAY - 1)),
                questLog = QuestLog(
                    listOf(QuestChoice("picnic", "food", "Фрукты", -40, 60, 19_000L, 1L))
                )
            )
        )
        val vm = testGameViewModel(store = store)

        vm.resetProgress()

        assertEquals(emptyList<BudgetResult>(), vm.state.value.budgetHistory)
        assertEquals(QuestLog.EMPTY, vm.state.value.questLog)
        assertEquals(emptyList<BudgetResult>(), store.state.budgetHistory)
        assertEquals(QuestLog.EMPTY, store.state.questLog)
    }
}
