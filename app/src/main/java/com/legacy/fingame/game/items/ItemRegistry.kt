package com.legacy.fingame.game.items

import android.content.Context

class ItemRegistry(context: Context) {
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
     * @return A list of [Item] that share the same category.
     */
    fun getItemsByCategory(category: ItemCategory): List<Item> {
        return itemsByCategory.getValue(category).values.toList()
    }

    /**
     * @param itemId id of an item.
     * @param variantId id of an item variant.
     * @return The asset path to variant.
     */
    fun getVariantPath(itemId: String, variantId: String): String {
        val item = itemsById.getValue(itemId)
        return item.variants.getValue(variantId)
    }

    /**
     * @param itemId id of an item.
     * @return [Item] data.
     */
    fun getItemById(itemId: String): Item {
        return itemsById.getValue(itemId)
    }
}
