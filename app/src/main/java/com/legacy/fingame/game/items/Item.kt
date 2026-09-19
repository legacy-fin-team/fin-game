package com.legacy.fingame.game.items

/**
 * An item the player can buy in the shop.
 *
 * @property id id of the item.
 * @property title name of the item shown to the player.
 * @property price what the item costs, in coins, the same for every one of its variants.
 * @property category section of the shop the item belongs to.
 * @property variants map of paths to the item variants, in the order the data declares them. The
 * key is the variant id, the value is the path to the variant folder relative to /assets/textures/,
 * which holds the item's sprite. Never empty: an item with no variant at all could not be drawn.
 */
data class Item(
    val id: String,
    val title: String,
    val price: Int,
    val category: ItemCategory,
    val variants: Map<String, String>
) {
    companion object {
        /** Name of the sprite file inside a variant folder. */
        const val SPRITE_FILE = "item.webp"
    }

    init {
        require(variants.isNotEmpty()) { "Item '$id' has no variants." }
    }

    /** Ids of the item's variants, in the order the data declares them. */
    val variantIds: List<String> get() = variants.keys.toList()

    /** Variant the item is shown and bought in until the player picks another one. */
    val defaultVariantId: String get() = variants.keys.first()

    /**
     * Whether the item is offered in more than one variant, i.e. whether the player has a choice to
     * make before buying it.
     */
    val hasSeveralVariants: Boolean get() = variants.size > 1

    /**
     * Builds the path to the sprite of one of the item's variants.
     *
     * @param variantId id of a variant. A variant this item doesn't have falls back to
     * [defaultVariantId], so a variant id saved before the data changed still draws the item.
     * @return Path to the sprite relative to /assets/textures/, e.g. 'items/hat/black/item.webp'.
     */
    fun getSpritePath(variantId: String): String {
        val variantPath = variants[variantId] ?: variants.getValue(defaultVariantId)
        return "$variantPath/$SPRITE_FILE"
    }
}
