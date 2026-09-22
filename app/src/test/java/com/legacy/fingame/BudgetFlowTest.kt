package com.legacy.fingame

import com.legacy.fingame.game.GameUiState
import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.Screen
import com.legacy.fingame.game.economy.BudgetDraft
import com.legacy.fingame.game.economy.BudgetResult
import com.legacy.fingame.game.economy.BudgetState
import com.legacy.fingame.game.economy.Deposit
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
    fun `planning is open to everyone without a confirmed budget, even before any bonus`() {
        val fresh = GameUiState()

        assertFalse(fresh.planningOpen)
        assertTrue(fresh.canPlanBudget)

        // Закрытый период сам по себе планировать не мешает: подтверждённого бюджета нет, значит
        // и показывать на экране, кроме раскладки, нечего.
        val afterPeriod = fresh.copy(
            previousBudgetResult = BudgetResult(
                planned = 100,
                plannedSavings = 0,
                plannedDeposit = 0,
                actual = 80
            )
        )

        assertTrue(afterPeriod.canPlanBudget)

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

    @Test
    fun `a saved game without a confirmed budget plans even after its bonus was taken`() {
        val clock = FakeGameClock()
        // Так выглядит старое сохранение: бонус за сегодня уже получен, а планирование в нём
        // никогда не открывалось и бюджет не подтверждался.
        val store = FakePlayerStateStore(
            PlayerState(
                balance = 100,
                savings = 40,
                planningOpen = false,
                lastDailyBonusDay = clock.day
            )
        )
        val vm = testGameViewModel(store = store, clock = clock)

        assertFalse(vm.state.value.dailyBonusAvailable)
        assertTrue(vm.state.value.canPlanBudget)

        vm.updateBudgetDraft(BudgetDraft(savings = 90, depositAmount = 0, depositTermDays = 3))

        assertEquals(90, vm.state.value.planningDraft.savings)
        assertTrue(vm.confirmBudget())

        val state = vm.state.value
        assertEquals(90, state.savings)
        assertEquals(50, state.balance)
        assertEquals(50, state.budget?.planned)
        assertFalse(state.planningOpen)
    }

    @Test
    fun `the draft is normalized and kept between visits to the screen`() {
        val store = FakePlayerStateStore()
        val clock = FakeGameClock()
        val vm = testGameViewModel(store = store, clock = clock)
        vm.claimDailyBonus()
        val total = vm.state.value.totalToPlan

        vm.updateBudgetDraft(
            BudgetDraft(savings = total + 1_000, depositAmount = 50, depositTermDays = 99)
        )

        val draft = vm.state.value.planningDraft
        assertEquals(total, draft.savings)
        assertEquals(0, draft.depositAmount)
        assertEquals(Deposit.MAX_TERM_DAYS, draft.depositTermDays)
        // Закрыли экран, вернулись — раскладка на месте, в том числе после перезапуска.
        vm.closeScreen()
        assertEquals(draft, testGameViewModel(store = store, clock = clock).state.value.planningDraft)
    }

    @Test
    fun `confirming the budget moves the money, opens the deposit and writes the log`() {
        val store = FakePlayerStateStore(PlayerState(balance = 1_000))
        val clock = FakeGameClock()
        val vm = testGameViewModel(store = store, clock = clock)
        vm.claimDailyBonus()

        vm.updateBudgetDraft(
            BudgetDraft(savings = 300, depositAmount = 400, depositTermDays = 5)
        )
        assertTrue(vm.confirmBudget())

        val state = vm.state.value
        assertEquals(1_000 + Economy.DAILY_BONUS - 300 - 400, state.balance)
        assertEquals(300, state.savings)
        assertEquals(400, state.deposit?.amount)
        assertEquals(5, state.deposit?.termDays)
        assertEquals(10, state.deposit?.ratePercent)
        assertEquals(FakeGameClock.DEFAULT_DAY, state.deposit?.openedDay)
        assertEquals(state.balance, state.budget?.planned)
        assertEquals(300, state.budget?.plannedSavings)
        assertEquals(400, state.budget?.plannedDeposit)
        assertEquals(0, state.budget?.spent)
        assertFalse(state.planningOpen)
        assertNull(state.budgetDraft)

        // Журнал: сначала перевод в сбережения, потом открытие вклада, новейшее первым.
        assertEquals(
            listOf(
                MoneyLog.REASON_DEPOSIT_OPENED,
                MoneyLog.REASON_TO_SAVINGS,
                MoneyLog.REASON_DAILY_BONUS
            ),
            state.moneyLog.entries.map { it.reason }
        )
        assertEquals(listOf(-400, -300, Economy.DAILY_BONUS), state.moneyLog.entries.map { it.delta })
    }

    @Test
    fun `a confirmed budget cannot be confirmed again`() {
        val vm = testGameViewModel()
        vm.claimDailyBonus()
        assertTrue(vm.confirmBudget())

        val planned = vm.state.value.budget?.planned
        assertFalse(vm.confirmBudget())
        assertEquals(planned, vm.state.value.budget?.planned)
    }

    @Test
    fun `no deposit is opened while one is already running`() {
        val store = FakePlayerStateStore(
            PlayerState(
                balance = 500,
                deposit = Deposit.openedOn(amount = 200, termDays = 7, day = FakeGameClock.DEFAULT_DAY)
            )
        )
        val vm = testGameViewModel(store = store)
        vm.claimDailyBonus()

        vm.updateBudgetDraft(BudgetDraft(savings = 0, depositAmount = 300, depositTermDays = 3))
        assertTrue(vm.confirmBudget())

        assertEquals(200, vm.state.value.deposit?.amount)
        assertEquals(7, vm.state.value.deposit?.termDays)
        assertEquals(500 + Economy.DAILY_BONUS, vm.state.value.balance)
    }

    @Test
    fun `a draft cannot be edited or confirmed once the planning is over`() {
        val vm = testGameViewModel()
        vm.claimDailyBonus()
        assertTrue(vm.confirmBudget())
        val savings = vm.state.value.savings

        vm.updateBudgetDraft(BudgetDraft(savings = 999, depositAmount = 0, depositTermDays = 2))

        assertNull(vm.state.value.budgetDraft)
        assertEquals(savings, vm.state.value.savings)
    }

    @Test
    fun `a purchase is written into the log and counted as spending`() {
        val vm = testGameViewModel()
        vm.claimDailyBonus()
        assertTrue(vm.confirmBudget())
        val planned = vm.state.value.budget?.planned ?: 0

        vm.increaseQty(TestItems.APPLE.id)
        vm.increaseQty(TestItems.APPLE.id)
        assertTrue(vm.buyCart())

        val state = vm.state.value
        val spent = TestItems.APPLE.price * 2
        assertEquals(spent, state.budget?.spent)
        assertEquals(planned - spent, state.balance)
        assertEquals(
            MoneyLog.purchaseReason(TestItems.APPLE.name, 2),
            state.moneyLog.entries.first().reason
        )
        assertEquals(-spent, state.moneyLog.entries.first().delta)
    }

    @Test
    fun `moving money after the budget was confirmed leaves the plan alone`() {
        val store = FakePlayerStateStore(PlayerState(balance = 500))
        val vm = testGameViewModel(store = store)
        vm.claimDailyBonus()
        vm.updateBudgetDraft(BudgetDraft(savings = 200, depositAmount = 0, depositTermDays = 2))
        assertTrue(vm.confirmBudget())
        val planned = vm.state.value.budget?.planned ?: 0

        assertTrue(vm.transferToSavings(100))
        assertEquals(300, vm.state.value.savings)
        assertEquals(planned - 100, vm.state.value.balance)
        assertEquals(planned, vm.state.value.budget?.planned)
        assertEquals(0, vm.state.value.budget?.spent)
        assertEquals(MoneyLog.REASON_TO_SAVINGS, vm.state.value.moneyLog.entries.first().reason)

        assertTrue(vm.transferFromSavings(250))
        assertEquals(50, vm.state.value.savings)
        assertEquals(planned + 150, vm.state.value.balance)
        assertEquals(MoneyLog.REASON_FROM_SAVINGS, vm.state.value.moneyLog.entries.first().reason)
    }

    @Test
    fun `a transfer of nothing, or of more than there is, moves nothing`() {
        val vm = testGameViewModel()
        vm.claimDailyBonus()
        assertTrue(vm.confirmBudget())
        val balance = vm.state.value.balance

        assertFalse(vm.transferToSavings(0))
        assertFalse(vm.transferToSavings(-10))
        assertFalse(vm.transferToSavings(balance + 1))
        assertFalse(vm.transferFromSavings(1))

        assertEquals(balance, vm.state.value.balance)
        assertEquals(0, vm.state.value.savings)
    }

    @Test
    fun `a deposit that lived out its term pays back the body and the interest`() {
        val clock = FakeGameClock()
        val store = FakePlayerStateStore(
            PlayerState(
                balance = 100,
                deposit = Deposit.openedOn(
                    amount = 500,
                    termDays = 7,
                    day = FakeGameClock.DEFAULT_DAY
                )
            )
        )
        val vm = testGameViewModel(store = store, clock = clock)
        assertEquals(500, vm.state.value.deposit?.amount)

        clock.day += 7
        vm.openScreen(Screen.BUDGET)

        val state = vm.state.value
        assertNull(state.deposit)
        assertEquals(100 + 500 + 75, state.balance)
        assertEquals(
            listOf(MoneyLog.REASON_DEPOSIT_INTEREST, MoneyLog.REASON_DEPOSIT_CLOSED),
            state.moneyLog.entries.map { it.reason }
        )
        assertEquals(listOf(75, 500), state.moneyLog.entries.map { it.delta })
    }

    @Test
    fun `a deposit that matured while the app was closed is paid back at the next launch`() {
        val clock = FakeGameClock()
        val store = FakePlayerStateStore(
            PlayerState(
                balance = 0,
                deposit = Deposit.openedOn(
                    amount = 100,
                    termDays = 2,
                    day = FakeGameClock.DEFAULT_DAY - 5
                )
            )
        )

        val vm = testGameViewModel(store = store, clock = clock)

        assertNull(vm.state.value.deposit)
        assertEquals(104, vm.state.value.balance)
        assertNull(store.state.deposit)
    }

    @Test
    fun `skipping time far enough pays the deposit back`() {
        val clock = FakeGameClock()
        val store = FakePlayerStateStore(
            PlayerState(
                balance = 0,
                deposit = Deposit.openedOn(
                    amount = 100,
                    termDays = 2,
                    day = FakeGameClock.DEFAULT_DAY
                )
            )
        )
        val vm = testGameViewModel(store = store, clock = clock)

        vm.fastForward(java.util.concurrent.TimeUnit.DAYS.toMillis(2))

        assertNull(vm.state.value.deposit)
        assertEquals(104, vm.state.value.balance)
    }

    @Test
    fun `closing a deposit early pays the body back and no interest`() {
        val store = FakePlayerStateStore(
            PlayerState(
                balance = 10,
                deposit = Deposit.openedOn(
                    amount = 300,
                    termDays = 7,
                    day = FakeGameClock.DEFAULT_DAY
                )
            )
        )
        val vm = testGameViewModel(store = store)

        assertTrue(vm.closeDepositEarly())

        val state = vm.state.value
        assertNull(state.deposit)
        assertEquals(310, state.balance)
        assertEquals(MoneyLog.REASON_DEPOSIT_CLOSED_EARLY, state.moneyLog.entries.first().reason)
        assertEquals(300, state.moneyLog.entries.first().delta)
        assertFalse(vm.closeDepositEarly())
    }

    @Test
    fun `closing a deposit that already matured settles it as matured, not as early`() {
        val clock = FakeGameClock()
        val store = FakePlayerStateStore(
            PlayerState(
                balance = 0,
                deposit = Deposit.openedOn(
                    amount = 100,
                    termDays = 2,
                    day = FakeGameClock.DEFAULT_DAY
                )
            )
        )
        val vm = testGameViewModel(store = store, clock = clock)

        // Срок наступил, но игрок не заходил ни на один экран, который бы это заметил.
        clock.day += 2
        assertFalse(vm.closeDepositEarly())

        val state = vm.state.value
        assertNull(state.deposit)
        assertEquals(104, state.balance)
        assertEquals(
            listOf(MoneyLog.REASON_DEPOSIT_INTEREST, MoneyLog.REASON_DEPOSIT_CLOSED),
            state.moneyLog.entries.map { it.reason }
        )
    }

    @Test
    fun `buying a mixed cart charges exactly what the log lines add up to`() {
        val vm = testGameViewModel()
        vm.claimDailyBonus()
        assertTrue(vm.confirmBudget())
        val balanceBefore = vm.state.value.balance

        vm.increaseQty(TestItems.APPLE.id)
        vm.increaseQty(TestItems.APPLE.id)
        vm.increaseQty(TestItems.FISH.id)
        vm.increaseQty(TestItems.FISH.id)
        vm.increaseQty(TestItems.FISH.id)
        assertTrue(vm.buyCart())

        val state = vm.state.value
        val loggedSpent = -state.moneyLog.entries.take(2).sumOf { it.delta }
        assertEquals(balanceBefore - state.balance, loggedSpent)
        assertEquals(loggedSpent, state.budget?.spent)
    }
}
