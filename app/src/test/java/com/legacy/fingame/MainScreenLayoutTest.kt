package com.legacy.fingame

import com.legacy.fingame.ui.screens.CornerBlock
import com.legacy.fingame.ui.screens.bottomRowFit
import com.legacy.fingame.ui.screens.stageBandOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The arithmetic the main screen is laid out by: how much the row of buttons along the bottom has
 * to shrink to fit the screen, and what the blocks in the corners leave the pet in the middle.
 */
class MainScreenLayoutTest {

    /** Size of an arrow button of the bottom row, in dp, as a phone draws it. */
    private val arrowSize = 64f

    /** Size of an action button of the bottom row, in dp, as a phone draws it. */
    private val actionSize = 80f

    /**
     * Width the bottom row takes at full size: two arrows, three action buttons, a gap inside
     * either group and a wider one between them.
     */
    private val rowNeeds = arrowSize * 2 + actionSize * 3 + 8f * 3 + 16f

    /** Smallest a button may be squeezed to and still be comfortable to hit, in dp. */
    private val minTouchTarget = 40f

    /**
     * @param screenWidth width of the screen, in dp.
     * @return Width the bottom row of that screen has, i.e. the screen less the padding on either
     * side of it.
     */
    private fun rowWidthOf(screenWidth: Float): Float = screenWidth - 16f * 2

    /**
     * @param screenWidth width of the screen, in dp.
     * @return How much of their full size the buttons of the bottom row keep on that screen.
     */
    private fun fitOn(screenWidth: Float): Float = bottomRowFit(
        availableWidth = rowWidthOf(screenWidth),
        neededWidth = rowNeeds,
        smallestButton = arrowSize,
        minTouchTarget = minTouchTarget
    )

    @Test
    fun `a screen with width to spare draws the bottom row at full size`() {
        assertEquals(1f, fitOn(screenWidth = 800f), 0f)
        assertEquals(1f, fitOn(screenWidth = 1280f), 0f)
    }

    @Test
    fun `the row of a phone too narrow for it shrinks as a whole, and stays easy to hit`() {
        // 411dp: the row needs 408 of the 379 it has, so everything in it gives up a twentieth.
        val wide = fitOn(screenWidth = 411f)
        // 360dp and 320dp: the narrowest screens the game is laid out for.
        val narrow = fitOn(screenWidth = 360f)
        val narrowest = fitOn(screenWidth = 320f)

        assertEquals(0.929f, wide, 0.001f)
        assertEquals(0.804f, narrow, 0.001f)
        assertEquals(0.706f, narrowest, 0.001f)

        assertTrue(wide > narrow && narrow > narrowest)
        assertTrue(arrowSize * narrowest >= minTouchTarget)
        assertTrue(rowNeeds * narrowest <= rowWidthOf(screenWidth = 320f) + 0.001f)
    }

    @Test
    fun `every button of a shrunken row is shrunk by the very same amount`() {
        val fit = fitOn(screenWidth = 320f)

        // Two arrows and three action buttons, each of them a fifth larger than an arrow, just as
        // they are on a screen that had room for them all along.
        assertEquals(actionSize / arrowSize, (actionSize * fit) / (arrowSize * fit), 0.0001f)
    }

    @Test
    fun `a row is left sticking out rather than shrunk past what a finger can hit`() {
        val fit = bottomRowFit(
            availableWidth = 100f,
            neededWidth = rowNeeds,
            smallestButton = arrowSize,
            minTouchTarget = minTouchTarget
        )

        assertEquals(minTouchTarget / arrowSize, fit, 0f)
        assertEquals(minTouchTarget, arrowSize * fit, 0f)
    }

    @Test
    fun `a row of nothing at all is drawn at full size instead of being divided by zero`() {
        assertEquals(
            1f,
            bottomRowFit(
                availableWidth = 0f,
                neededWidth = 0f,
                smallestButton = 0f,
                minTouchTarget = minTouchTarget
            ),
            0f
        )
    }

    @Test
    fun `an upright screen gives the pet the band between the corners, from side to side`() {
        // A 360x800dp phone with the demo build's time button in the top end corner, which is what
        // makes that corner the taller of the two.
        val band = stageBandOf(
            width = 328,
            height = 716,
            topStart = CornerBlock(width = 186, height = 142),
            topEnd = CornerBlock(width = 130, height = 192),
            bottomStart = CornerBlock(width = 103, height = 52),
            bottomEnd = CornerBlock(width = 173, height = 64),
            gap = 12
        )

        assertEquals(204, band.top)
        assertEquals(640, band.bottom)
        assertEquals(0, band.left)
        assertEquals(328, band.right)
        assertEquals(328, band.width)
        assertEquals(436, band.height)
    }

    @Test
    fun `a build without the time button hands the pet the room that button took`() {
        val corners = { topEndHeight: Int ->
            stageBandOf(
                width = 328,
                height = 716,
                topStart = CornerBlock(width = 186, height = 142),
                topEnd = CornerBlock(width = 130, height = topEndHeight),
                bottomStart = CornerBlock(width = 103, height = 52),
                bottomEnd = CornerBlock(width = 173, height = 64),
                gap = 12
            )
        }

        val demo = corners(192)
        val released = corners(140)

        // Without the button the top end corner is shorter than the one with the money in it, so
        // the band starts under that one instead.
        assertEquals(154, released.top)
        assertEquals(demo.bottom, released.bottom)
        assertTrue(released.height > demo.height)
    }

    @Test
    fun `a wide screen gives the pet the band between the sides, from top to bottom`() {
        // A 800x360dp phone held sideways: the time button stands beside the other two up there,
        // so the end side is as wide as the action buttons under it.
        val band = stageBandOf(
            width = 768,
            height = 280,
            topStart = CornerBlock(width = 208, height = 142),
            topEnd = CornerBlock(width = 274, height = 64),
            bottomStart = CornerBlock(width = 136, height = 64),
            bottomEnd = CornerBlock(width = 256, height = 80),
            gap = 12
        )

        assertEquals(220, band.left)
        assertEquals(482, band.right)
        assertEquals(0, band.top)
        assertEquals(280, band.bottom)
        assertEquals(262, band.width)
        assertEquals(280, band.height)
    }

    @Test
    fun `a screen the corners alone fill leaves a band of no size rather than an upside-down one`() {
        // A 320x380dp window with the text turned up: the corners want 234 of the 300 there are,
        // the bottom row the last 76 of them, and the pet is left with nothing between them.
        val band = stageBandOf(
            width = 288,
            height = 300,
            topStart = CornerBlock(width = 208, height = 180),
            topEnd = CornerBlock(width = 130, height = 222),
            bottomStart = CornerBlock(width = 103, height = 52),
            bottomEnd = CornerBlock(width = 173, height = 64),
            gap = 12
        )

        assertEquals(0, band.height)
        assertEquals(band.top, band.bottom)
        assertTrue(band.top in 0..300)
        assertEquals(288, band.width)
    }
}
