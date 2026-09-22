package com.legacy.fingame

import com.legacy.fingame.game.GameUiState
import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.Screen
import com.legacy.fingame.game.economy.BudgetState
import com.legacy.fingame.game.economy.Economy
import com.legacy.fingame.game.economy.MoneyLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Круг игрового периода: бонус дня, планирование, подтверждение, траты и следующий бонус. */
class BudgetFlowTest {

    @Test
    fun `taking the daily bonus opens the planning and the screen for it`() {
        val vm = testGameViewModel()

        assertFalse(vm.state.value.planningOpen)
        assertTrue(vm.claimDailyBonus())

        val state = vm.state.value
        assertTrue(state.planningOpen)
        assertEquals(Screen.BUDGET, state.screen)
        assertEquals(Economy.STARTING_BALANCE + Economy.DAILY_BONUS, state.balance)
    }

    @Test
    fun `the bonus is written into the log`() {
        val vm = testGameViewModel()

        vm.claimDailyBonus()

        val entry = vm.state.value.moneyLog.entries.first()
        assertEquals(MoneyLog.REASON_DAILY_BONUS, entry.reason)
        assertEquals(Economy.DAILY_BONUS, entry.delta)
        assertEquals(FakeGameClock.DEFAULT_DAY, entry.gameDay)
    }

    @Test
    fun `a bonus that is not there changes nothing`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(clock = clock)
        assertTrue(vm.claimDailyBonus())
        vm.closeScreen()

        assertFalse(vm.claimDailyBonus())
        assertEquals(Screen.MAIN, vm.state.value.screen)
        assertEquals(1, vm.state.value.moneyLog.entries.size)
    }

    @Test
    fun `the running period is closed into the previous result by the next bonus`() {
        val clock = FakeGameClock()
        val store = FakePlayerStateStore(
            PlayerState(
                budget = BudgetState(
                    planned = 300,
                    plannedSavings = 100,
                    plannedDeposit = 0,
                    spent = 180,
                    startDay = FakeGameClock.DEFAULT_DAY - 1
                )
            )
        )
        val vm = testGameViewModel(store = store, clock = clock)

        assertTrue(vm.claimDailyBonus())

        val state = vm.state.value
        assertNull(state.budget)
        assertEquals(300, state.previousBudgetResult?.planned)
        assertEquals(180, state.previousBudgetResult?.actual)
        assertEquals(120, state.previousBudgetResult?.diff)
        assertTrue(state.planningOpen)
    }

    @Test
    fun `everything the bonus changed is there on the next launch`() {
        val store = FakePlayerStateStore()
        val clock = FakeGameClock()

        testGameViewModel(store = store, clock = clock).claimDailyBonus()
        val nextRun = testGameViewModel(store = store, clock = clock)

        val state = nextRun.state.value
        assertTrue(state.planningOpen)
        assertEquals(1, state.moneyLog.entries.size)
        // Экран не переживает перезапуск: игрок возвращается на главный, как и раньше.
        assertEquals(Screen.MAIN, state.screen)
    }

    @Test
    fun `earned coins name their reason in the log`() {
        val vm = testGameViewModel()

        vm.earn(120)

        assertEquals(MoneyLog.REASON_REWARD, vm.state.value.moneyLog.entries.first().reason)
        assertEquals(120, vm.state.value.moneyLog.entries.first().delta)
    }

    @Test
    fun `claiming the bonus saves the planning and the bonus entry into the store`() {
        val store = FakePlayerStateStore()
        val vm = testGameViewModel(store = store)

        vm.claimDailyBonus()

        assertTrue(store.state.planningOpen)
        val entry = store.state.moneyLog.entries.first()
        assertEquals(MoneyLog.REASON_DAILY_BONUS, entry.reason)
        assertEquals(Economy.DAILY_BONUS, entry.delta)
    }

    @Test
    fun `planning is open to a player who never confirmed a budget, even before any bonus`() {
        val fresh = GameUiState()

        assertFalse(fresh.planningOpen)
        assertTrue(fresh.canPlanBudget)

        val confirmed = fresh.copy(
            budget = BudgetState(
                planned = 100,
                plannedSavings = 0,
                plannedDeposit = 0,
                spent = 0,
                startDay = FakeGameClock.DEFAULT_DAY
            ),
            planningOpen = false
        )

        assertFalse(confirmed.canPlanBudget)
    }
}
