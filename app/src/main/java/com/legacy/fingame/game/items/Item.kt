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
 * @property variantIds ids of the item's variants, in the order the data declares them. Never
 * empty: an item with no variant at all could not be drawn. The sprites themselves are not listed
 * here — their paths are built out of the item's id and the variant id by [ItemSprites].
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
    val variantIds: List<String>,
    val effects: Map<StatKind, Int> = emptyMap(),
    val layer: GameLayer? = category.defaultLayer
) {
    init {
        require(variantIds.isNotEmpty()) { "Item '$id' has no variants." }
    }

    /** Whether the item is put on and taken off instead of being used up; see [ItemUse.WEARABLE]. */
    val isWearable: Boolean get() = category.use == ItemUse.WEARABLE && layer != null

    /** Variant the item is shown and bought in until the player picks another one. */
    val defaultVariantId: String get() = variantIds.first()

    /**
     * Whether the item is offered in more than one variant, i.e. whether the player has a choice to
     * make before buying it.
     */
    val hasSeveralVariants: Boolean get() = variantIds.size > 1

    /**
     * Path to the icon the shop and the inventory show the item by, relative to /assets/textures/,
     * e.g. 'items/hat/icon.webp'. The same for every variant; see [ItemSprites.icon].
     */
    val iconPath: String get() = ItemSprites.icon(id)

    /**
     * Builds the path to the sprite of the item as it is worn, i.e. what the game area draws on the
     * item's [layer].
     *
     * Which sprite that is depends on the layer: something worn by the pet is painted per species
     * (see [ItemSprites.equippedOnAnimal]), while a decoration belongs to the room and is painted
     * once for all pets (see [ItemSprites.equippedInScenery]).
     *
     * @param variantId id of a variant. A variant this item doesn't have falls back to
     * [defaultVariantId], so a variant id saved before the data changed still draws the item.
     * @param animalId species id of the pet the item is worn by, or null when there is no pet;
     * needed by clothes alone, which are cut to fit the animal they sit on.
     * @return Path to the sprite relative to /assets/textures/, e.g. 'items/hat/equipped-cat-black.webp'
     * for clothes and 'items/rug/equipped-beige.webp' for a decoration; null for an item that is not
     * drawn in the game area at all — food and toys, which have no worn look — and for clothes with
     * no pet to put them on.
     */
    fun getEquippedSpritePath(variantId: String, animalId: String?): String? {
        val variant = if (variantId in variantIds) variantId else defaultVariantId
        return when (layer) {
            null -> null
            GameLayer.CLOTHES -> animalId?.let { ItemSprites.equippedOnAnimal(id, it, variant) }
            else -> ItemSprites.equippedInScenery(id, variant)
        }
    }
}
