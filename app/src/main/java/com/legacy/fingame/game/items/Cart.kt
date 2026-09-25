package com.legacy.fingame.game.items

import com.legacy.fingame.game.economy.SpendKind

/**
 * One line of the shop cart: an item in the variant it was picked in, and how many of it the player
 * is about to pay for.
 *
 * @property item the item itself, as the catalog registered it.
 * @property variantId variant the item would be bought in; see [ItemSelection].
 * @property quantity how many of it are in the cart. Always at least one — a line for nothing
 * picked would be a line about nothing.
 */
data class CartLine(
    val item: Item,
    val variantId: String,
    val quantity: Int
) {
    /** The item and the variant as one value, the way the player's state keys them. */
    val selection: ItemSelection get() = ItemSelection(item.id, variantId)

    /** Path to the icon of the picked variant, relative to /assets/textures/; see [Item.getIconPath]. */
    val iconPath: String get() = item.getIconPath(variantId)

    /** What this line costs altogether: the item's price taken [quantity] times. */
    val price: Int get() = item.price * quantity
}

/**
 * Turns the shop cart into the lines the purchase confirmation shows.
 *
 * The cart itself lives in [com.legacy.fingame.game.GameUiState] as bare item ids and counters,
 * which is all the shop screen needs to count with; this object is where those ids meet the catalog
 * and become items with a name, a sprite and a price.
 */
object Cart {

    /**
     * Builds the cart lines out of what the player put in the cart.
     *
     * Items that are no longer registered are left out rather than shown as broken lines: the data
     * files can change between two launches, and the player is never charged for what the shop
     * can't hand over (see [com.legacy.fingame.game.GameUiState.cartPrice], which counts them as
     * nothing for the same reason).
     *
     * @param quantities how many of each item is in the cart, keyed by item id.
     * @param pickedVariants which variant of an item the player wants, keyed by item id; an item
     * with nothing picked for it is bought in its [Item.defaultVariantId].
     * @param catalog what the game knows about the items.
     * @return The lines to show, ordered by shop section and then by name, so the list reads the
     * same way the shop itself is laid out — the cart keeps no order at all.
     */
    fun linesOf(
        quantities: Map<String, Int>,
        pickedVariants: Map<String, String>,
        catalog: ItemCatalog
    ): List<CartLine> = quantities.entries
        .filter { (_, quantity) -> quantity > 0 }
        .mapNotNull { (itemId, quantity) ->
            val item = catalog.findItemById(itemId) ?: return@mapNotNull null
            CartLine(
                item = item,
                variantId = pickedVariants[itemId] ?: item.defaultVariantId,
                quantity = quantity
            )
        }
        .sortedWith(
            compareBy(
                { it.item.category.ordinal },
                { it.item.name },
                { it.variantId }
            )
        )

    /**
     * What the cart is about to cost the period's plan: how much of it is a necessity and how much
     * is not.
     *
     * The shop section an item sits on is what decides which of the two it is (see
     * [ItemCategory.spendKind]), so the warning the player is shown before paying and the line the
     * period's report adds up afterwards are counted by one and the same rule.
     *
     * @param lines the cart lines, as [linesOf] built them.
     * @return What each kind of spending adds up to; a kind the cart holds nothing of is not in the
     * map at all, which is how a caller tells "nothing of this kind" from "nothing yet".
     */
    fun spendByKindOf(lines: List<CartLine>): Map<SpendKind, Int> = lines
        .groupBy { line -> line.item.category.spendKind }
        .mapValues { (_, kindLines) -> kindLines.sumOf { line -> line.price } }
}
