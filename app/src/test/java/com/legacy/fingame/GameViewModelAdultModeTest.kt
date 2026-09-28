package com.legacy.fingame

import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.Screen
import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.game.economy.BudgetState
import com.legacy.fingame.game.items.ItemSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Режим взрослого: вход и выход, и что взрослый ничего не меняет в игре ребёнка. */
class GameViewModelAdultModeTest {

    private val apple = ItemSelection(TestItems.APPLE.id, "red")
    private val hat = ItemSelection(TestItems.HAT.id, TestItems.HAT.defaultVariantId)

    private fun playingStore(
        balance: Int = 500,
        owned: Map<ItemSelection, Int> = emptyMap(),
        budget: BudgetState? = null,
        lastDailyBonusDay: Long = FakeGameClock.DEFAULT_DAY - 1
    ): FakePlayerStateStore {
        val clock = FakeGameClock()
        return FakePlayerStateStore(
            PlayerState(
                selection = AnimalSelection(animalId = "cat", variantId = "white"),
                balance = balance,
                owned = owned,
                budget = budget,
                lastDailyBonusDay = lastDailyBonusDay,
                statsUpdatedAtMillis = clock.millis,
                petBornAtMillis = clock.millis
            )
        )
    }

    @Test
    fun `entering and leaving adult mode`() {
        val vm = testGameViewModel(store = playingStore())
        vm.openScreen(Screen.OPTIONS)
        vm.openScreen(Screen.ADULT_LOCK)

        vm.enterAdultMode()
        assertTrue(vm.state.value.adultMode)
        assertEquals(Screen.ADULT_MODE, vm.state.value.screen)

        vm.exitAdultMode()
        assertFalse(vm.state.value.adultMode)
        assertEquals(Screen.OPTIONS, vm.state.value.screen)
    }

    @Test
    fun `closing the adult screen goes back to the settings and leaves adult mode`() {
        val vm = testGameViewModel(store = playingStore())
        vm.enterAdultMode()

        vm.closeScreen()

        assertFalse(vm.state.value.adultMode)
        assertEquals(Screen.OPTIONS, vm.state.value.screen)
    }

    @Test
    fun `closing the lock goes back to the settings`() {
        val vm = testGameViewModel(store = playingStore())
        vm.openScreen(Screen.OPTIONS)
        vm.openScreen(Screen.ADULT_LOCK)

        vm.closeScreen()

        assertEquals(Screen.OPTIONS, vm.state.value.screen)
        assertFalse(vm.state.value.adultMode)
    }

    @Test
    fun `navigating away from the adult screen leaves adult mode`() {
        val vm = testGameViewModel(store = playingStore())
        vm.enterAdultMode()

        vm.openScreen(Screen.MAIN)

        assertFalse(vm.state.value.adultMode)
    }

    @Test
    fun `adult mode is not saved`() {
        val store = playingStore()
        val vm = testGameViewModel(store = store)
        vm.enterAdultMode()
        vm.earn(10)

        assertFalse(testGameViewModel(store = store).state.value.adultMode)
    }

    @Test
    fun `an adult cannot buy`() {
        val store = playingStore()
        val vm = testGameViewModel(store = store)
        vm.increaseQty(TestItems.APPLE.id)
        vm.enterAdultMode()
        val before = vm.state.value

        assertFalse(vm.buyCart())

        assertEquals(before.balance, vm.state.value.balance)
        assertEquals(before.owned, vm.state.value.owned)
    }

    @Test
    fun `an adult cannot use or wear items`() {
        val vm = testGameViewModel(store = playingStore(owned = mapOf(apple to 2, hat to 1)))
        vm.enterAdultMode()

        assertFalse(vm.useItem(apple))
        assertFalse(vm.toggleWorn(hat))

        assertEquals(2, vm.state.value.owned[apple])
        assertEquals(emptySet<ItemSelection>(), vm.state.value.worn)
    }

    @Test
    fun `an adult cannot star goals`() {
        val vm = testGameViewModel(store = playingStore())
        vm.enterAdultMode()

        assertFalse(vm.toggleGoal(hat))

        assertEquals(emptyList<ItemSelection>(), vm.state.value.goals)
    }

    @Test
    fun `an adult cannot take or play quests`() {
        val vm = testGameViewModel(store = playingStore(), questCatalog = TestQuests.CATALOG)
        assertTrue(vm.startQuest(TestQuests.PICNIC.id))
        vm.enterAdultMode()
        val quests = vm.state.value.quests

        assertFalse(vm.startQuest(TestQuests.PIGGY_BANK.id))
        assertFalse(vm.chooseQuestOption(TestQuests.PICNIC.id, 0))
        assertFalse(vm.advanceQuest(TestQuests.PICNIC.id))
        assertFalse(vm.restartQuest(TestQuests.PICNIC.id))

        assertEquals(quests, vm.state.value.quests)
        assertTrue(vm.state.value.questLog.entries.isEmpty())
    }

    @Test
    fun `an adult cannot claim the bonus or touch the budget`() {
        val store = playingStore(balance = 100)
        val vm = testGameViewModel(store = store)
        vm.enterAdultMode()

        assertFalse(vm.claimDailyBonus())
        assertFalse(vm.confirmBudget())

        assertEquals(100, vm.state.value.balance)
        assertEquals(null, vm.state.value.budget)
        assertEquals(Screen.ADULT_MODE, vm.state.value.screen)
    }
}
