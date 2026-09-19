package com.legacy.fingame.game.items

/**
 * One line of the player's inventory: an item in one variant, how many of it are there and whether
 * it is currently on the pet.
 *
 * @property item the item itself, as the catalog registered it.
 * @property variantId variant the player owns it in; see [ItemSelection].
 * @property count how many of it the player has. Always at least one — a line for nothing owned
 * would be an empty cell.
 * @property worn whether the item is put on right now, i.e. drawn in the game area. Always false for
 * items that are not worn at all (see [Item.isWearable]).
 */
data class InventoryEntry(
    val item: Item,
    val variantId: String,
    val count: Int,
    val worn: Boolean
) {
    /** The item and the variant as one value, the way the player's state keys them. */
    val selection: ItemSelection get() = ItemSelection(item.id, variantId)

    /** Path to the sprite of the owned variant, relative to /assets/textures/. */
    val spritePath: String get() = item.getSpritePath(variantId)
}

/**
 * Turns what the player owns into the lines the inventory screen shows.
 *
 * The owned items themselves live in [com.legacy.fingame.game.PlayerState.owned] as bare ids, which
 * is all that has to be saved; this object is where those ids meet the catalog and become items with
 * a name, a sprite and an action.
 */
object Inventory {

    /**
     * Builds the inventory lines out of the player's state.
     *
     * Items that are no longer registered are left out rather than shown as broken cells: the data
     * files can change between two launches, and a player cannot be offered to feed the pet
     * something the game no longer knows.
     *
     * @param owned how many of each item and variant the player has.
     * @param worn which of them are put on right now.
     * @param catalog what the game knows about the items.
     * @return The lines to show, ordered by shop section and then by name, so the inventory looks the
     * same every time it is opened — the saved state itself keeps no order at all.
     */
    fun entriesOf(
        owned: Map<ItemSelection, Int>,
        worn: Set<ItemSelection>,
        catalog: ItemCatalog
    ): List<InventoryEntry> = owned.entries
        .filter { (_, count) -> count > 0 }
        .mapNotNull { (selection, count) ->
            val item = catalog.findItemById(selection.itemId) ?: return@mapNotNull null
            InventoryEntry(
                item = item,
                variantId = selection.variantId,
                count = count,
                worn = item.isWearable && selection in worn
            )
        }
        .sortedWith(
            compareBy(
                { it.item.category.ordinal },
                { it.item.title },
                { it.variantId }
            )
        )
}
