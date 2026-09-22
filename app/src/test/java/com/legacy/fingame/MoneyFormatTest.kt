package com.legacy.fingame

import com.legacy.fingame.game.economy.MoneyEntry
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.ui.screens.balancesDescriptionOf
import com.legacy.fingame.ui.screens.dayNumberOf
import com.legacy.fingame.ui.screens.dayTimeTextOf
import com.legacy.fingame.ui.screens.depositMaturityTextOf
import com.legacy.fingame.ui.screens.depositTextOf
import com.legacy.fingame.ui.screens.firstDayOf
import com.legacy.fingame.ui.screens.signedAmountText
import com.legacy.fingame.ui.screens.timeTextOf
import com.legacy.fingame.ui.screens.todayTextOf
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Как деньги и время превращаются в текст, который читает игрок. */
class MoneyFormatTest {

    @Test
    fun `the deposit is written out in words next to the money`() {
        assertEquals("вклад 510", depositTextOf(510))
    }

    @Test
    fun `no deposit, no caption about one`() {
        // Раньше в шапке стояло «250 | 0»: ноль про вклад, которого нет, ребёнку не говорит ничего,
        // а чёрточка перед ним читается как случайный значок.
        assertNull(depositTextOf(0))
    }

    @Test
    fun `a screen reader is told which number is which`() {
        assertEquals("Текущие 100, на вкладе 510", balancesDescriptionOf(100, 510))
    }

    @Test
    fun `a screen reader hears about the deposit only while there is one`() {
        assertEquals("Текущие 250", balancesDescriptionOf(250, 0))
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

    @Test
    fun `the log's time column is the day number and the time together`() {
        // 2023-11-14T16:09:00Z
        val millis = 1_699_978_140_000L
        assertEquals(
            "Д8 · 16:09",
            dayTimeTextOf(gameDay = 19_007L, oldestGameDay = 19_000L, millis = millis, zone = ZoneId.of("UTC"))
        )
    }

    @Test
    fun `today is day one while the log is empty`() {
        // Считать не от чего, и счёт начинается с сегодня: иначе у игрока вышло бы два разных
        // «дня 1» — один в журнале, другой на бюджете.
        val today = 19_042L
        assertEquals(1, dayNumberOf(today, firstDayOf(MoneyLog.EMPTY.oldestGameDay, today)))
        assertEquals("Сегодня: день 1", todayTextOf(today, firstDayOf(MoneyLog.EMPTY.oldestGameDay, today)))
    }

    @Test
    fun `the day the player is told is the day the log calls it`() {
        val log = MoneyLog(
            listOf(
                MoneyEntry(MoneyLog.REASON_DAILY_BONUS, 50, 19_004L, 1_700_004_000_000L),
                MoneyEntry(MoneyLog.REASON_DAILY_BONUS, 50, 19_000L, 1_700_000_000_000L)
            )
        )
        assertEquals("Сегодня: день 5", todayTextOf(19_004L, firstDayOf(log.oldestGameDay, 19_004L)))
    }

    @Test
    fun `a deposit says how long is left and which day that is`() {
        assertEquals(
            "Закроется через 2 дн · день 5",
            depositMaturityTextOf(maturityDay = 19_004L, todayDay = 19_002L, oldestGameDay = 19_000L)
        )
        assertEquals(
            "Закроется через 1 дн · день 4",
            depositMaturityTextOf(maturityDay = 19_003L, todayDay = 19_002L, oldestGameDay = 19_000L)
        )
        assertEquals(
            "Закроется через 5 дн · день 8",
            depositMaturityTextOf(maturityDay = 19_007L, todayDay = 19_002L, oldestGameDay = 19_000L)
        )
    }

    @Test
    fun `a deposit that is due says so in words, not in zero days`() {
        assertEquals(
            "Закроется сегодня",
            depositMaturityTextOf(maturityDay = 19_002L, todayDay = 19_002L, oldestGameDay = 19_000L)
        )
    }

    @Test
    fun `a deposit left standing past its day is still closing today`() {
        // Вклад созревает, когда день дошёл до срока, и позже — тоже: «через -3 дн» не бывает.
        assertEquals(
            "Закроется сегодня",
            depositMaturityTextOf(maturityDay = 19_002L, todayDay = 19_005L, oldestGameDay = 19_000L)
        )
    }
}
