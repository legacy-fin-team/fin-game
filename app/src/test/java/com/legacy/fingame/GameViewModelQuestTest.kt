package com.legacy.fingame

import com.legacy.fingame.game.GameViewModel
import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.Screen
import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.game.economy.BudgetState
import com.legacy.fingame.game.quests.Quest
import com.legacy.fingame.game.quests.QuestProgress
import com.legacy.fingame.game.quests.QuestStatus
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.game.stats.StatKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Квесты в самой игре: деньги и полоски питомца после выбора, ожидание по игровым часам,
 * случайные квесты по тикам, точка на кнопке и сохранение между запусками.
 */
class GameViewModelQuestTest {

    private val start = FakeGameClock.DEFAULT_MILLIS
    private val minute = TimeUnit.MINUTES.toMillis(1)
    private val sixHours = TimeUnit.HOURS.toMillis(6)
    private val halfStats = PetStats(StatKind.entries.associateWith { 50 })

    /**
     * @return Хранилище игры, в которой питомец заведён в [start], полоски наполовину пусты, а на
     * счёте [balance] монет.
     */
    private fun storeWith(
        balance: Int = 150,
        quests: List<QuestProgress> = emptyList(),
        lastRandomQuestAtMillis: Long = PlayerState.NO_RANDOM_QUEST,
        budget: BudgetState? = null
    ) = FakePlayerStateStore(
        PlayerState(
            selection = AnimalSelection(animalId = "cat", variantId = "white"),
            balance = balance,
            budget = budget,
            stats = halfStats,
            statsUpdatedAtMillis = start,
            petBornAtMillis = start,
            quests = quests,
            lastRandomQuestAtMillis = lastRandomQuestAtMillis
        )
    )

    private fun vmOver(
        store: FakePlayerStateStore,
        clock: FakeGameClock = FakeGameClock(),
        random: Random = ScriptedRandom()
    ): GameViewModel = testGameViewModel(
        store = store,
        clock = clock,
        questCatalog = TestQuests.CATALOG,
        random = random
    )

    // --- Взять ---

    @Test
    fun `a quest cannot be taken without its minimum`() {
        val vm = vmOver(storeWith(balance = 99))

        assertFalse(vm.startQuest("picnic"))
        assertEquals(emptyList<QuestProgress>(), vm.state.value.quests)
        assertEquals(99, vm.state.value.balance)
    }

    @Test
    fun `taking a quest keeps the minimum on the balance and is saved`() {
        val store = storeWith(balance = 150)
        val vm = vmOver(store)

        assertTrue(vm.startQuest("picnic"))

        assertEquals(150, vm.state.value.balance)
        assertEquals(listOf(QuestProgress("picnic", "food", start)), vm.state.value.quests)
        assertEquals(vm.state.value.quests, store.state.quests)
    }

    @Test
    fun `a running quest cannot be taken again`() {
        val vm = vmOver(storeWith())
        assertTrue(vm.startQuest("picnic"))

        assertFalse(vm.startQuest("picnic"))
        assertFalse(vm.restartQuest("picnic"))
        assertEquals(1, vm.state.value.quests.size)
    }

    @Test
    fun `a random quest cannot be taken by hand`() {
        val vm = vmOver(storeWith())

        assertFalse(vm.startQuest("lost_wallet"))
        assertFalse(vm.startQuest("nowhere"))
        assertEquals(emptyList<QuestProgress>(), vm.state.value.quests)
    }

    // --- Выбрать ---

    @Test
    fun `a choice moves the stats, the money through the log and the progress, not the budget`() {
        val budget = BudgetState(
            plannedMust = 50,
            plannedWant = 50,
            plannedSavings = 50,
            plannedDeposit = 0,
            spentMust = 0,
            spentWant = 0,
            startDay = FakeGameClock.DEFAULT_DAY
        )
        val store = storeWith(balance = 150, budget = budget)
        val vm = vmOver(store)
        vm.startQuest("picnic")

        assertTrue(vm.chooseQuestOption("picnic", 0))

        val state = vm.state.value
        assertEquals(70, state.stats[StatKind.HUNGER])
        assertEquals(55, state.stats[StatKind.HEALTH])
        assertEquals(50, state.stats[StatKind.PLEASURE])
        assertEquals(110, state.balance)
        val entry = state.moneyLog.entries.first()
        assertEquals("Квест: Пикник", entry.reason)
        assertEquals(-40, entry.delta)
        assertEquals(budget, state.budget)
        assertEquals(60, state.questProgressOf("picnic")!!.progress)
        assertEquals(110, store.state.balance)
    }

