package com.legacy.fingame

import androidx.compose.runtime.saveable.SaverScope
import com.legacy.fingame.game.scene.SceneOffset
import com.legacy.fingame.game.scene.SceneViewport
import com.legacy.fingame.ui.screens.SceneOffsetSaver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The window the game area looks at the scene through: how big the room is drawn, how big the card
 * showing it comes out, how far the player may drag it and how a pinch changes all of that.
 */
class SceneViewportTest {

    /** Side of the scene's artwork, i.e. what the background of a sub-location is painted at. */
    private val scenePixels = 128

    /**
     * @param availableWidth widest the game area may be, in screen pixels.
     * @param availableHeight tallest it may be; square, like the card itself, unless stated.
     * @param pixelSize screen pixels per pixel of the artwork the scene is meant to be drawn at.
     * @param maxZoom how many times past its starting size a pinch may blow the scene up.
     * @return The viewport of such an area.
     */
    private fun viewport(
        availableWidth: Float,
        availableHeight: Float = availableWidth,
        pixelSize: Float = 8f,
        maxZoom: Float = 2f
    ) = SceneViewport.of(
        availableWidth = availableWidth,
        availableHeight = availableHeight,
        scenePixels = scenePixels,
        pixelSize = pixelSize,
        maxZoom = maxZoom
    )

    /**
     * @param focusX a spot of the window, in screen pixels from the middle of it.
     * @param moved how far the scene is moved inside the window.
     * @return Which pixel of the artwork, counted from the middle of the scene, that spot of the
     * window is over — the thing a pinch has to keep under the fingers holding it.
     */
    private fun SceneViewport.artPixelUnder(focusX: Float, moved: SceneOffset): Float =
        (focusX - moved.x) / scale

    @Test
    fun `a small window blows the scene up past its edges, by whole pixels of the artwork`() {
        // 128 * 8 = 1024 screen pixels of scene in an area of 700.
        val window = viewport(availableWidth = 700f)

        assertEquals(8f, window.scale, 0f)
        assertEquals(1024f, window.sceneSide, 0f)
        assertEquals(0f, window.sceneSide % scenePixels, 0f)
        assertTrue(window.isDraggable)
    }

    @Test
    fun `an area the scene does not fit into keeps all of it, and the scene is cut off instead`() {
        val window = viewport(availableWidth = 700f)

        assertEquals(700f, window.windowSide, 0f)
        assertTrue(window.windowSide < window.sceneSide)
    }

    @Test
    fun `the window is always exactly what the area gave, and the scene covers it`() {
        // 128 * 12 = 1536 is the smallest whole blow-up of the art that still covers 1440.
        val window = viewport(availableWidth = 1440f)

        assertEquals(1440f, window.windowSide, 0f)
        assertTrue(window.sceneSide >= window.windowSide)
        assertTrue(window.isDraggable)
    }

    @Test
    fun `an area that is not square is measured by its shorter side, and the scene covers it`() {
        val window = viewport(availableWidth = 1200f, availableHeight = 600f, pixelSize = 3f)

        // 128 * 5 = 640 is the smallest whole blow-up of the art that still covers the 600 the
        // shorter side of the area allows.
        assertEquals(640f, window.sceneSide, 0f)
        assertEquals(600f, window.windowSide, 0f)
        assertTrue(window.sceneSide >= window.windowSide)
    }

    @Test
    fun `a phone held sideways gets as big a window as an upright one, out of the height left`() {
        // A 411x891dp phone at 2.625 screen pixels per dp. Upright, the width decides: 280dp of
        // area. On its side, the height left between the badge and the bonus button does: 277dp.
        val upright = viewport(availableWidth = 735f, availableHeight = 2254f, pixelSize = 7.875f)
        val sideways = viewport(availableWidth = 2129f, availableHeight = 727f, pixelSize = 7.875f)

        assertEquals(735f, upright.windowSide, 0f)
        assertEquals(727f, sideways.windowSide, 0f)
        assertEquals(upright.scale, sideways.scale, 0f)
        assertTrue(sideways.isDraggable)
    }

