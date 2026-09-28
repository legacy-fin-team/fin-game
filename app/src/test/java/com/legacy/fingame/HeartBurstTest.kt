package com.legacy.fingame

import com.legacy.fingame.game.scene.HeartBurst
import com.legacy.fingame.game.scene.PetTouchController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
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
    fun `a wave lives 1400 ms by the clock and is gone after that`() {
        val touch = controller()
        val burst = touch.onTap(start)!!

        assertEquals(1400L, HeartBurst.LIFE_MILLIS)
        assertEquals(listOf(burst), touch.alive(start + 1399))
        assertEquals(emptyList<HeartBurst>(), touch.alive(start + 1400))
    }

    @Test
    fun `a second pat 100 ms later is ignored`() {
        val touch = controller()
        touch.onTap(start)

        assertNull(touch.onTap(start + 100))
        assertEquals(1, touch.alive(start + 100).size)
    }

    @Test
    fun `a second pat 450 ms later sets off another wave`() {
        val touch = controller()
        val first = touch.onTap(start)!!

        val second = touch.onTap(start + 450)

        assertNotNull(second)
        assertTrue(second!!.id != first.id)
        assertEquals(listOf(first, second), touch.alive(start + 450))
    }

    @Test
    fun `an ignored pat does not push the next one back`() {
        val touch = controller()
        touch.onTap(start)
        assertNull(touch.onTap(start + 399))

        // 400 ms after the wave that counted, not after the ignored tap.
        assertNotNull(touch.onTap(start + 400))
    }

    @Test
    fun `hearts start within 8 pixels of the middle of the pet sideways`() {
        val touch = controller()
        val spread = PetTouchController.SPREAD_PX
        assertEquals(8, spread)
        repeat(50) { i ->
            val burst = touch.onTap(start + i * 1000L)!!
            burst.spreads.forEach { assertTrue(it in -spread..spread) }
        }
    }

    @Test
    fun `a heart is born on top of the head of every age`() {
        // Top row of the cat's own pixels in its 32x32 sprite at each age: the kitten's head is
        // 11 rows down, the grown-up cat's at the very top. The middle of the sprite is row 16.
        listOf(0 to 11, 1 to 3, 2 to 0).forEach { (age, headTop) ->
            val burst = controller().onTap(start, age = age)!!
            val born = burst.heartAt(index = 0, elapsedMillis = 0)!!
            assertEquals(-16f + headTop, born.y + HeartBurst.HEART_PIXELS / 2, 0.001f)
        }
        // An age past the last one drawn is the last one, as the pet's own sprite is.
        assertEquals(HeartBurst.heartStartY(age = 2), HeartBurst.heartStartY(age = 5), 0f)
    }

    @Test
    fun `a younger heart is born fully below its older neighbour`() {
        val burst = HeartBurst(id = 0, startMillis = start, spreads = listOf(0, 0, 0))

        // The moment a heart is born, the one before it has already risen a whole sprite's height
        // ([HeartBurst.HEART_PIXELS]) further up, so the two never overlap whatever their lanes.
        listOf(0 to 1, 1 to 2).forEach { (older, younger) ->
            val elapsed = younger * HeartBurst.HEART_STAGGER_MILLIS
            val olderY = burst.heartAt(index = older, elapsedMillis = elapsed)!!.y
            val youngerY = burst.heartAt(index = younger, elapsedMillis = elapsed)!!.y
            assertTrue(olderY + HeartBurst.HEART_PIXELS <= youngerY)
        }
    }

    @Test
    fun `a heart comes out over the pet's head, rises 12 pixels and fades out`() {
        // A kitten: its head is 11 rows down the sprite, so a heart sitting on it is centred
        // -16 + 11 - 4 = -9 pixels from the middle of the pet.
        val burst = HeartBurst(id = 0, startMillis = start, spreads = listOf(-5, 0, 5))

        val born = burst.heartAt(index = 0, elapsedMillis = 0)!!
        assertEquals(-5f, born.x, 0.001f)
        assertEquals(-9f, born.y, 0.001f)
        assertEquals(0f, born.alpha, 0.001f)

        val halfway = burst.heartAt(index = 0, elapsedMillis = 300)!!
        assertEquals(-15f, halfway.y, 0.001f)
        assertEquals(1f, halfway.alpha, 0.001f)

        val nearlyGone = burst.heartAt(index = 0, elapsedMillis = 570)!!
        assertEquals(-9f - 12f * 0.95f, nearlyGone.y, 0.001f)
        assertEquals(0.1f, nearlyGone.alpha, 0.001f)

        assertNull(burst.heartAt(index = 0, elapsedMillis = 600))
    }

    @Test
    fun `a heart fades in during the first tenth of its flight`() {
        val burst = HeartBurst(id = 0, startMillis = start, spreads = listOf(0, 0, 0))

        assertEquals(0f, burst.heartAt(index = 0, elapsedMillis = 0)!!.alpha, 0.001f)
        assertEquals(0.5f, burst.heartAt(index = 0, elapsedMillis = 30)!!.alpha, 0.01f)
        assertEquals(1f, burst.heartAt(index = 0, elapsedMillis = 60)!!.alpha, 0.01f)
    }

    @Test
    fun `the hearts of a wave come out one after another`() {
        val burst = HeartBurst(id = 0, startMillis = start, spreads = listOf(0, 0, 0))

        assertNull(burst.heartAt(index = 1, elapsedMillis = 399))
        assertNotNull(burst.heartAt(index = 1, elapsedMillis = 400))
        assertNull(burst.heartAt(index = 2, elapsedMillis = 799))
        assertNotNull(burst.heartAt(index = 2, elapsedMillis = 1399))
        assertNull(burst.heartAt(index = 2, elapsedMillis = 1400))
    }

    @Test
    fun `a heart sways no further than a pixel and a half from where it started`() {
        val burst = HeartBurst(id = 0, startMillis = start, spreads = listOf(3, 3, 3))

        (0 until 600 step 10).forEach { t ->
            val heart = burst.heartAt(index = 0, elapsedMillis = t.toLong())!!
            assertTrue(heart.x in 1.5f..4.5f)
        }
    }
}
