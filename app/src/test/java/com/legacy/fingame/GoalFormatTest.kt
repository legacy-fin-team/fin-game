package com.legacy.fingame

import com.legacy.fingame.ui.screens.GoalReadyText
import com.legacy.fingame.ui.screens.NoBreakSpace
import com.legacy.fingame.ui.screens.goalAmountText
import com.legacy.fingame.ui.screens.goalCounterText
import com.legacy.fingame.ui.screens.goalFooterText
import com.legacy.fingame.ui.screens.goalIsReady
import com.legacy.fingame.ui.screens.goalPercentText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Как цель превращается в текст на карточке. */
class GoalFormatTest {

    @Test
    fun `the amount says what is saved out of the price`() {
        assertEquals("120$NoBreakSpace/${NoBreakSpace}200", goalAmountText(balance = 120, price = 200))
    }

    @Test
    fun `more money than the price is written as the price`() {
        assertEquals("200$NoBreakSpace/${NoBreakSpace}200", goalAmountText(balance = 500, price = 200))
    }

    @Test
    fun `no money is written as nothing saved`() {
        assertEquals("0$NoBreakSpace/${NoBreakSpace}200", goalAmountText(balance = 0, price = 200))
        assertEquals("0$NoBreakSpace/${NoBreakSpace}200", goalAmountText(balance = -3, price = 200))
    }

    @Test
    fun `the percentage rounds down, so a hundred means enough`() {
        assertEquals("99%", goalPercentText(balance = 199, price = 200))
        assertEquals("29%", goalPercentText(balance = 29, price = 100))
        assertEquals("0%", goalPercentText(balance = 0, price = 200))
        assertEquals("0%", goalPercentText(balance = -10, price = 200))
        assertEquals("100%", goalPercentText(balance = 200, price = 200))
        assertEquals("100%", goalPercentText(balance = 999, price = 200))
    }

    @Test
    fun `a free item is all the way there`() {
        assertEquals("100%", goalPercentText(balance = 5, price = 0))
        assertEquals("0$NoBreakSpace/${NoBreakSpace}0", goalAmountText(balance = 5, price = 0))
    }

    @Test
    fun `the footer shows the amount until there is enough`() {
        assertEquals("120$NoBreakSpace/${NoBreakSpace}200", goalFooterText(balance = 120, price = 200))
        assertEquals("199$NoBreakSpace/${NoBreakSpace}200", goalFooterText(balance = 199, price = 200))
    }

    @Test
    fun `the footer says it can be bought instead of the amount once there is enough`() {
        assertEquals(GoalReadyText, goalFooterText(balance = 200, price = 200))
        assertEquals(GoalReadyText, goalFooterText(balance = 999, price = 200))
        assertEquals(GoalReadyText, goalFooterText(balance = 0, price = 0))
    }

    @Test
    fun `a goal is ready exactly when the percentage reaches a hundred`() {
        assertFalse(goalIsReady(balance = 199, price = 200))
        assertTrue(goalIsReady(balance = 200, price = 200))
        assertTrue(goalIsReady(balance = -5, price = 0))
        for (balance in listOf(-10, 0, 1, 99, 100, 101, 1000)) {
            assertEquals(goalPercentText(balance = balance, price = 100) == "100%", goalIsReady(balance, 100))
        }
    }

    @Test
    fun `the counter counts goals from one`() {
        assertEquals("1/3", goalCounterText(index = 0, count = 3))
        assertEquals("3/3", goalCounterText(index = 2, count = 3))
    }
}
