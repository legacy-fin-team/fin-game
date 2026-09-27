package com.legacy.fingame

import com.legacy.fingame.game.economy.goalProgress
import com.legacy.fingame.game.items.GoalLine
import com.legacy.fingame.game.items.Goals
import com.legacy.fingame.game.items.ItemSelection
import org.junit.Assert.assertEquals
import org.junit.Test

/** Сколько пройдено до цели и как цели встречаются с каталогом. */
class GoalsTest {

    @Test
    fun `no money is no progress`() {
        assertEquals(0f, goalProgress(balance = 0, price = 200), 0f)
        assertEquals(0f, goalProgress(balance = -5, price = 200), 0f)
    }

    @Test
    fun `part of the price is that part of the way`() {
        assertEquals(0.25f, goalProgress(balance = 50, price = 200), 0.0001f)
        assertEquals(0.6f, goalProgress(balance = 120, price = 200), 0.0001f)
    }

    @Test
    fun `enough money is the whole way, and never more`() {
        assertEquals(1f, goalProgress(balance = 200, price = 200), 0f)
        assertEquals(1f, goalProgress(balance = 500, price = 200), 0f)
    }

    @Test
    fun `a free item is always within reach`() {
        assertEquals(1f, goalProgress(balance = 0, price = 0), 0f)
        assertEquals(1f, goalProgress(balance = 10, price = -1), 0f)
    }

    @Test
    fun `goal lines keep the goals' order, skip what the shop lost and repeats`() {
        val lines = Goals.linesOf(
            goals = listOf(
                ItemSelection(TestItems.LAMP.id, "default"),
                ItemSelection("ghost", "default"),
                ItemSelection(TestItems.HAT.id, "white"),
                ItemSelection(TestItems.HAT.id, "purple"),
                ItemSelection(TestItems.LAMP.id, "default"),
                ItemSelection(TestItems.BALL.id, "red")
            ),
            catalog = FakeItemCatalog()
        )

        assertEquals(
            listOf(
                GoalLine(item = TestItems.LAMP, variantId = "default"),
                GoalLine(item = TestItems.HAT, variantId = "white"),
                GoalLine(item = TestItems.BALL, variantId = "red")
            ),
            lines
        )
    }

    @Test
    fun `a goal line names the goal it came from`() {
        assertEquals(
            ItemSelection(TestItems.HAT.id, "white"),
            GoalLine(item = TestItems.HAT, variantId = "white").selection
        )
    }
}
