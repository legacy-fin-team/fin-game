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
import java.util.concurrent.TimeUnit

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
                balance = 70,
                budget = BudgetState(
                    plannedMust = 200,
                    plannedWant = 100,
                    plannedSavings = 100,
                    plannedDeposit = 0,
                    spentMust = 230,
                    spentWant = 40,
                    startDay = FakeGameClock.DEFAULT_DAY - 1
                )
            )
        )
        val vm = testGameViewModel(store = store, clock = clock)

        assertTrue(vm.claimDailyBonus())

        val state = vm.state.value
        assertNull(state.budget)
        val result = state.previousBudgetResult
        assertEquals(200, result?.plannedMust)
        assertEquals(230, result?.actualMust)
        assertEquals(100, result?.plannedWant)
        assertEquals(40, result?.actualWant)
        assertEquals(100, result?.plannedSavings)
        // Остаток счёта на момент закрытия периода, до начисления бонуса.
        assertEquals(70, result?.actualSavings)
        // Обязательного потрачено сверх плана, необязательного — меньше плана.
        assertEquals(-30, result?.mustDiff)
        assertEquals(60, result?.wantDiff)
        assertEquals(-30, result?.savingsDiff)
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
                plannedMust = 100,
                actualMust = 80,
                plannedWant = 0,
                actualWant = 0,
                plannedSavings = 0,
                actualSavings = 20,
                plannedDeposit = 0
            )
        )

        assertTrue(afterPeriod.canPlanBudget)

        val confirmed = fresh.copy(
            budget = BudgetState(
                plannedMust = 100,
                plannedWant = 0,
                plannedSavings = 0,
                plannedDeposit = 0,
                spentMust = 0,
                spentWant = 0,
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
                balance = 140,
                planningOpen = false,
                lastDailyBonusDay = clock.day
            )
        )
        val vm = testGameViewModel(store = store, clock = clock)

        assertFalse(vm.state.value.dailyBonusAvailable)
        assertTrue(vm.state.value.canPlanBudget)

        vm.updateBudgetDraft(
            BudgetDraft(mustSpend = 90, wantSpend = 0, depositAmount = 0, depositTermDays = 3)
        )

        assertEquals(90, vm.state.value.planningDraft.mustSpend)
        assertTrue(vm.confirmBudget())

        val state = vm.state.value
        // Планы трат денег не двигают: на счёте всё, что было.
        assertEquals(140, state.balance)
        assertEquals(90, state.budget?.plannedMust)
        assertEquals(50, state.budget?.plannedSavings)
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
            BudgetDraft(
                mustSpend = total + 1_000,
                wantSpend = 30,
                depositAmount = 50,
                depositTermDays = 99
            )
        )

        val draft = vm.state.value.planningDraft
        assertEquals(50, draft.depositAmount)
        assertEquals(total - 50, draft.mustSpend)
        assertEquals(0, draft.wantSpend)
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

        val total = vm.state.value.totalToPlan
        vm.updateBudgetDraft(
            BudgetDraft(mustSpend = 300, wantSpend = 100, depositAmount = 400, depositTermDays = 5)
        )
        assertTrue(vm.confirmBudget())

        val state = vm.state.value
        // Со счёта уехал ровно вклад: планы трат — это планы, а не переводы.
        assertEquals(1_000 + Economy.DAILY_BONUS - 400, state.balance)
        assertEquals(400, state.deposit?.amount)
        assertEquals(5, state.deposit?.termDays)
        assertEquals(30, state.deposit?.ratePercent)
        assertEquals(FakeGameClock.DEFAULT_DAY, state.deposit?.openedDay)
        assertEquals(300, state.budget?.plannedMust)
        assertEquals(100, state.budget?.plannedWant)
        assertEquals(total - 300 - 100 - 400, state.budget?.plannedSavings)
        assertEquals(400, state.budget?.plannedDeposit)
        assertEquals(0, state.budget?.spent)
        assertFalse(state.planningOpen)
        assertNull(state.budgetDraft)

        // Журнал: открытие вклада поверх бонуса дня, новейшее первым.
        assertEquals(
            listOf(MoneyLog.REASON_DEPOSIT_OPENED, MoneyLog.REASON_DAILY_BONUS),
            state.moneyLog.entries.map { it.reason }
        )
        assertEquals(listOf(-400, Economy.DAILY_BONUS), state.moneyLog.entries.map { it.delta })
    }

    @Test
    fun `a confirmed budget cannot be confirmed again`() {
        val vm = testGameViewModel()
        vm.claimDailyBonus()
        assertTrue(vm.confirmBudget())

        val budget = vm.state.value.budget
        assertFalse(vm.confirmBudget())
        assertEquals(budget, vm.state.value.budget)
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

        vm.updateBudgetDraft(
            BudgetDraft(mustSpend = 0, wantSpend = 0, depositAmount = 300, depositTermDays = 3)
        )
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
        val budget = vm.state.value.budget

        vm.updateBudgetDraft(
            BudgetDraft(mustSpend = 999, wantSpend = 999, depositAmount = 0, depositTermDays = 2)
        )

        assertNull(vm.state.value.budgetDraft)
        assertEquals(budget, vm.state.value.budget)
    }

    @Test
    fun `a purchase is written into the log and counted against its own category`() {
        val vm = testGameViewModel()
        vm.claimDailyBonus()
        val balance = vm.state.value.balance
        assertTrue(vm.confirmBudget())

        vm.increaseQty(TestItems.APPLE.id)
        vm.increaseQty(TestItems.APPLE.id)
        assertTrue(vm.buyCart())

        val state = vm.state.value
        val spent = TestItems.APPLE.price * 2
        assertEquals(spent, state.budget?.spentMust)
        assertEquals(0, state.budget?.spentWant)
        assertEquals(balance - spent, state.balance)
        assertEquals(
            MoneyLog.purchaseReason(TestItems.APPLE.name, 2),
            state.moneyLog.entries.first().reason
        )
        assertEquals(-spent, state.moneyLog.entries.first().delta)
    }

    @Test
    fun `a decoration is counted as spending the pet could have done without`() {
        val store = FakePlayerStateStore(PlayerState(balance = 500))
        val vm = testGameViewModel(store = store)
        vm.claimDailyBonus()
        assertTrue(vm.confirmBudget())

        vm.increaseQty(TestItems.LAMP.id)
        assertTrue(vm.buyCart())

        assertEquals(0, vm.state.value.budget?.spentMust)
        assertEquals(TestItems.LAMP.price, vm.state.value.budget?.spentWant)
    }

    @Test
    fun `a mixed cart lands in both categories at once`() {
        val store = FakePlayerStateStore(PlayerState(balance = 500))
        val vm = testGameViewModel(store = store)
        vm.claimDailyBonus()
        assertTrue(vm.confirmBudget())

        vm.increaseQty(TestItems.APPLE.id)
        vm.increaseQty(TestItems.BALL.id)
        vm.increaseQty(TestItems.HAT.id)
        assertTrue(vm.buyCart())

        val budget = vm.state.value.budget
        assertEquals(TestItems.APPLE.price + TestItems.BALL.price, budget?.spentMust)
        assertEquals(TestItems.HAT.price, budget?.spentWant)
        assertEquals(
            TestItems.APPLE.price + TestItems.BALL.price + TestItems.HAT.price,
            budget?.spent
        )
    }

    @Test
    fun `a deposit closed early during the planning can be opened again right away`() {
        val store = FakePlayerStateStore(
            PlayerState(
                balance = 100,
                deposit = Deposit.openedOn(
                    amount = 300,
                    termDays = 7,
                    day = FakeGameClock.DEFAULT_DAY
                )
            )
        )
        val vm = testGameViewModel(store = store)
        vm.claimDailyBonus()
        assertTrue(vm.state.value.planningOpen)

        assertTrue(vm.closeDepositEarly())

        assertNull(vm.state.value.deposit)
        assertTrue(vm.state.value.planningOpen)
        assertEquals(100 + Economy.DAILY_BONUS + 300, vm.state.value.totalToPlan)

        vm.updateBudgetDraft(
            BudgetDraft(mustSpend = 0, wantSpend = 0, depositAmount = 250, depositTermDays = 5)
        )

        assertEquals(250, vm.state.value.planningDraft.depositAmount)
        assertTrue(vm.confirmBudget())

        val state = vm.state.value
        assertEquals(250, state.deposit?.amount)
        assertEquals(5, state.deposit?.termDays)
        assertEquals(250, state.budget?.plannedDeposit)
    }

    @Test
    fun `a deposit that matured by the day of the bonus counts as money the player saved`() {
        val clock = FakeGameClock()
        val store = FakePlayerStateStore(
            PlayerState(
                balance = 40,
                deposit = Deposit.openedOn(
                    amount = 500,
                    termDays = 7,
                    day = FakeGameClock.DEFAULT_DAY - 7
                ),
                budget = BudgetState(
                    plannedMust = 100,
                    plannedWant = 0,
                    plannedSavings = 60,
                    plannedDeposit = 500,
                    spentMust = 100,
                    spentWant = 0,
                    startDay = FakeGameClock.DEFAULT_DAY - 7
                )
            )
        )
        val vm = testGameViewModel(store = store, clock = clock)

        assertTrue(vm.claimDailyBonus())

        // Тело и проценты вернулись на счёт до того, как с него сняли «сколько сохранено».
        assertEquals(40 + 500 + 250, vm.state.value.previousBudgetResult?.actualSavings)
        assertEquals(60, vm.state.value.previousBudgetResult?.plannedSavings)
        assertEquals(40 + 500 + 250 + Economy.DAILY_BONUS, vm.state.value.balance)
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
        assertEquals(100 + 500 + 250, state.balance)
        assertEquals(
            listOf(MoneyLog.REASON_DEPOSIT_INTEREST, MoneyLog.REASON_DEPOSIT_CLOSED),
            state.moneyLog.entries.map { it.reason }
        )
        assertEquals(listOf(250, 500), state.moneyLog.entries.map { it.delta })
    }

    @Test
    fun `a deposit too small to earn a coin writes no interest line at all`() {
        val clock = FakeGameClock()
        // 15% of 3 is less than half a coin, so the interest rounds to nothing.
        val store = FakePlayerStateStore(
            PlayerState(
                balance = 100,
                deposit = Deposit.openedOn(
                    amount = 3,
                    termDays = 3,
                    day = FakeGameClock.DEFAULT_DAY
                )
            )
        )
        val vm = testGameViewModel(store = store, clock = clock)

        clock.day += 3
        vm.openScreen(Screen.BUDGET)

        val state = vm.state.value
        assertNull(state.deposit)
        assertEquals(100 + 3, state.balance)
        assertEquals(
            listOf(MoneyLog.REASON_DEPOSIT_CLOSED),
            state.moneyLog.entries.map { it.reason }
        )
        assertEquals(listOf(3), state.moneyLog.entries.map { it.delta })
    }

    @Test
    fun `a deposit opened after a skip matures while the app is closed`() {
        val clock = FakeGameClock()
        val store = FakePlayerStateStore(PlayerState(balance = 100))
        val firstRun = testGameViewModel(store = store, clock = clock)

        // Двенадцать часов дважды: вклад открывается днём, до которого игра дошла перемоткой.
        firstRun.fastForward(TimeUnit.HOURS.toMillis(12))
        firstRun.fastForward(TimeUnit.HOURS.toMillis(12))
        assertTrue(firstRun.claimDailyBonus())
        firstRun.updateBudgetDraft(
            BudgetDraft(mustSpend = 0, wantSpend = 0, depositAmount = 100, depositTermDays = 3)
        )
        assertTrue(firstRun.confirmBudget())
        assertEquals(
            FakeGameClock.DEFAULT_DAY + 1 + 3,
            firstRun.state.value.deposit?.maturityDay
        )

        // Приложение закрыто на час реального времени, за который календарь ушёл на три дня.
        clock.millis += TimeUnit.HOURS.toMillis(1)
        clock.day += 3
        val nextRun = testGameViewModel(store = store, clock = clock)

        assertNull(nextRun.state.value.deposit)
        assertEquals(Economy.DAILY_BONUS + 115, nextRun.state.value.balance)
    }

    @Test
    fun `a deposit that matured while the app was closed is paid back at the next launch`() {
        val clock = FakeGameClock()
        val store = FakePlayerStateStore(
            PlayerState(
                balance = 0,
                deposit = Deposit.openedOn(
                    amount = 100,
                    termDays = 3,
                    day = FakeGameClock.DEFAULT_DAY - 5
                )
            )
        )

        val vm = testGameViewModel(store = store, clock = clock)

        assertNull(vm.state.value.deposit)
        assertEquals(115, vm.state.value.balance)
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
                    termDays = 3,
                    day = FakeGameClock.DEFAULT_DAY
                )
            )
        )
        val vm = testGameViewModel(store = store, clock = clock)

        vm.fastForward(java.util.concurrent.TimeUnit.DAYS.toMillis(3))

        assertNull(vm.state.value.deposit)
        assertEquals(115, vm.state.value.balance)
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
                    termDays = 3,
                    day = FakeGameClock.DEFAULT_DAY
                )
            )
        )
        val vm = testGameViewModel(store = store, clock = clock)

        // Срок наступил, но игрок не заходил ни на один экран, который бы это заметил.
        clock.day += 3
        assertFalse(vm.closeDepositEarly())

        val state = vm.state.value
        assertNull(state.deposit)
        assertEquals(115, state.balance)
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

    @Test
    fun `an overspent category comes out of the report with a minus`() {
        val store = FakePlayerStateStore(PlayerState(balance = 500))
        val clock = FakeGameClock()
        val vm = testGameViewModel(store = store, clock = clock)
        vm.claimDailyBonus()
        vm.updateBudgetDraft(
            BudgetDraft(mustSpend = 10, wantSpend = 200, depositAmount = 0, depositTermDays = 2)
        )
        assertTrue(vm.confirmBudget())

        vm.increaseQty(TestItems.BALL.id)
        assertTrue(vm.buyCart())

        clock.day += 1
        assertTrue(vm.claimDailyBonus())

        val result = vm.state.value.previousBudgetResult
        assertEquals(10, result?.plannedMust)
        assertEquals(TestItems.BALL.price, result?.actualMust)
        assertEquals(10 - TestItems.BALL.price, result?.mustDiff)
        assertEquals(200, result?.plannedWant)
        assertEquals(0, result?.actualWant)
    }
}
