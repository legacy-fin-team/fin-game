package com.legacy.fingame.game.items

/**
 * What is on sale, as the game logic sees it: the items of a category and the item behind an id.
 *
 * The logic that spends the player's money has to know the price of what it is buying, but must not
 * care where the items were read from, so it only ever sees this interface. The catalog the app
 * runs on is [ItemRegistry], which reads the items from the asset data files.
 */
interface ItemCatalog {

    /**
     * @param category category to list.
     * @return Every registered item of [category], in the order the data declares them, or an empty
     * list when the category has no items at all — a category is a section of the shop whether or
     * not anything is on its shelves yet.
     */
    fun getItemsByCategory(category: ItemCategory): List<Item>

    /**
     * @param itemId id of an item.
     * @return The item with that id, or null when nothing is registered under it: ids can come from
     * a state saved back when the item still existed, and a missing item is data that changed, not
     * a bug to crash on.
     */
    fun findItemById(itemId: String): Item?

    companion object {

        /** Catalog with nothing on sale, for previews and for tests that never buy anything. */
        val EMPTY: ItemCatalog = object : ItemCatalog {

            override fun getItemsByCategory(category: ItemCategory): List<Item> = emptyList()

            override fun findItemById(itemId: String): Item? = null
        }
    }
}
