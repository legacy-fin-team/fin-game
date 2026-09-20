package com.legacy.fingame.game.scene

/**
 * Side, in its own pixels, of the art the scenery is drawn from: a sub-location background is a
 * square picture of this many pixels.
 */
private const val SCENERY_SPRITE_PIXELS = 128

/**
 * Side, in its own pixels, of the art of everything standing in the scenery — the pet, the things
 * around it and the clothes it wears. Four times smaller than [SCENERY_SPRITE_PIXELS], because a
 * room is drawn with four times as many pixels as the pet living in it.
 */
private const val STAGE_SPRITE_PIXELS = 32

/**
 * A layer of the game area, i.e. how far back or front something is drawn on the screen where the
 * pet lives.
 *
 * The area is stacked out of five layers, from the farthest one to the nearest one: the background,
 * the environment behind the pet, the pet itself, the environment in front of it and finally the
 * clothes it wears. Everything that is drawn in the game area belongs to exactly one of them, and
 * [zIndex] is what decides which of two things covers the other.
 *
 * @property xmlName name the item data files use to put an item on this layer.
 * @property zIndex depth of the layer: the higher the value, the closer to the player the layer is
 * drawn. Used verbatim as the Compose z-index of the layer, so the drawing order cannot drift away
 * from the order declared here.
 * @property spritePixels side of the art this layer is drawn from, in the pixels the art itself is
 * made of. The scenery is painted at a coarser resolution than what stands in it, and this is what
 * [sizeFraction] turns into on-screen sizes that keep every pixel equally big.
 */
enum class GameLayer(val xmlName: String, val zIndex: Float, val spritePixels: Int) {
    /** The farthest layer: the scenery of the sub-location the pet is in. */
    BACKGROUND("background", 0f, SCENERY_SPRITE_PIXELS),

    /** Things standing behind the pet, e.g. a rug or furniture it sits on. */
    ENVIRONMENT_BACK("environment-back", 1f, STAGE_SPRITE_PIXELS),

    /** The pet itself. */
    ANIMAL("animal", 2f, STAGE_SPRITE_PIXELS),

    /** Things standing in front of the pet and covering it, e.g. a lamp at the front of the room. */
    ENVIRONMENT_FRONT("environment-front", 3f, STAGE_SPRITE_PIXELS),

    /** The nearest layer: what the pet wears, drawn over the pet and over the whole room. */
    CLOTHES("clothes", 4f, STAGE_SPRITE_PIXELS);

    /**
     * How much of the game area a sprite of this layer takes up.
     *
     * The area is exactly the scenery stretched to its full side, so a layer drawn from smaller art
     * has to take up as much smaller a part of it: the pet, painted at a quarter of the resolution
     * of the room, is drawn a quarter of the area wide. That is what makes one pixel of the pet
     * exactly as big on the screen as one pixel of the room it sits in, instead of four times
     * bigger.
     */
    val sizeFraction: Float
        get() = spritePixels.toFloat() / BACKGROUND.spritePixels

    companion object {

        /** The layers from the farthest to the nearest, i.e. the order they have to be drawn in. */
        val DRAW_ORDER: List<GameLayer> = entries.sortedBy { it.zIndex }

        /**
         * @param layerId name of a layer, as the data files write it.
         * @return The layer with that name, or null when no layer goes by it — a layer named by data
         * that changed is not a layer the game can draw on.
         */
        fun fromString(layerId: String): GameLayer? = entries.find {
            it.xmlName.equals(layerId, ignoreCase = true)
        }
    }
}
