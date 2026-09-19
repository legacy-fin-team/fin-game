package com.legacy.fingame.game.items

/**
 * What happens to an item in the inventory once the player uses it on the pet.
 *
 * Every [ItemCategory] picks one of these, so the inventory does not have to know the categories
 * themselves: it asks the category how its items are used and offers the matching action.
 */
enum class ItemUse {
    /** The item is gone once it is used: the pet ate it, so the inventory counter goes down by one. */
    CONSUMED,

    /** The item stays in the inventory and can be used again and again, like a toy. */
    REUSABLE,

    /**
     * The item stays in the inventory and is put on and taken off instead of being used up: it is
     * drawn in the game area for as long as it is on (see [ItemCategory.defaultLayer]).
     */
    WEARABLE
}