    @Test
    fun `the balance never goes below zero`() {
        val vm = vmOver(storeWith(balance = 30))
        vm.startQuest("ice_cream")

        assertTrue(vm.chooseQuestOption("ice_cream", 0))

        assertEquals(0, vm.state.value.balance)
        val entry = vm.state.value.moneyLog.entries.first()
        assertEquals("Квест: Мороженое", entry.reason)
        assertEquals(-30, entry.delta)
        assertEquals(-30, vm.state.value.questProgressOf("ice_cream")!!.lastChoice!!.moneyDelta)
    }

    @Test
    fun `income from a quest is logged too`() {
        val vm = vmOver(storeWith(balance = 0))
        vm.startQuest("piggy_bank")

        vm.chooseQuestOption("piggy_bank", 0)

        assertEquals(30, vm.state.value.balance)
        assertEquals("Квест: Копилка", vm.state.value.moneyLog.entries.first().reason)
    }

    @Test
    fun `a second tap on an option changes nothing`() {
        val vm = vmOver(storeWith(balance = 150))
        vm.startQuest("picnic")
        vm.chooseQuestOption("picnic", 0)

        assertFalse(vm.chooseQuestOption("picnic", 1))
        assertEquals(110, vm.state.value.balance)
    }

    // --- Ожидание по часам ---

    @Test
    fun `the next step waits for the game clock`() {
        val clock = FakeGameClock()
        val vm = vmOver(storeWith(), clock)
        vm.startQuest("picnic")
        vm.chooseQuestOption("picnic", 0)

        clock.millis = start + minute - 1
        assertFalse(vm.advanceQuest("picnic"))

        clock.millis = start + minute
        assertTrue(vm.advanceQuest("picnic"))
        assertEquals("games", vm.state.value.questProgressOf("picnic")!!.nodeId)
    }

    @Test
    fun `the demo time button brings the next step closer`() {
        val vm = vmOver(storeWith())
        vm.startQuest("picnic")
        vm.chooseQuestOption("picnic", 0)
        assertFalse(vm.advanceQuest("picnic"))

        vm.fastForward(minute)

        assertTrue(vm.advanceQuest("picnic"))
    }

    @Test
    fun `a device clock moved back does not undo the wait`() {
        val clock = FakeGameClock()
        val vm = vmOver(storeWith(), clock)
        vm.startQuest("picnic")
        vm.chooseQuestOption("picnic", 0)
        val waiting = vm.state.value.questProgressOf("picnic")

        clock.millis = start - TimeUnit.HOURS.toMillis(1)

        assertFalse(vm.advanceQuest("picnic"))
        assertEquals(waiting, vm.state.value.questProgressOf("picnic"))
        assertTrue(vm.nowMillis() >= start)
    }

    @Test
    fun `finishing a quest and playing it again`() {
        val vm = vmOver(storeWith(balance = 150))
        vm.startQuest("piggy_bank")
        vm.chooseQuestOption("piggy_bank", 0)

        assertTrue(vm.advanceQuest("piggy_bank"))
        val finished = vm.state.value.questProgressOf("piggy_bank")!!
        assertEquals(QuestStatus.FINISHED, finished.status)
        assertEquals(Quest.END_NODE, finished.nodeId)

        assertFalse(vm.startQuest("piggy_bank"))
        assertTrue(vm.restartQuest("piggy_bank"))
        assertEquals(QuestProgress("piggy_bank", "start", start), vm.state.value.questProgressOf("piggy_bank"))
        assertEquals(180, vm.state.value.balance)
    }

    @Test
    fun `restart is only for finished quests of the player`() {
        val vm = vmOver(storeWith())

        assertFalse(vm.restartQuest("picnic"))
        assertFalse(vm.restartQuest("lost_wallet"))
    }

    // --- Случайные квесты ---

    @Test
    fun `a random quest comes up after six hours when the roll says so`() {
        val clock = FakeGameClock()
        val store = storeWith(balance = 0)
        val vm = vmOver(store, clock, ScriptedRandom(0, 0))

        clock.millis = start + sixHours
        vm.tick()

        val state = vm.state.value
        assertEquals(listOf(QuestProgress("lost_wallet", "found", start + sixHours)), state.quests)
        assertEquals(start + sixHours, state.lastRandomQuestAtMillis)
        assertTrue(state.hasUnseenQuestStep)
        assertEquals(state.quests, store.state.quests)
        assertEquals(start + sixHours, store.state.lastRandomQuestAtMillis)
    }

