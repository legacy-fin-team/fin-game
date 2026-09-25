package com.legacy.fingame.game.items

import android.content.Context

class ItemRegistry(context: Context) : ItemCatalog {
    private val itemsByCategory: Map<ItemCategory, Map<String, Item>>
    
    private val itemsById: Map<String, Item>

    init {
        val reader = ItemReader()
        itemsByCategory = context.assets.open("data/items.xml").use { inputStream ->
            reader.readItems(inputStream)
        }

        val flatMap = mutableMapOf<String, Item>()
        itemsByCategory.values.forEach { categoryMap ->
            flatMap.putAll(categoryMap)
        }
        itemsById = flatMap.toMap()
    }

    /**
     * @param category a valid item category.
     * @return A list of [Item] that share the same category, empty when the data registers no item
     * of that category — the shop still has a section for it.
     */
    override fun getItemsByCategory(category: ItemCategory): List<Item> {
        return itemsByCategory[category]?.values?.toList() ?: emptyList()
    }

    /**
     * @param itemId id of an item.
     * @return [Item] data, or null when no item is registered under [itemId].
     */
    override fun findItemById(itemId: String): Item? {
        return itemsById[itemId]
    }

    /**
     * @param itemId id of an item.
     * @return [Item] data.
     */
    fun getItemById(itemId: String): Item {
        return itemsById.getValue(itemId)
    }
}
