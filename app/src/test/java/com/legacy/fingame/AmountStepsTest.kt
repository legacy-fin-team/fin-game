package com.legacy.fingame

import com.legacy.fingame.ui.screens.amountSnappedTo
import com.legacy.fingame.ui.screens.amountStepOf
import com.legacy.fingame.ui.screens.amountSteppedBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Как набирается сумма кнопками и ползунком: шаг, сдвиг и прилипание к ступеням. */
class AmountStepsTest {

    @Test
    fun `the step is a round tenth of what there is to lay out`() {
        assertEquals(1, amountStepOf(0))
        assertEquals(1, amountStepOf(1))
        assertEquals(1, amountStepOf(7))
        assertEquals(5, amountStepOf(60))
        assertEquals(25, amountStepOf(260))
        assertEquals(100, amountStepOf(1000))
        assertEquals(1000, amountStepOf(12400))
    }

    @Test
    fun `the step is never smaller than a coin`() {
        (0..30).forEach { max ->
            assertTrue(amountStepOf(max) >= 1)
        }
    }

    @Test
    fun `stepping up and down sticks to round amounts`() {
        assertEquals(40, amountSteppedBy(value = 37, delta = 1, max = 1000, step = 10))
        assertEquals(30, amountSteppedBy(value = 37, delta = -1, max = 1000, step = 10))
        assertEquals(50, amountSteppedBy(value = 40, delta = 1, max = 1000, step = 10))
        assertEquals(30, amountSteppedBy(value = 40, delta = -1, max = 1000, step = 10))
    }

    @Test
    fun `stepping stays within what there is`() {
        assertEquals(0, amountSteppedBy(value = 5, delta = -10, max = 100, step = 25))
        assertEquals(100, amountSteppedBy(value = 90, delta = 10, max = 100, step = 25))
        assertEquals(0, amountSteppedBy(value = -50, delta = 0, max = 100, step = 25))
        assertEquals(100, amountSteppedBy(value = 500, delta = 0, max = 100, step = 25))
    }

    @Test
    fun `a step of nothing at all leaves the amount where it is`() {
        assertEquals(37, amountSteppedBy(value = 37, delta = 0, max = 1000, step = 10))
    }

    @Test
    fun `the slider snaps to the nearest step`() {
        assertEquals(100, amountSnappedTo(raw = 96f, max = 1000, step = 25))
        assertEquals(100, amountSnappedTo(raw = 104f, max = 1000, step = 25))
        assertEquals(125, amountSnappedTo(raw = 120f, max = 1000, step = 25))
    }

    @Test
    fun `both ends of the slider answer exactly`() {
        assertEquals(0, amountSnappedTo(raw = 0f, max = 260, step = 25))
        assertEquals(260, amountSnappedTo(raw = 260f, max = 260, step = 25))
        // Ползунок иногда отдаёт чуть больше или чуть меньше края — это всё равно край.
        assertEquals(0, amountSnappedTo(raw = -5f, max = 260, step = 25))
        assertEquals(260, amountSnappedTo(raw = 999f, max = 260, step = 25))
    }

    @Test
    fun `nothing to lay out divides by nothing`() {
        assertEquals(0, amountSnappedTo(raw = 0f, max = 0))
        assertEquals(0, amountSnappedTo(raw = 50f, max = 0))
        assertEquals(0, amountSteppedBy(value = 0, delta = 1, max = 0))
    }

    @Test
    fun `the step of a plan stays the same while its lines change`() {
        // Раскладываются 250: шаг — от всей суммы, 25, и он не сходит на 10, когда соседняя строка
        // забрала часть и строке осталось 150. Раньше шаг считался от остатка, и «плюс» давал 25,
        // а следующий — 10.
        val total = 250
        val step = amountStepOf(total)
        val want = 100
        var must = 0
        val seen = mutableListOf<Int>()
        repeat(3) {
            must = amountSteppedBy(value = must, delta = 1, max = total - want, step = step)
            seen += must
        }
        assertEquals(25, step)
        assertEquals(listOf(25, 50, 75), seen)
    }
}
