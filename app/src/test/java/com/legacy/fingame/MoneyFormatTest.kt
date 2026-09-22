package com.legacy.fingame

import com.legacy.fingame.ui.screens.balancesDescriptionOf
import com.legacy.fingame.ui.screens.balancesTextOf
import com.legacy.fingame.ui.screens.dayNumberOf
import com.legacy.fingame.ui.screens.signedAmountText
import com.legacy.fingame.ui.screens.timeTextOf
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/** Как деньги и время превращаются в текст, который читает игрок. */
class MoneyFormatTest {

    @Test
    fun `both balances are written through a bar`() {
        assertEquals("100 | 510", balancesTextOf(100, 510))
        assertEquals("0 | 0", balancesTextOf(0, 0))
    }

    @Test
    fun `a screen reader is told which number is which`() {
        assertEquals("Текущие 100, на вкладе 510", balancesDescriptionOf(100, 510))
    }

    @Test
    fun `a gain carries a plus, a loss carries a minus, nothing carries neither`() {
        assertEquals("+100", signedAmountText(100))
        assertEquals("-100", signedAmountText(-100))
        assertEquals("0", signedAmountText(0))
    }

    @Test
    fun `the day is counted from the oldest record the log still holds`() {
        assertEquals(1, dayNumberOf(gameDay = 19_000L, oldestGameDay = 19_000L))
        assertEquals(4, dayNumberOf(gameDay = 19_003L, oldestGameDay = 19_000L))
        // Запись из дня раньше самой старой невозможна, но и она не даст нулевого дня.
        assertEquals(1, dayNumberOf(gameDay = 18_900L, oldestGameDay = 19_000L))
    }

    @Test
    fun `the time is the hours and the minutes of the moment`() {
        // 2023-11-14T22:13:20Z
        val millis = 1_700_000_000_000L
        assertEquals("22:13", timeTextOf(millis, ZoneId.of("UTC")))
        assertEquals("01:13", timeTextOf(millis, ZoneId.of("Europe/Moscow")))
    }
}
