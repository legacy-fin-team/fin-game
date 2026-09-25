package com.legacy.fingame

import com.legacy.fingame.game.scene.HeartBurst
import com.legacy.fingame.game.scene.PetTouchController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/**
 * Patting the pet: how often a pat sets off a wave of hearts, how long the wave lasts and where its
 * hearts go.
 */
class HeartBurstTest {

    /** An arbitrary moment the tests start at. */
    private val start = 1_000_000L

    /** A controller whose hearts are spread the same way on every run. */
    private fun controller() = PetTouchController(random = Random(seed = 7))

    @Test
    fun `a pat sets off a wave of three hearts`() {
        val burst = controller().onTap(start)

        assertNotNull(burst)
        assertEquals(start, burst!!.startMillis)
        assertEquals(HeartBurst.HEARTS, burst.spreads.size)
        assertEquals(3, HeartBurst.HEARTS)
    }

    @Test
    fun `a wave lives 1340 ms by the clock and is gone after that`() {
        val touch = controller()
        val burst = touch.onTap(start)!!

        assertEquals(1340L, HeartBurst.LIFE_MILLIS)
        assertEquals(listOf(burst), touch.alive(start + 1339))
        assertEquals(emptyList<HeartBurst>(), touch.alive(start + 1340))
    }

    @Test
    fun `a second pat 100 ms later is ignored`() {
        val touch = controller()
        touch.onTap(start)

        assertNull(touch.onTap(start + 100))
        assertEquals(1, touch.alive(start + 100).size)
    }

    @Test
    fun `a second pat 300 ms later sets off another wave`() {
        val touch = controller()
        val first = touch.onTap(start)!!

        val second = touch.onTap(start + 300)

        assertNotNull(second)
        assertTrue(second!!.id != first.id)
        assertEquals(listOf(first, second), touch.alive(start + 300))
    }

    @Test
    fun `an ignored pat does not push the next one back`() {
        val touch = controller()
        touch.onTap(start)
        touch.onTap(start + 200)

        // 250 ms after the wave that counted, not after the ignored tap.
        assertNotNull(touch.onTap(start + 250))
    }

    @Test
    fun `hearts start within 8 pixels of the middle of the pet sideways`() {
        val touch = controller()
        repeat(50) { i ->
            val burst = touch.onTap(start + i * 1000L)!!
            burst.spreads.forEach { spread -> assertTrue(spread in -8..8) }
        }
    }

    @Test
    fun `neighbouring hearts start on different lanes`() {
        val touch = controller()
        repeat(50) { i ->
            val burst = touch.onTap(start + i * 1000L)!!
            assertTrue(abs(burst.spreads[0] - burst.spreads[1]) >= 12)
        }
    }

    @Test
    fun `a heart comes out over the pet's head, rises 24 pixels and fades out`() {
        val burst = HeartBurst(id = 0, startMillis = start, spreads = listOf(-5, 0, 5))

        val born = burst.heartAt(index = 0, elapsedMillis = 0)!!
        assertEquals(-5f, born.x, 0.001f)
        assertEquals(-19f, born.y, 0.001f)
        assertEquals(0f, born.alpha, 0.001f)

        val halfway = burst.heartAt(index = 0, elapsedMillis = 450)!!
        assertEquals(-31f, halfway.y, 0.001f)
        assertEquals(1f, halfway.alpha, 0.001f)

        val nearlyGone = burst.heartAt(index = 0, elapsedMillis = 855)!!
        assertEquals(-19f - 24f * 0.95f, nearlyGone.y, 0.001f)
        assertEquals(0.1f, nearlyGone.alpha, 0.001f)

        assertNull(burst.heartAt(index = 0, elapsedMillis = 900))
    }

    @Test
    fun `a heart fades in during the first tenth of its flight`() {
        val burst = HeartBurst(id = 0, startMillis = start, spreads = listOf(0, 0, 0))

        assertEquals(0f, burst.heartAt(index = 0, elapsedMillis = 0)!!.alpha, 0.001f)
        assertEquals(0.5f, burst.heartAt(index = 0, elapsedMillis = 45)!!.alpha, 0.01f)
        assertEquals(1f, burst.heartAt(index = 0, elapsedMillis = 90)!!.alpha, 0.01f)
    }

    @Test
    fun `the hearts of a wave come out one after another`() {
        val burst = HeartBurst(id = 0, startMillis = start, spreads = listOf(0, 0, 0))

        assertNull(burst.heartAt(index = 1, elapsedMillis = 219))
        assertNotNull(burst.heartAt(index = 1, elapsedMillis = 220))
        assertNull(burst.heartAt(index = 2, elapsedMillis = 439))
        assertNotNull(burst.heartAt(index = 2, elapsedMillis = 1339))
        assertNull(burst.heartAt(index = 2, elapsedMillis = 1340))
    }

    @Test
    fun `a heart sways no further than a pixel and a half from where it started`() {
        val burst = HeartBurst(id = 0, startMillis = start, spreads = listOf(3, 3, 3))

        (0 until 900 step 10).forEach { t ->
            val heart = burst.heartAt(index = 0, elapsedMillis = t.toLong())!!
            assertTrue(heart.x in 1.5f..4.5f)
        }
    }
}
