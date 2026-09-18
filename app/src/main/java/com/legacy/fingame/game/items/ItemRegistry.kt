package com.legacy.fingame.game.items

import android.content.Context

class ItemRegistry(context: Context) {
    // 1. Словарь, разделенный на 4 словаря по категориям
    private val itemsByCategory: Map<ItemCategory, Map<String, Item>>
    
    // 2. Дополнительно создаем единый плоский словарь всех предметов 
    // для быстрого поиска по id (чтобы не искать по всем категориям вручную)
    private val allItemsById: Map<String, Item>

    init {
        val reader = ItemReader()
        // Получаем доступ к файлу в assets/data/items.xml
        itemsByCategory = context.assets.open("data/items.xml").use { inputStream ->
            reader.readItems(inputStream)
        }

        val flatMap = mutableMapOf<String, Item>()
        itemsByCategory.values.forEach { categoryMap ->
            flatMap.putAll(categoryMap)
        }
        allItemsById = flatMap
    }

    /**
     * Получить список предметов определенной категории (например, для 1 вкладки магазина)
     */
    fun getItemsByCategory(category: ItemCategory): List<Item> {
        return itemsByCategory.getValue(category).values.toList()
    }

    /**
     * Получить словарь предметов определенной категории (ключ - id предмета)
     */
    fun getCategoryMap(category: ItemCategory): Map<String, Item> {
        return itemsByCategory.getValue(category)
    }

    /**
     * Получить путь до ассетов конкретного варианта предмета.
     */
    fun getVariantPath(itemId: String, variantId: String): String {
        val item = allItemsById.getValue(itemId)
        return item.variants.getValue(variantId)
    }

    /**
     * Получить сам предмет по его id (ищет сразу среди всех).
     */
    fun getItemById(itemId: String): Item {
        return allItemsById.getValue(itemId)
    }
}
