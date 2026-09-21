package com.legacy.fingame

import androidx.compose.runtime.saveable.SaverScope
import com.legacy.fingame.game.scene.SceneOffset
import com.legacy.fingame.game.scene.SceneViewport
import com.legacy.fingame.ui.screens.SceneOffsetSaver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The window the game area looks at the scene through: how big the room is drawn, how far the
 * player may drag it and where it stands to begin with.
 */
class SceneViewportTest {

    /** Side of the scene's artwork, i.e. what the background of a sub-location is painted at. */
    private val scenePixels = 128

    /**
     * @param windowWidth width of the game area, in screen pixels.
     * @param windowHeight height of the game area; square, like the card itself, unless stated.
     * @param pixelSize screen pixels per pixel of the artwork the scene is meant to be drawn at.
     * @return The viewport of such an area.
     */
    private fun viewport(
        windowWidth: Float,
        windowHeight: Float = windowWidth,
        pixelSize: Float = 8f
    ) = SceneViewport.of(
        windowWidth = windowWidth,
        windowHeight = windowHeight,
        scenePixels = scenePixels,
        pixelSize = pixelSize
    )

    @Test
    fun `a small window blows the scene up past its edges, by whole pixels of the artwork`() {
        // 128 * 8 = 1024 screen pixels of scene in a window of 700.
        val window = viewport(windowWidth = 700f)

        assertEquals(1024f, window.sceneSide, 0f)
        assertEquals(0f, window.sceneSide % scenePixels, 0f)
        assertTrue(window.isDraggable)
    }

    @Test
    fun `a window with room to spare shows the whole scene and lets nothing be dragged`() {
        // 128 * 11 = 1408 fits into 1440, while one more pixel per pixel of the art would not.
        val window = viewport(windowWidth = 1440f)

        assertEquals(1408f, window.sceneSide, 0f)
        assertTrue(window.sceneSide <= window.windowWidth)
        assertFalse(window.isDraggable)
        assertEquals(SceneOffset.NONE, window.clamp(SceneOffset(x = 300f, y = -300f)))
        assertEquals(SceneOffset.NONE, window.initialOffset)
    }

    @Test
    fun `a window that is not square is measured by its shorter side, so the scene fits whole`() {
        val window = viewport(windowWidth = 1200f, windowHeight = 600f, pixelSize = 3f)

        // 128 * 4 = 512 is the largest whole blow-up the shorter side of the window holds.
        assertEquals(512f, window.sceneSide, 0f)
        assertTrue(window.sceneSide <= window.windowHeight)
        assertFalse(window.isDraggable)
    }

    @Test
    fun `a drag is held to the edges of the scene, each axis on its own`() {
        val window = viewport(windowWidth = 1024f, windowHeight = 512f, pixelSize = 12f)

        // 128 * 12 = 1536 of scene: 256 of slack to the sides and 512 up and down.
        assertEquals(1536f, window.sceneSide, 0f)
        assertEquals(256f, window.freeX, 0f)
        assertEquals(512f, window.freeY, 0f)

        assertEquals(
            SceneOffset(x = 256f, y = 400f),
            window.clamp(SceneOffset(x = 1000f, y = 400f))
        )
        assertEquals(
            SceneOffset(x = -100f, y = -512f),
            window.clamp(SceneOffset(x = -100f, y = -9000f))
        )
    }

    @Test
    fun `dragging the finger down brings the scene down with it, up to the edge`() {
        val window = viewport(windowWidth = 700f)
        val dragged = window.clamp(window.initialOffset + SceneOffset(x = 0f, y = 120f))

        assertEquals(120f, dragged.y, 0f)
        assertEquals(window.freeY, window.clamp(dragged + SceneOffset(x = 0f, y = 9000f)).y, 0f)
    }

    @Test
    fun `the area starts out with the pet in the middle of the window`() {
        val window = viewport(windowWidth = 700f)

        // The pet stands in the middle of the scene, and the scene is centered when it has not
        // been moved, so there is nothing to move to put the pet in view.
        assertEquals(SceneOffset.NONE, window.initialOffset)
    }

    @Test
    fun `looking at a corner of the scene moves it no further than its own edge`() {
        val window = viewport(windowWidth = 700f)

        val corner = window.focusedOn(sceneX = 0f, sceneY = 0f)

        assertEquals(window.freeX, corner.x, 0f)
        assertEquals(window.freeY, corner.y, 0f)
        assertEquals(
            SceneOffset(x = -window.freeX, y = -window.freeY),
            window.focusedOn(sceneX = window.sceneSide, sceneY = window.sceneSide)
        )
    }

    @Test
    fun `a spot away from the middle is brought into the middle of the window`() {
        val window = viewport(windowWidth = 900f, pixelSize = 16f)

        // 128 * 16 = 2048 of scene in a window of 900: 574 of slack, so a spot 100 pixels off the
        // middle is simply brought to it.
        val spot = window.focusedOn(
            sceneX = window.sceneSide / 2f + 100f,
            sceneY = window.sceneSide / 2f - 100f
        )

        assertEquals(SceneOffset(x = -100f, y = 100f), spot)
    }

    @Test
    fun `a window that grew holds the scene to its new edges`() {
        val small = viewport(windowWidth = 512f, pixelSize = 12f)
        val dragged = small.clamp(SceneOffset(x = small.freeX, y = small.freeY))

        val grown = viewport(windowWidth = 1408f, pixelSize = 12f)
        val held = grown.clamp(dragged)

        assertEquals(SceneOffset(x = 64f, y = 64f), held)
        assertTrue(grown.isDraggable)
    }

    @Test
    fun `a window that grew past the scene puts it back in the middle`() {
        val small = viewport(windowWidth = 512f)
        val dragged = small.clamp(SceneOffset(x = 1000f, y = 1000f))

        val grown = viewport(windowWidth = 1440f)

        assertFalse(grown.isDraggable)
        assertEquals(SceneOffset.NONE, grown.clamp(dragged))
    }

    @Test
    fun `a window not measured yet still draws the scene at the size it is meant to have`() {
        val window = viewport(windowWidth = 0f, windowHeight = 0f)

        assertEquals(1024f, window.sceneSide, 0f)
        assertTrue(window.isDraggable)
    }

    @Test
    fun `how far the scene was dragged comes back after the screen is recreated`() {
        val dragged = SceneOffset(x = -120.5f, y = 64f)

        val saved = with(SceneOffsetSaver) { SaverScope { true }.save(dragged) }

        assertEquals(listOf(-120.5f, 64f), saved)
        assertEquals(dragged, SceneOffsetSaver.restore(saved!!))
    }
}
