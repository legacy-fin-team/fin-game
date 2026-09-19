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
     * Получить список предметов определенной категории (например, для 1 вкладки магазина)
     */
    fun getItemsByCategory(category: ItemCategory): List<Item> {
        return itemsByCategory.getValue(category).values.toList()
    }

    /**
     * Получить путь до ассетов конкретного варианта предмета.
     */
    fun getVariantPath(itemId: String, variantId: String): String {
        val item = itemsById.getValue(itemId)
        return item.variants.getValue(variantId)
    }

    /**
     * Получить сам предмет по его id (ищет сразу среди всех).
     */
    fun getItemById(itemId: String): Item {
        return itemsById.getValue(itemId)
    }
}
