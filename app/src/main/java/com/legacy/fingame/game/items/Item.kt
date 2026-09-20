package com.legacy.fingame.game.items

import com.legacy.fingame.game.scene.GameLayer
import com.legacy.fingame.game.stats.StatKind

/**
 * An item the player can buy in the shop, use on the pet and — when the item is worn — see in the
 * game area.
 *
 * @property id id of the item.
 * @property name name of the item shown to the player.
 * @property price what the item costs, in coins, the same for every one of its variants.
 * @property category section of the shop the item belongs to, which also says how the item is used
 * once the player owns it (see [ItemCategory.use]).
 * @property variants map of paths to the item variants, in the order the data declares them. The
 * key is the variant id, the value is the path to the variant folder relative to /assets/textures/,
 * which holds the item's sprite. Never empty: an item with no variant at all could not be drawn.
 * @property effects what using the item does to the pet's stats, keyed by stat: a positive value
 * fills the bar, a negative one empties it (a cake that is sweet but not healthy). Empty for an item
 * the pet feels nothing about.
 * @property layer layer of the game area the item is drawn on while it is worn, or null for an item
 * that is never drawn there. Defaults to [ItemCategory.defaultLayer], so only items that stand apart
 * from their category — a decoration in front of the pet rather than behind it — name a layer of
 * their own in the data.
 */
data class Item(
    val id: String,
    val name: String,
    val price: Int,
    val category: ItemCategory,
    val variants: Map<String, String>,
    val effects: Map<StatKind, Int> = emptyMap(),
    val layer: GameLayer? = category.defaultLayer
) {
    companion object {
        /** Name of the sprite file inside a variant folder. */
        const val SPRITE_FILE = "item.webp"
    }

    init {
        require(variants.isNotEmpty()) { "Item '$id' has no variants." }
    }

    /** Whether the item is put on and taken off instead of being used up; see [ItemUse.WEARABLE]. */
    val isWearable: Boolean get() = category.use == ItemUse.WEARABLE && layer != null

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
