package com.legacy.fingame.game.items

import android.util.Log
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class ItemReader {

    companion object {
        private const val TAG = "ItemReader"
    }

    /**
     * Reads XML document. Doesn't do path data validation.
     * @param inputStream stream that reads XML file.
     * @return Map of items. The key is item id.
     */
    fun readItems(inputStream: InputStream): Map<ItemCategory, Map<String, Item>> {
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
                Log.e(TAG, "Tag <item> does not have 'id' attribute.")
                continue
            }

            val priceStr = itemNode.getAttribute("price")
            val price = priceStr.toIntOrNull()
            if (price == null) {
                Log.e(TAG, "Item with id '$itemId' does not have proper 'price' attribute.")
                continue
            }

            val categoryStr = itemNode.getAttribute("category")
            val category = ItemCategory.fromString(categoryStr)
            if (category == null) {
                Log.e(TAG, "Item with id '$itemId' does not have proper 'category' attribute.")
                continue
            }

            val variantsNodes = itemNode.getElementsByTagName("variants")
            if (variantsNodes.length == 0) {
                Log.e(TAG, "Item with id '$itemId' does not have <variants> tag.")
                continue
            }

            val variantsElement = variantsNodes.item(0) as Element
            val variantsPath = variantsElement.getAttribute("path")

            if (variantsPath.isNullOrBlank()) {
                Log.e(TAG, "<variants> tag of item with id '$itemId' " +
                        "does not have 'path' attribute.")
                continue
            }

            val variantNodes = variantsElement.getElementsByTagName("variant")
            val variantMap = getVariants(variantNodes, itemId, variantsPath)

            if (variantMap.isEmpty()) {
                Log.e(TAG, "<variants> tag of item with id '$itemId' is empty.")
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
        Log.i(TAG, "Loaded items: $totalItems. Loaded variants: $totalVariants.")

        return itemsByCategory
    }

    private fun getVariants(
        variantNodes: NodeList,
        itemId: String,
        variantsPath: String
    ): Map<String, String> {
        val variantMap = mutableMapOf<String, String>()

        for (i in 0 until variantNodes.length) {
            val variantNode = variantNodes.item(i)
            if (variantNode !is Element) {
                continue
            }

            val variantId = variantNode.getAttribute("id")
            if (variantId.isNullOrBlank()) {
                Log.e(TAG, "Tag <variant> of item with id '$itemId' " +
                        "doesn't have 'id' attribute.")
                continue
            }

            val fullPath = if (variantsPath.endsWith("/")) {
                "$variantsPath$variantId"
            } else {
                "$variantsPath/$variantId"
            }

            variantMap[variantId] = fullPath
        }

        return variantMap.toMap()
    }

}