    @Test
    fun `a drag is held to the edges of the scene, each axis on its own`() {
        val window = viewport(availableWidth = 1024f, availableHeight = 512f, pixelSize = 12f)

        // 128 * 12 = 1536 of scene in a square card of 512: 512 of slack either way.
        assertEquals(1536f, window.sceneSide, 0f)
        assertEquals(512f, window.windowSide, 0f)
        assertEquals(512f, window.free, 0f)

        assertEquals(
            SceneOffset(x = 512f, y = 400f),
            window.clamp(SceneOffset(x = 1000f, y = 400f))
        )
        assertEquals(
            SceneOffset(x = -100f, y = -512f),
            window.clamp(SceneOffset(x = -100f, y = -9000f))
        )
    }

    @Test
    fun `dragging the finger down brings the scene down with it, up to the edge`() {
        val window = viewport(availableWidth = 700f)
        val dragged = window.clamp(window.initialOffset + SceneOffset(x = 0f, y = 120f))

        assertEquals(120f, dragged.y, 0f)
        assertEquals(window.free, window.clamp(dragged + SceneOffset(x = 0f, y = 9000f)).y, 0f)
    }

    @Test
    fun `the area starts out with the pet in the middle of the window`() {
        val window = viewport(availableWidth = 700f)

        // The pet stands in the middle of the scene, and the scene is centered when it has not
        // been moved, so there is nothing to move to put the pet in view.
        assertEquals(SceneOffset.NONE, window.initialOffset)
    }

    @Test
    fun `looking at a corner of the scene moves it no further than its own edge`() {
        val window = viewport(availableWidth = 700f)

        val corner = window.focusedOn(sceneX = 0f, sceneY = 0f)

        assertEquals(window.free, corner.x, 0f)
        assertEquals(window.free, corner.y, 0f)
        assertEquals(
            SceneOffset(x = -window.free, y = -window.free),
            window.focusedOn(sceneX = window.sceneSide, sceneY = window.sceneSide)
        )
    }

    @Test
    fun `a spot away from the middle is brought into the middle of the window`() {
        val window = viewport(availableWidth = 900f, pixelSize = 16f)

        // 128 * 16 = 2048 of scene in a window of 900: 574 of slack, so a spot 100 pixels off the
        // middle is simply brought to it.
        val spot = window.focusedOn(
            sceneX = window.sceneSide / 2f + 100f,
            sceneY = window.sceneSide / 2f - 100f
        )

        assertEquals(SceneOffset(x = -100f, y = 100f), spot)
    }

    @Test
    fun `an area that grew holds the scene to its new edges`() {
        val small = viewport(availableWidth = 512f, pixelSize = 12f)
        val dragged = small.clamp(SceneOffset(x = small.free, y = small.free))

        val grown = viewport(availableWidth = 1408f, pixelSize = 12f)
        val held = grown.clamp(dragged)

        assertEquals(SceneOffset(x = 64f, y = 64f), held)
        assertTrue(grown.isDraggable)
    }

    @Test
    fun `an area that grew keeps the scene as close to where it was dragged as the new edges allow`() {
        val small = viewport(availableWidth = 512f)
        val dragged = small.clamp(SceneOffset(x = 1000f, y = 1000f))

        val grown = viewport(availableWidth = 1440f)

        assertTrue(grown.isDraggable)
        assertEquals(SceneOffset(x = grown.free, y = grown.free), grown.clamp(dragged))
    }

    @Test
    fun `an area not measured yet still draws the scene at the size it is meant to have`() {
        val window = viewport(availableWidth = 0f, availableHeight = 0f)

        assertEquals(1024f, window.sceneSide, 0f)
        assertEquals(0f, window.windowSide, 0f)
        assertEquals(1f, window.minScale, 0f)
        assertEquals(SceneOffset(x = 10f, y = 10f), window.clamp(SceneOffset(x = 10f, y = 10f)))
        assertEquals(
            9f,
            window.zoomedAt(
                rawScale = 9f,
                focusX = 0f,
                focusY = 0f,
                moved = SceneOffset.NONE
            ).viewport.scale,
            0f
        )
    }

    @Test
    fun `the scene is only ever drawn at a whole number of screen pixels per pixel of the art`() {
        val window = viewport(availableWidth = 700f)

        assertEquals(0f, window.scale % 1f, 0f)
        assertEquals(9f, window.steppedTo(8.7f).scale, 0f)
        assertEquals(7f, window.steppedTo(7.2f).scale, 0f)
        assertEquals(11f, window.withScale(10.6f).scale, 0f)
    }

