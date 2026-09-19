package com.legacy.fingame.game.scene

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
 */
enum class GameLayer(val xmlName: String, val zIndex: Float) {
    /** The farthest layer: the scenery of the sub-location the pet is in. */
    BACKGROUND("background", 0f),

    /** Things standing behind the pet, e.g. a rug or furniture it sits on. */
    ENVIRONMENT_BACK("environment-back", 1f),

    /** The pet itself. */
    ANIMAL("animal", 2f),

    /** Things standing in front of the pet and covering it, e.g. a lamp at the front of the room. */
    ENVIRONMENT_FRONT("environment-front", 3f),

    /** The nearest layer: what the pet wears, drawn over the pet and over the whole room. */
    CLOTHES("clothes", 4f);

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
