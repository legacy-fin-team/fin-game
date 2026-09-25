package com.legacy.fingame

import com.legacy.fingame.ui.screens.CornerBlock
import com.legacy.fingame.ui.screens.bottomRowFit
import com.legacy.fingame.ui.screens.clearedTopOf
import com.legacy.fingame.ui.screens.stageBandOf
import com.legacy.fingame.ui.screens.timeButtonTopOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The arithmetic the main screen is laid out by: how much the row of buttons along the bottom has
 * to shrink to fit the screen, and what the blocks in the corners leave the pet in the middle.
 */
class MainScreenLayoutTest {

    /** Size of a button of the bottom row, in dp, as a phone draws it: every one of them is that. */
    private val actionSize = 80f

    /**
     * Width the bottom row takes at full size: five buttons of one size — two for the money screens
     * and three for the actions — a gap inside either group and a wider one between them.
     */
    private val rowNeeds = actionSize * 5 + 8f * 3 + 16f

    /** Smallest a button may be squeezed to and still be comfortable to hit, in dp. */
    private val minTouchTarget = 40f

    /** Size a button is comfortably hit at, in dp: what the row is checked to keep on a phone. */
    private val comfortableTouchTarget = 48f

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
        smallestButton = actionSize,
        minTouchTarget = minTouchTarget
    )

    @Test
    fun `a screen with width to spare draws the bottom row at full size`() {
        assertEquals(1f, fitOn(screenWidth = 800f), 0f)
        assertEquals(1f, fitOn(screenWidth = 1280f), 0f)
    }

    @Test
    fun `the row of a phone too narrow for it shrinks as a whole, and stays easy to hit`() {
        // 411dp: the row needs 440 of the 379 it has, so everything in it gives up a seventh.
        val wide = fitOn(screenWidth = 411f)
        // 360dp and 320dp: the narrowest screens the game is laid out for.
        val narrow = fitOn(screenWidth = 360f)
        val narrowest = fitOn(screenWidth = 320f)

        assertEquals(0.861f, wide, 0.001f)
        assertEquals(0.745f, narrow, 0.001f)
        assertEquals(0.655f, narrowest, 0.001f)

        assertTrue(wide > narrow && narrow > narrowest)
        assertTrue(actionSize * narrowest >= minTouchTarget)
        // Even on the narrowest of them the row is nowhere near that floor: a button comes out
        // comfortably over the size a finger asks for.
        assertTrue(actionSize * narrow >= comfortableTouchTarget)
        assertTrue(actionSize * narrowest >= comfortableTouchTarget)
        assertTrue(rowNeeds * narrowest <= rowWidthOf(screenWidth = 320f) + 0.001f)
    }

    @Test
    fun `every part of a shrunken row is shrunk by the very same amount`() {
        val fit = fitOn(screenWidth = 320f)
        val gap = 8f
        val groupGap = 16f

        // Five buttons, the gaps inside the two groups and the wider one between them, each
        // multiplied by the same number: the row ends up exactly as wide as the screen leaves it,
        // with nothing taken by one button at the expense of another.
        assertEquals(
            rowWidthOf(screenWidth = 320f),
            actionSize * fit * 5 + gap * fit * 3 + groupGap * fit,
            0.001f
        )
    }

    @Test
    fun `a row is left sticking out rather than shrunk past what a finger can hit`() {
        val fit = bottomRowFit(
            availableWidth = 100f,
            neededWidth = rowNeeds,
            smallestButton = actionSize,
            minTouchTarget = minTouchTarget
        )

        assertEquals(minTouchTarget / actionSize, fit, 0f)
        assertEquals(minTouchTarget, actionSize * fit, 0f)
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
        // A 360x800dp phone in a demo build: the top end corner counts as reaching down to the
        // bottom of the time button hanging under it, which makes it the taller of the two.
        val band = stageBandOf(
            width = 328,
            height = 716,
            topStart = playerCorner(),
            topEnd = CornerBlock(width = 167, height = 218),
            bottomStart = moneyCorner,
            bottomEnd = actionCorner,
            gap = 12
        )

        assertEquals(230, band.top)
        assertEquals(644, band.bottom)
        assertEquals(0, band.left)
        assertEquals(328, band.right)
        assertEquals(328, band.width)
        assertEquals(414, band.height)
    }

    @Test
    fun `a build without the time button hands the pet the room that button took`() {
        val corners = { topEnd: CornerBlock ->
            stageBandOf(
                width = 328,
                height = 716,
                topStart = playerCorner(),
                topEnd = topEnd,
                bottomStart = moneyCorner,
                bottomEnd = actionCorner,
                gap = 12
            )
        }

        val demo = corners(CornerBlock(width = 167, height = 218))
        val released = corners(controlsCorner)

        // Without the button the corner is the settings sprite and nothing else — shorter than
        // the player's things opposite, which is what the band then starts under.
        assertEquals(playerCorner().height + 12, released.top)
        assertTrue(playerCorner().height > controlsCorner.height)
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
            topStart = playerCorner(),
            topEnd = CornerBlock(width = 243, height = 64),
            bottomStart = moneyCorner,
            bottomEnd = actionCorner,
            gap = 12
        )

        // The start side is the strip of stats, which keeps the room of "100%" in every chip.
        assertEquals(274, band.left)
        assertEquals(513, band.right)
        assertEquals(0, band.top)
        assertEquals(280, band.bottom)
        assertEquals(239, band.width)
        assertEquals(280, band.height)
    }

    /**
     * The two buttons of the bottom start corner on a 360dp phone, where the row keeps 0.745 of its
     * full size: two sprites of 60 with a gap of 6 between them, and no room kept under either for
     * an underline neither can carry.
     */
    private val moneyCorner = CornerBlock(width = 125, height = 60)

    /** The three buttons of the bottom end corner on the same phone, measured the same way. */
    private val actionCorner = CornerBlock(width = 191, height = 60)

    /**
     * The block with the player's things, as an upright phone measures it.
     *
     * The width is set by the strip of three compact stat chips, which is the widest line of the
     * block and the line the goal card under it is cut to: a chip is an icon of 16, a gap of 4, the
     * room of "100%" — four glyphs of `labelSmall` — whatever the stat says, and 8 of padding on
     * either side, and three of them stand 8 apart. The height is the balance chip, that strip and
     * the goal card, with a gap between each. The goal card holds three lines — the name, the bar
     * with its percentage and the amount saved — inside 10 of padding above and below: 82 at the
     * ordinary text size, the same as the two-line card before it, and 5 more than that card with
     * the text turned up by 1.3, where the third line grows with the text — hence 181, not 176.
     *
     * @param large whether the system text is turned up, which is what makes every one of them
     *   wider and taller.
     * @return Size of the block, in dp.
     */
    private fun playerCorner(large: Boolean = false): CornerBlock = CornerBlock(
        width = if (large) 304 else 262,
        height = if (large) 181 else 158
    )

    /**
     * The settings button of the top end corner, as an upright phone measures it: the sprite and
     * nothing else, since a button belonging to no group of tabs keeps no room under itself for the
     * underline that would mark one.
     */
    private val controlsCorner = CornerBlock(width = 64, height = 64)

    /**
     * @param screenWidth width of the screen, in dp.
     * @param large whether the system text is turned up, which is what makes the time button wider.
     * @return Where the top of the time button goes on such a screen, in dp from the top.
     */
    private fun timeButtonTopOn(screenWidth: Float, large: Boolean = false): Int = timeButtonTopOf(
        width = (screenWidth - 16f * 2).toInt(),
        // "+12 ч": five glyphs of `bodyMedium` and 12 of padding on either side, compact.
        timeButtonWidth = if (large) 110 else 90,
        topStart = playerCorner(large),
        topEnd = controlsCorner,
        gap = 12
    )

    @Test
    fun `on an ordinary phone the time button stands beside the player's things, under the settings`() {
        // 411dp, with the system text as it comes: the strip of stats takes 262 of the 379, and the
        // short "+12 ч" fits in what is left, so it follows the settings button above it — level
        // with the top of the goal card — instead of standing alone below the whole corner.
        assertEquals(controlsCorner.height + 12, timeButtonTopOn(screenWidth = 411f))
    }

    @Test
    fun `on the narrowest phone the time button hangs below the player's things`() {
        // 360dp: 328 less the 262 of the stats leaves no room for the button beside them.
        assertEquals(playerCorner().height + 12, timeButtonTopOn(screenWidth = 360f))
    }

    @Test
    fun `a time button wide enough to reach the player's things steps down past them`() {
        // With the system text turned up the button is wider and the corner opposite is taller, so
        // following the buttons above it would have it run over the goal card.
        val large = timeButtonTopOn(screenWidth = 360f, large = true)

        assertEquals(playerCorner(large = true).height + 12, large)
        assertTrue(large > controlsCorner.height + 12)
    }

    @Test
    fun `a time button with the whole width to itself follows the buttons it belongs to`() {
        // A tablet held upright: the button ends nowhere near the player's things.
        val onTablet = timeButtonTopOn(screenWidth = 800f, large = true)

        assertEquals(controlsCorner.height + 12, onTablet)
    }

    @Test
    fun `the time button never lies over the player's things, whatever the screen`() {
        listOf(320f, 360f, 411f, 800f).forEach { screenWidth ->
            listOf(false, true).forEach { large ->
                val width = (screenWidth - 16f * 2).toInt()
                val timeButtonWidth = if (large) 110 else 90
                val top = timeButtonTopOn(screenWidth = screenWidth, large = large)
                val player = playerCorner(large)

                // Either it starts after the player's things end, or it hangs below them.
                val clearAcross = width - timeButtonWidth >= player.width + 12
                val clearBelow = top >= player.height + 12
                assertTrue(clearAcross || clearBelow)
                // And it is always under the buttons it belongs to.
                assertTrue(top >= controlsCorner.height + 12)
            }
        }
    }

    @Test
    fun `a screen the corners alone fill leaves a band of no size rather than an upside-down one`() {
        // A 320x380dp window with the text turned up: the corners want more of the 300 there are
        // than the screen has, and the pet is left with nothing between them.
        val band = stageBandOf(
            width = 288,
            height = 300,
            topStart = playerCorner(large = true),
            topEnd = CornerBlock(width = 210, height = 236),
            bottomStart = moneyCorner,
            bottomEnd = actionCorner,
            gap = 12
        )

        assertEquals(0, band.height)
        assertEquals(band.top, band.bottom)
        assertTrue(band.top in 0..300)
        assertEquals(288, band.width)
    }

    @Test
    fun `a line of the player's things that fits beside the settings button stays where it is`() {
        // 411dp: the strip of stats is 262 wide, and the button leaves 303 of the 379 beside it.
        assertEquals(44, clearedTopOf(top = 44, width = 262, roomBeside = 303, clearanceBottom = 72))
    }

    @Test
    fun `a line too wide to stand beside the settings button goes down past it`() {
        // 360dp: the button leaves 252 of the 328, and the strip of stats needs 262 — or 304 with
        // the system text turned up. Squeezed in beside the button, it took the button's own room
        // and left the button drawn at no size at all.
        assertEquals(72, clearedTopOf(top = 44, width = 262, roomBeside = 252, clearanceBottom = 72))
        assertEquals(72, clearedTopOf(top = 44, width = 304, roomBeside = 252, clearanceBottom = 72))
    }

    @Test
    fun `a line already below the settings button is not moved at all`() {
        assertEquals(104, clearedTopOf(top = 104, width = 304, roomBeside = 252, clearanceBottom = 72))
    }

    @Test
    fun `the time button stays put while the stats run down`() {
        // Every stat chip keeps the room of "100%", so the corner is one width at 100% and at 0%, and
        // the button a second tap is aimed at is still where the first one found it.
        val full = timeButtonTopOn(screenWidth = 411f)
        val empty = timeButtonTopOf(
            width = (411f - 16f * 2).toInt(),
            timeButtonWidth = 90,
            topStart = playerCorner(),
            topEnd = controlsCorner,
            gap = 12
        )
        assertEquals(full, empty)
    }
}
