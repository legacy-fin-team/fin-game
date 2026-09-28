package com.legacy.fingame

import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.adult.AdultMoney
import com.legacy.fingame.game.economy.MoneyLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ручное изменение монет взрослым: правила и запись в журнал. */
class AdultMoneyTest {

    @Test
    fun `a filled change is valid`() {
        assertEquals(emptyList<String>(), AdultMoney.validate(50, add = true, reason = "за уборку", balance = 0))
        assertEquals(emptyList<String>(), AdultMoney.validate(30, add = false, reason = "чашка", balance = 30))
    }

    @Test
    fun `the amount, the reason and the balance are checked`() {
        assertTrue(AdultMoney.validate(0, true, "за уборку", 0).any { it.startsWith("Сумма") })
        assertTrue(AdultMoney.validate(AdultMoney.AMOUNT_MAX + 1, true, "за уборку", 0).any { it.startsWith("Сумма") })
        assertTrue(AdultMoney.validate(10, true, "   ", 0).contains("Впиши причину"))
        // Ниже нуля нельзя: убрать можно не больше, чем есть на счёте.
        assertTrue(AdultMoney.validate(31, false, "чашка", 30).any { it.startsWith("Можно убрать") })
    }

    @Test
    fun `can not save when removing more than the balance`() {
        assertFalse(AdultMoney.canSave(31, add = false, reason = "чашка", balance = 30))
        assertTrue(AdultMoney.insufficientBalance(31, add = false, balance = 30))
        assertTrue(AdultMoney.canSave(30, add = false, reason = "чашка", balance = 30))
        assertFalse(AdultMoney.insufficientBalance(30, add = false, balance = 30))
        // Добавить деньги можно и сверх текущего счёта — тут ограничения нет.
        assertFalse(AdultMoney.insufficientBalance(1000, add = true, balance = 0))
    }

    @Test
    fun `the reason is cleaned and cut`() {
        assertEquals("за уборку", AdultMoney.cleanReason("  за уборку\n "))
        assertEquals(AdultMoney.REASON_MAX, AdultMoney.cleanReason("а".repeat(100)).length)
        assertEquals("аб", AdultMoney.cleanReason("а\u001Fб"))
    }

    @Test
    fun `the log reason says who and why`() {
        assertEquals("Взрослый добавил 50: за уборку", MoneyLog.adultReason(50, "за уборку"))
        assertEquals("Взрослый убрал 20: разбил чашку", MoneyLog.adultReason(-20, "разбил чашку"))
    }

    @Test
    fun `only the adult changes the money, and it is logged and saved`() {
        val clock = FakeGameClock()
        val store = FakePlayerStateStore(PlayerState(balance = 40))
        val vm = testGameViewModel(store = store, clock = clock)

        assertFalse(vm.adjustBalanceByAdult(50, add = true, reason = "за уборку"))
        vm.enterAdultMode()
        assertTrue(vm.adjustBalanceByAdult(50, add = true, reason = " за уборку "))
        assertFalse(vm.adjustBalanceByAdult(100, add = false, reason = "слишком много"))
        assertFalse(vm.adjustBalanceByAdult(10, add = false, reason = ""))
        assertTrue(vm.adjustBalanceByAdult(90, add = false, reason = "разбил чашку"))
        vm.exitAdultMode()

        val state = vm.state.value
        assertEquals(0, state.balance)
        assertEquals(
            listOf("Взрослый убрал 90: разбил чашку", "Взрослый добавил 50: за уборку"),
            state.moneyLog.entries.take(2).map { it.reason }
        )
        assertTrue(state.moneyLog.entries.take(2).all { it.fromAdult })
        assertEquals(listOf(-90, 50), state.moneyLog.entries.take(2).map { it.delta })
        assertEquals(0, store.state.balance)
        assertTrue(store.state.moneyLog.entries.first().fromAdult)
    }
}
