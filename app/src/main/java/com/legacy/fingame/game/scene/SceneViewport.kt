package com.legacy.fingame.game.scene

import kotlin.math.abs
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
 * How far a pinch has to carry the scale past the size the scene is drawn at before it is redrawn
 * one whole pixel larger or smaller.
 *
 * Half a step is where the nearest whole number changes; everything past that is the hold that
 * keeps a finger trembling right on that spot from flipping the scene between two sizes over and
 * over, which is what the pixel grid would be seen shivering as.
 */
private const val SCALE_STEP_HOLD = 0.6f

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
 * What a pinch left behind: the scene blown up to a new size, and the drag that keeps what was
 * between the fingers where it was.
 *
 * @property viewport the viewport at the new size.
 * @property offset how far the scene is moved inside it, already held to its edges.
 */
data class SceneZoom(val viewport: SceneViewport, val offset: SceneOffset)

/**
 * The window the game area looks at the scene through, and the scene behind it.
 *
 * The scene — the room with the pet and everything standing in it — is a square picture of
 * [scenePixels] pixels of artwork, blown up by [scale] screen pixels per pixel of that artwork. The
 * window is the card on the screen showing it, and it is square as well: as large as the game area
 * is allowed to be ([availableWidth] by [availableHeight]), or exactly as large as the scene when
 * the scene is the smaller of the two. That is why a screen with room to spare never shows a band
 * of the card around the room — there is no card left over to show (see [windowSide]).
 *
 * When the scene is the larger of the two only a part of it is visible at a time and the player
 * drags the rest into view; a pinch changes [scale] between [minScale] and [maxScale] (see
 * [zoomedAt]).
 *
 * This is the whole geometry of the game area and it knows nothing about Compose: the UI measures
 * how much room the area has, builds a viewport out of it and asks it how big the card is, how big
 * the scene is and where the scene may go.
 *
 * @property availableWidth widest the game area may be, in screen pixels.
 * @property availableHeight tallest the game area may be, in screen pixels.
 * @property scenePixels side of the scene in the pixels the artwork itself is made of.
 * @property scale how many screen pixels one pixel of the artwork takes up. Always a whole number:
 * the scene is pixel art, and anything else would smear its grid across the screen and make some
 * pixels wider than their neighbours.
 * @property minScale smallest [scale] the player may pinch down to, i.e. the largest whole blow-up
 * at which the whole room still fits into the game area.
 * @property maxScale largest [scale] the player may pinch up to.
 */