    @Test
    fun `nothing comes up before six hours, and the dice are not even rolled`() {
        val clock = FakeGameClock()
        val vm = vmOver(storeWith(), clock, ScriptedRandom())

        clock.millis = start + sixHours - 1
        vm.tick()

        assertEquals(emptyList<QuestProgress>(), vm.state.value.quests)
        assertFalse(vm.state.value.hasUnseenQuestStep)
    }

    @Test
    fun `an unlucky roll brings nothing and the next tick tries again`() {
        val clock = FakeGameClock()
        val vm = vmOver(storeWith(balance = 0), clock, ScriptedRandom(3, 0, 1))

        clock.millis = start + sixHours
        vm.tick()
        assertEquals(emptyList<QuestProgress>(), vm.state.value.quests)

        clock.millis += TimeUnit.SECONDS.toMillis(30)
        vm.tick()
        assertEquals(listOf("guests"), vm.state.value.quests.map { it.questId })
    }

    @Test
    fun `no second random quest while one is running`() {
        val clock = FakeGameClock()
        val running = listOf(QuestProgress("lost_wallet", "found", start))
        val vm = vmOver(
            storeWith(quests = running, lastRandomQuestAtMillis = start),
            clock,
            ScriptedRandom()
        )

        clock.millis = start + TimeUnit.HOURS.toMillis(7)
        vm.tick()

        assertEquals(running, vm.state.value.quests)
    }

    @Test
    fun `the demo time button can bring a random quest`() {
        val vm = vmOver(storeWith(balance = 0), random = ScriptedRandom(0, 0))

        vm.fastForward(TimeUnit.HOURS.toMillis(12))

        assertEquals(listOf("lost_wallet"), vm.state.value.quests.map { it.questId })
    }

    // --- Точка на кнопке ---

    @Test
    fun `opening the quests screen clears the badge and remembers the look`() {
        val clock = FakeGameClock()
        val store = storeWith(balance = 0)
        val vm = vmOver(store, clock, ScriptedRandom(0, 0))
        clock.millis = start + sixHours
        vm.tick()
        assertTrue(vm.state.value.hasUnseenQuestStep)

        vm.openScreen(Screen.QUESTS)

        assertFalse(vm.state.value.hasUnseenQuestStep)
        assertEquals(start + sixHours, store.state.questsSeenAtMillis)
    }

    @Test
    fun `the badge comes back when the wait is over`() {
        val clock = FakeGameClock()
        val vm = vmOver(storeWith(), clock)
        vm.openScreen(Screen.QUESTS)
        vm.startQuest("picnic")
        vm.chooseQuestOption("picnic", 0)
        vm.closeScreen()
        assertFalse(vm.state.value.hasUnseenQuestStep)

        clock.millis = start + minute
        vm.tick()

        assertTrue(vm.state.value.hasUnseenQuestStep)
    }

    @Test
    fun `while the quests screen is open a new step is seen right away`() {
        val clock = FakeGameClock()
        val store = storeWith()
        val vm = vmOver(store, clock)
        vm.openScreen(Screen.QUESTS)
        vm.startQuest("picnic")
        vm.chooseQuestOption("picnic", 0)

        clock.millis = start + minute
        vm.tick()

        assertFalse(vm.state.value.hasUnseenQuestStep)
        assertEquals(start + minute, store.state.questsSeenAtMillis)
    }

    // --- Перезапуск ---

    @Test
    fun `quests survive a restart`() {
        val clock = FakeGameClock()
        val store = storeWith()
        val first = vmOver(store, clock)
        first.startQuest("picnic")
        first.chooseQuestOption("picnic", 0)

        val second = vmOver(store, clock)

        assertEquals(first.state.value.quests, second.state.value.quests)
        assertNotNull(second.state.value.questProgressOf("picnic")!!.lastChoice)
    }

    @Test
    fun `a saved quest the data no longer has is dropped`() {
        val saved = listOf(
            QuestProgress("gone", "somewhere", start),
            QuestProgress("picnic", "food", start)
        )

        val vm = vmOver(storeWith(quests = saved))

        assertEquals(listOf("picnic"), vm.state.value.quests.map { it.questId })
        assertNull(vm.state.value.questProgressOf("gone"))
    }

    @Test
    fun `a saved quest whose step vanished is finished on load`() {
        val saved = listOf(QuestProgress("picnic", "vanished", start))

        val vm = vmOver(storeWith(quests = saved))

        val progress = vm.state.value.questProgressOf("picnic")!!
        assertEquals(QuestStatus.FINISHED, progress.status)
        assertEquals(Quest.END_NODE, progress.nodeId)
        assertTrue(vm.restartQuest("picnic"))
    }
}
