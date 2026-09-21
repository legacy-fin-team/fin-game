package com.legacy.fingame.game.scene

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Where in the scene the pet stands, as a part of the side of the scene counted from its start.
 * Everything that lives in the room is drawn in the middle of it (see [GameLayer.sizeFraction]), so
 * the pet is exactly half a scene away from either edge.
 */
private const val PET_PLACE_IN_SCENE = 0.5f

/**
 * How far the scene is moved inside the window the game area looks at it through, in screen pixels,
 * counted from the scene sitting right in the middle of that window.
 *
 * The axes point the way the player's finger goes: a positive [y] means the scene has been pulled
 * down and shows what used to be above the window, exactly as dragging a map does.
 *
 * @property x how far the scene is moved to the right.
 * @property y how far the scene is moved down.
 */
data class SceneOffset(val x: Float, val y: Float) {

    /**
     * @param other a further move of the scene, e.g. how far a finger has just travelled.
     * @return The two moves one after the other. The result is not held to the scene in any way —
     * that is what [SceneViewport.clamp] is for.
     */
    operator fun plus(other: SceneOffset): SceneOffset = SceneOffset(x + other.x, y + other.y)

    companion object {

        /** The scene sitting right in the middle of the window, not moved anywhere. */
        val NONE: SceneOffset = SceneOffset(x = 0f, y = 0f)
    }
}

/**
 * The window the game area looks at the scene through, and the scene behind it.
 *
 * The scene — the room with the pet and everything standing in it — is drawn as one square picture
 * of [sceneSide] screen pixels, and the window is the card on the screen that shows it. On a phone
 * the scene is blown up past the card, so only a part of it is visible at a time and the player
 * drags the rest into view; on a screen with room to spare the whole scene fits and there is
 * nothing to drag (see [SceneViewport.of] and [isDraggable]).
 *
 * This is the whole geometry of the game area and it knows nothing about Compose: the UI measures
 * the card, builds a viewport out of it and asks it where the scene may go.
 *
 * @property windowWidth width of the window, in screen pixels.
 * @property windowHeight height of the window, in screen pixels.
 * @property sceneSide side of the square the scene is drawn as, in screen pixels.
 */
data class SceneViewport(
    val windowWidth: Float,
    val windowHeight: Float,
    val sceneSide: Float
) {

    /**
     * How far the scene may be moved to either side before its own edge would come into the window.
     * Zero when the scene is no wider than the window, i.e. when there is nothing hidden to the
     * sides to drag into view.
     */
    val freeX: Float = max(0f, (sceneSide - windowWidth) / 2f)

    /** How far the scene may be moved up or down. The vertical twin of [freeX]. */
    val freeY: Float = max(0f, (sceneSide - windowHeight) / 2f)

    /**
     * Whether the player has anything to drag at all: `false` when the scene fits into the window
     * whole, so a screen big enough to show the entire room never moves it under the finger.
     */
    val isDraggable: Boolean = freeX > 0f || freeY > 0f

    /**
     * Where the scene stands when the game area is first shown: with the pet in the middle of the
     * window, as far as the edges of the scene allow.
     */
    val initialOffset: SceneOffset = focusedOn(
        sceneX = sceneSide * PET_PLACE_IN_SCENE,
        sceneY = sceneSide * PET_PLACE_IN_SCENE
    )

    /**
     * Holds a move of the scene to what there is to see.
     *
     * @param offset where the scene is being moved to, e.g. the last position plus the travel of
     *   the finger.
     * @return The same move cut down, along each axis on its own, to the point past which the edge
     * of the scene would come into the window and leave an empty band next to the room.
     */
    fun clamp(offset: SceneOffset): SceneOffset = SceneOffset(
        x = heldTo(free = freeX, moved = offset.x),
        y = heldTo(free = freeY, moved = offset.y)
    )

    /**
     * @param sceneX horizontal position of the thing to look at, in screen pixels from the start of
     *   the scene.
     * @param sceneY vertical position of the thing to look at, counted the same way.
     * @return The move that brings that spot of the scene into the middle of the window, [clamp]ed
     * to the scene — a spot near an edge is shown as close to the middle as the room allows.
     */
    fun focusedOn(sceneX: Float, sceneY: Float): SceneOffset = clamp(
        SceneOffset(x = sceneSide / 2f - sceneX, y = sceneSide / 2f - sceneY)
    )

    /**
     * Holds a move along one axis to what the scene has hidden along it.
     *
     * @param free how far the scene may go that way, as [freeX] and [freeY] state it.
     * @param moved how far it is being moved.
     * @return The move cut down to that much, and squarely zero when the axis has nothing hidden
     * to show — an axis with no room left has exactly one position the scene may take, and it is
     * the middle of the window.
     */
    private fun heldTo(free: Float, moved: Float): Float =
        if (free <= 0f) 0f else moved.coerceIn(-free, free)

    companion object {

        /**
         * Builds the viewport of a measured game area, picking how big the scene is drawn.
         *
         * The scene is pixel art, so it is only ever blown up by a whole number of screen pixels
         * per pixel of the artwork: anything else would smear the pixel grid across the screen and
         * make some pixels wider than their neighbours. Of those whole numbers the scene takes
         * [pixelSize] — the size a pixel of the game is meant to have — and falls back to the
         * largest blow-up that still fits into the window when the window is bigger than that, so
         * a tablet shows the whole room instead of a needlessly small picture in the middle of a
         * large card.
         *
         * The fit is measured against the shorter side of the window, so a scene said to fit fits
         * whole — the game area is a square card, where both sides are the same anyway.
         *
         * @param windowWidth width of the game area, in screen pixels.
         * @param windowHeight height of the game area, in screen pixels.
         * @param scenePixels side of the scene in the pixels the art itself is made of, i.e.
         *   [GameLayer.BACKGROUND]'s [GameLayer.spritePixels].
         * @param pixelSize how many screen pixels one pixel of the art should take up.
         * @return The viewport of that window with the scene sized to it.
         */
        fun of(
            windowWidth: Float,
            windowHeight: Float,
            scenePixels: Int,
            pixelSize: Float
        ): SceneViewport {
            val artPixels = max(1, scenePixels)
            val fitting = floor(min(windowWidth, windowHeight) / artPixels)
            val wanted = max(1, pixelSize.roundToInt()).toFloat()
            return SceneViewport(
                windowWidth = windowWidth,
                windowHeight = windowHeight,
                sceneSide = max(fitting, wanted) * artPixels
            )
        }
    }
}