data class SceneViewport(
    val availableWidth: Float,
    val availableHeight: Float,
    val scenePixels: Int,
    val scale: Float,
    val minScale: Float,
    val maxScale: Float
) {

    /**
     * Side of the largest square the game area may take up: the shorter of the two sides it is
     * allowed to have, since the area is a square card. Zero when the area has not been measured
     * yet.
     */
    val availableSide: Float = max(0f, min(availableWidth, availableHeight))

    /** Side of the square the scene is drawn as, in screen pixels. */
    val sceneSide: Float = scenePixels * scale

    /**
     * Side of the square window the scene is shown through, in screen pixels: as much of the room
     * as the game area is allowed to take, and no more of the card than there is room to fill.
     */
    val windowSide: Float = min(availableSide, sceneSide)

    /**
     * How far the scene may be moved either way before its own edge would come into the window.
     * Zero when the scene is no larger than the window, i.e. when there is nothing hidden to drag
     * into view.
     */
    val free: Float = max(0f, (sceneSide - windowSide) / 2f)

    /**
     * Whether the player has anything to drag at all: `false` when the scene fits into the window
     * whole, so a window that shows the entire room never moves it under the finger.
     */
    val isDraggable: Boolean = free > 0f

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
        x = heldTo(offset.x),
        y = heldTo(offset.y)
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
     * @param scale how big a pixel of the artwork is wanted, in screen pixels.
     * @return That size rounded to a whole number of screen pixels and held between [minScale] and
     * [maxScale] — the sizes the scene may actually be drawn at.
     */
    fun heldScale(scale: Float): Float =
        max(1f, scale.roundToInt().toFloat()).coerceIn(minScale, maxScale)

    /**
     * @param scale how big a pixel of the artwork is to be, in screen pixels.
     * @return The same viewport with the scene drawn at that size, as far as [heldScale] allows it.
     */
    fun withScale(scale: Float): SceneViewport = copy(scale = heldScale(scale))

    /**
     * Follows a pinch that has carried the scale to [rawScale].
     *
     * A pinch travels smoothly, and the scene does not: it is only ever drawn at a whole number of
     * screen pixels per pixel of its artwork, so it steps from one size to the next as the fingers
     * pass the point between them — with a hold of [SCALE_STEP_HOLD] around that point, so fingers
     * resting on it do not make the pixel grid shiver between two sizes.
     *
     * @param rawScale the size the fingers have asked for, which is any number at all.
     * @return The viewport at the whole size that asks for, or this very viewport when the pinch
     * has not carried far enough to change the size the scene is drawn at.
     */
    fun steppedTo(rawScale: Float): SceneViewport {
        val asked = rawScale.coerceIn(minScale, maxScale)
        return if (abs(asked - scale) > SCALE_STEP_HOLD) withScale(asked) else this
    }

    /**
     * Blows the scene up or down around the spot the fingers hold, the way a map behaves: whatever
     * is between the fingers stays between them, and the rest of the room grows away from it.
     *
     * @param rawScale the size the pinch has carried the scale to, as [steppedTo] takes it.
     * @param focusX how far the middle of the pinch is to the right of the middle of the window, in
     *   screen pixels.
     * @param focusY how far it is below the middle of the window.
     * @param moved how far the scene is moved right now.
     * @return The viewport at the new size and the move that keeps the spot under the fingers where
     * it is, held to the edges of the scene at that size.
     */
    fun zoomedAt(
        rawScale: Float,
        focusX: Float,
        focusY: Float,
        moved: SceneOffset
    ): SceneZoom {
        val zoomed = steppedTo(rawScale)
        val grown = zoomed.scale / scale
        return SceneZoom(
            viewport = zoomed,
            offset = zoomed.clamp(
                SceneOffset(
                    x = focusX - (focusX - moved.x) * grown,
                    y = focusY - (focusY - moved.y) * grown
                )
            )
        )
    }

    /**
     * Holds a move along one axis to what the scene has hidden along it.
     *
     * @param moved how far the scene is being moved that way.
     * @return The move cut down to [free], and squarely zero when the scene has nothing hidden to
     * show — a scene that fits into the window whole has exactly one position it may take, and it
     * is the middle of the window.
     */
    private fun heldTo(moved: Float): Float =
        if (free <= 0f) 0f else moved.coerceIn(-free, free)

    companion object {

        /**
         * Builds the viewport of a measured game area, picking how big the scene is drawn to begin
         * with and how far the player may pinch it either way.
         *
         * The scene takes [pixelSize] — the size a pixel of the game is meant to have — and falls
         * back to the largest whole blow-up that still fits into the area when the area is bigger
         * than that, so a tablet shows the whole room instead of a needlessly small picture in the
         * middle of a large card. That same blow-up is as far as a pinch may take the scale down
         * ([minScale]): there is no point in shrinking the room past the card it is shown in, as
         * the card shrinks with it (see [windowSide]).
         *
         * @param availableWidth widest the game area may be, in screen pixels.
         * @param availableHeight tallest it may be, in screen pixels.
         * @param scenePixels side of the scene in the pixels the art itself is made of, i.e.
         *   [GameLayer.BACKGROUND]'s [GameLayer.spritePixels].
         * @param pixelSize how many screen pixels one pixel of the art should take up.
         * @param maxZoom how many times past the size it starts at the player may blow the scene
         *   up.
         * @return The viewport of that area with the scene sized to it.
         */
        fun of(
            availableWidth: Float,
            availableHeight: Float,
            scenePixels: Int,
            pixelSize: Float,
            maxZoom: Float
        ): SceneViewport {
            val artPixels = max(1, scenePixels)
            val side = max(0f, min(availableWidth, availableHeight))
            val fitting = max(1f, floor(side / artPixels))
            val wanted = max(1f, pixelSize.roundToInt().toFloat())
            val started = max(fitting, wanted)
            return SceneViewport(
                availableWidth = availableWidth,
                availableHeight = availableHeight,
                scenePixels = artPixels,
                scale = started,
                minScale = fitting,
                maxScale = max(started, (started * max(1f, maxZoom)).roundToInt().toFloat())
            )
        }
    }
}
