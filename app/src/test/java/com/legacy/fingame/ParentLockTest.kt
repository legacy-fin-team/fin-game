package com.legacy.fingame

import com.legacy.fingame.game.adult.LOCK_MAX_FACTOR
import com.legacy.fingame.game.adult.LOCK_MIN_FACTOR
import com.legacy.fingame.game.adult.LockTask
import com.legacy.fingame.game.adult.isSolved
import com.legacy.fingame.game.adult.lockTasksOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Замок перед режимом взрослого: примеры на умножение и их проверка. */
class ParentLockTest {

    @Test
    fun `the answer is the product`() {
        assertEquals(56, LockTask(7, 8).answer)
    }

    @Test
    fun `three tasks with factors from 3 to 9`() {
        repeat(50) { seed ->
            val tasks = lockTasksOf(Random(seed))
            assertEquals(3, tasks.size)
            tasks.forEach { task ->
                assertTrue(task.a in LOCK_MIN_FACTOR..LOCK_MAX_FACTOR)
                assertTrue(task.b in LOCK_MIN_FACTOR..LOCK_MAX_FACTOR)
            }
        }
    }

    @Test
    fun `every factor from 3 to 9 comes up`() {
        val factors = (0 until 200).flatMap { seed ->
            lockTasksOf(Random(seed)).flatMap { listOf(it.a, it.b) }
        }.toSet()
        assertEquals((LOCK_MIN_FACTOR..LOCK_MAX_FACTOR).toSet(), factors)
    }

    @Test
    fun `solved only when every answer is right`() {
        val tasks = listOf(LockTask(7, 8), LockTask(3, 4), LockTask(9, 9))

        assertTrue(isSolved(tasks, listOf("56", "12", "81")))
        assertFalse(isSolved(tasks, listOf("56", "12", "80")))
        assertFalse(isSolved(tasks, listOf("56", "12", "")))
        assertFalse(isSolved(tasks, listOf("56", "12")))
    }
}
