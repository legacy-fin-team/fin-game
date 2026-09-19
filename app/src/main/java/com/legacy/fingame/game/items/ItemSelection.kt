package com.legacy.fingame.game.items

/**
 * An item together with the variant it was picked in — what the player actually buys and owns.
 *
 * An item with several variants is a different thing to the player in each of them: a black hat and
 * a white one are bought, owned and worn apart from each other, so both ids are needed to name one.
 *
 * @property itemId id of the item, as in [Item.id].
 * @property variantId id of the variant, one of the keys of [Item.variants].
 */
data class ItemSelection(
    val itemId: String,
    val variantId: String
)
