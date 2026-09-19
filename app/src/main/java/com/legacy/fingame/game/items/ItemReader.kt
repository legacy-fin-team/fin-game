package com.legacy.fingame.game.items

import android.util.Log
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class ItemReader {

    companion object {
        private const val TAG = "ItemReader"
    }

    /**
     * Считывает XML документ и возвращает словарь, где ключ - это категория,
     * а значение - словарь предметов этой категории (по id предмета).
     */
    fun readItems(inputStream: InputStream): Map<ItemCategory, Map<String, Item>> {
        // Подготавливаем словари для каждой категории
        val itemsByCategory = ItemCategory.entries.associateWith { mutableMapOf<String, Item>() }
        var totalVariants = 0

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(inputStream)
        document.documentElement.normalize()

        val itemNodes = document.getElementsByTagName("item")
        for (i in 0 until itemNodes.length) {
            val itemNode = itemNodes.item(i)
            if (itemNode !is Element) {
                continue
            }

            val itemId = itemNode.getAttribute("id")
            if (itemId.isNullOrBlank()) {
                val msg = "Отсутствует обязательный атрибут 'id' у тега <item>."
                Log.e(TAG, msg)
                continue
            }

            val priceStr = itemNode.getAttribute("price")
            val price = priceStr.toIntOrNull()
            if (price == null) {
                val msg = "Отсутствует или неверный формат атрибута 'price' у предмета id = '$itemId'"
                Log.e(TAG, msg)
                continue
            }

            val categoryStr = itemNode.getAttribute("category")
            if (categoryStr.isNullOrBlank()) {
                val msg = "Отсутствует обязательный атрибут 'category' у предмета id = '$itemId'"
                Log.e(TAG, msg)
                continue
            }
            val category = ItemCategory.fromString(categoryStr)

            val variantsNodes = itemNode.getElementsByTagName("variants")
            if (variantsNodes.length == 0) {
                val msg = "Отсутствует обязательный тег <variants> у предмета id = '$itemId'"
                Log.e(TAG, msg)
                continue
            }

            val variantsElement = variantsNodes.item(0) as Element
            val variantsPath = variantsElement.getAttribute("path")

            if (variantsPath.isNullOrBlank()) {
                val msg = "У тега <variants> отсутствует обязательный атрибут 'path' (item id: $itemId)"
                Log.e(TAG, msg)
                continue
            }

            val variantMap = mutableMapOf<String, String>()
            val variantNodes = variantsElement.getElementsByTagName("variant")

            for (j in 0 until variantNodes.length) {
                val variantNode = variantNodes.item(j)
                if (variantNode is Element) {
                    val variantId = variantNode.getAttribute("id")

                    if (variantId.isNullOrBlank()) {
                        val msg = "У тега <variant> отсутствует обязательный атрибут 'id' (item id: $itemId)"
                        Log.e(TAG, msg)
                        continue
                    }

                    // Формируем полный путь
                    val fullPath = if (variantsPath.endsWith("/")) {
                        "$variantsPath$variantId"
                    } else {
                        "$variantsPath/$variantId"
                    }

                    variantMap[variantId] = fullPath
                }
            }

            if (variantMap.isEmpty()) {
                val msg = "У предмета с id = '$itemId' нет ни одного варианта (тега <variant>)."
                Log.e(TAG, msg)
                continue
            }

            val item = Item(
                id = itemId,
                price = price,
                category = category,
                variants = variantMap
            )
            itemsByCategory[category]?.put(itemId, item)
            totalVariants += variantMap.size
        }

        val totalItems = itemsByCategory.values.sumOf { it.size }
        Log.i(TAG, "Успешно загружено предметов: $totalItems. Всего вариантов загружено: $totalVariants.")

        return itemsByCategory
    }
}