    @Test
    fun `a pinch resting between two sizes leaves the scene at the one it is drawn at`() {
        val window = viewport(availableWidth = 700f)

        // Half a step past is where the nearest whole size changes, and fingers trembling right
        // there would have the pixel grid shivering between two of them.
        assertEquals(8f, window.steppedTo(8.5f).scale, 0f)
        assertEquals(8f, window.steppedTo(7.5f).scale, 0f)
        assertEquals(9f, window.steppedTo(8.7f).scale, 0f)
        assertEquals(9f, window.steppedTo(8.7f).steppedTo(9.3f).scale, 0f)
    }

    @Test
    fun `a pinch keeps the pixel of the art between the fingers between the fingers`() {
        val window = viewport(availableWidth = 700f)
        val held = 120f
        val under = window.artPixelUnder(focusX = held, moved = SceneOffset.NONE)

        val zoomed = window.zoomedAt(
            rawScale = 9f,
            focusX = held,
            focusY = -60f,
            moved = SceneOffset.NONE
        )

        assertEquals(9f, zoomed.viewport.scale, 0f)
        assertEquals(
            under,
            zoomed.viewport.artPixelUnder(focusX = held, moved = zoomed.offset),
            0.001f
        )
    }

    @Test
    fun `a pinch goes no further than the sizes the scene may be drawn at`() {
        val window = viewport(availableWidth = 700f)

        // 128 * 6 = 768 is the smallest whole blow-up that still covers the area, and twice the
        // size the art is meant to have is as large as the room may get.
        assertEquals(6f, window.minScale, 0f)
        assertEquals(16f, window.maxScale, 0f)
        assertEquals(
            16f,
            window.zoomedAt(rawScale = 900f, focusX = 0f, focusY = 0f, moved = SceneOffset.NONE)
                .viewport.scale,
            0f
        )
        assertEquals(
            6f,
            window.zoomedAt(rawScale = 0.01f, focusX = 0f, focusY = 0f, moved = SceneOffset.NONE)
                .viewport.scale,
            0f
        )
    }

    @Test
    fun `a pinch that shrinks the scene never shrinks the card with it`() {
        val window = viewport(availableWidth = 700f)
        val dragged = window.clamp(SceneOffset(x = window.free, y = window.free))

        listOf(window.minScale, window.maxScale, 1f, 900f).forEach { rawScale ->
            val zoomed = window.zoomedAt(
                rawScale = rawScale,
                focusX = 0f,
                focusY = 0f,
                moved = dragged
            )
            assertEquals(700f, zoomed.viewport.windowSide, 0f)
        }
    }

    @Test
    fun `a pinch at the edge of the scene does not pull an empty band into view`() {
        val window = viewport(availableWidth = 700f)
        val corner = window.focusedOn(sceneX = 0f, sceneY = 0f)

        // Blowing the room up around the far corner of the window would carry the scene well past
        // its own edge if the drag were not held to it.
        val zoomed = window.zoomedAt(
            rawScale = 16f,
            focusX = -350f,
            focusY = -350f,
            moved = corner
        )

        assertEquals(16f, zoomed.viewport.scale, 0f)
        assertEquals(zoomed.viewport.free, zoomed.offset.x, 0f)
        assertEquals(zoomed.viewport.free, zoomed.offset.y, 0f)
    }

    @Test
    fun `a size the player pinched to is held to what an area of another shape allows`() {
        val phone = viewport(availableWidth = 700f)
        val pinched = phone.zoomedAt(
            rawScale = 6f,
            focusX = 0f,
            focusY = 0f,
            moved = SceneOffset.NONE
        ).viewport

        // A tablet cannot draw the room that small: it would leave a band of the card around it.
        val tablet = viewport(availableWidth = 1440f)

        assertEquals(6f, pinched.scale, 0f)
        assertEquals(12f, tablet.heldScale(pinched.scale), 0f)
    }

    @Test
    fun `the card never changes size, however the player pinches the scene`() {
        val window = viewport(availableWidth = 735f, availableHeight = 2254f, pixelSize = 7.875f)
        val sizes = listOf(window.minScale, 1f, 8f, 12f, 900f)
            .map { window.withScale(it).windowSide }
        assertEquals(setOf(735f), sizes.toSet())
    }

    @Test
    fun `how far the scene was dragged comes back after the screen is recreated`() {
        val dragged = SceneOffset(x = -120.5f, y = 64f)

        val saved = with(SceneOffsetSaver) { SaverScope { true }.save(dragged) }

        assertEquals(listOf(-120.5f, 64f), saved)
        assertEquals(dragged, SceneOffsetSaver.restore(saved!!))
    }
}
