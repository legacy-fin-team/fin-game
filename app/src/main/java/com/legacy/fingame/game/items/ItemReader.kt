package com.legacy.fingame.game.items

import android.util.Log
import com.legacy.fingame.game.scene.GameLayer
import com.legacy.fingame.game.stats.StatKind
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class ItemReader {

    companion object {
        private const val TAG = "ItemReader"
    }

    /**
     * Reads XML document. An item whose data is broken is skipped with a log line rather than
     * taken along half-read.
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

            val categoryStr = itemNode.getAttribute("category")
            val category = ItemCategory.fromString(categoryStr)
            if (category == null) {
                Log.e(TAG, "Item with id '$itemId' does not have proper 'category' attribute.")
                continue
            }

            if (itemsByCategory.getValue(category).containsKey(itemId)) {
                Log.e(TAG, "At least two items share the same id: '$itemId'")
                continue
            }

            val priceStr = itemNode.getAttribute("price")
            val price = priceStr.toIntOrNull()
            if (price == null) {
                Log.e(TAG, "Item with id '$itemId' does not have proper 'price' attribute.")
                continue
            }

            val variantsNodes = itemNode.getElementsByTagName("variants")
            if (variantsNodes.length == 0) {
                Log.e(TAG, "Item with id '$itemId' does not have <variants> tag.")
                continue
            }

            val variantsElement = variantsNodes.item(0) as Element
            val variantNodes = variantsElement.getElementsByTagName("variant")
            val variantIds = getVariants(variantNodes, itemId)

            if (variantIds.isEmpty()) {
                Log.e(TAG, "<variants> tag of item with id '$itemId' is empty.")
                continue
            }

            // An item whose data doesn't name it is still playable, it is just called by its id.
            val name = itemNode.getAttribute("name").ifBlank { itemId }

            val item = Item(
                id = itemId,
                name = name,
                price = price,
                category = category,
                variantIds = variantIds,
                declaredEffects = getEffects(itemNode, itemId),
                layer = getLayer(itemNode, itemId, category)
            )
            itemsByCategory.getValue(category)[itemId] = item
            totalVariants += variantIds.size
        }

        val totalItems = itemsByCategory.values.sumOf { it.size }
        Log.i(TAG, "Loaded items: $totalItems. Loaded variants: $totalVariants.")

        return itemsByCategory
    }

    /**
     * Reads what using the item does to the pet's stats out of its `<effects>` tag.
     *
     * An effect naming a stat the pet doesn't have, or an amount that is not a number, is dropped
     * with a log line: the item is still worth having, it just doesn't do that one thing.
     *
     * Effects declared on an item the pet wears are read here all the same and then ignored by
     * [Item.effects]: clothes and decorations are a look and nothing more, so data that still names
     * an effect on a hat is read without an error and worn without one.
     *
     * @param itemNode the `<item>` tag being read.
     * @param itemId id of the item, for the log messages.
     * @return The effects the item declares, keyed by stat, empty when it declares none.
     */
    private fun getEffects(itemNode: Element, itemId: String): Map<StatKind, Int> {
        val effectNodes = itemNode.getElementsByTagName("effect")
        val effects = mutableMapOf<StatKind, Int>()

        for (i in 0 until effectNodes.length) {
            val effectNode = effectNodes.item(i)
            if (effectNode !is Element) {
                continue
            }

            val statStr = effectNode.getAttribute("stat")
            val stat = StatKind.fromString(statStr)
            if (stat == null) {
                Log.e(TAG, "Item with id '$itemId' has an <effect> on unknown stat '$statStr'.")
                continue
            }

            val value = effectNode.getAttribute("value").toIntOrNull()
            if (value == null) {
                Log.e(TAG, "<effect> on stat '$statStr' of item with id '$itemId' " +
                        "does not have proper 'value' attribute.")
                continue
            }

            if (effects.containsKey(stat)) {
                Log.e(TAG, "Item with id '$itemId' has more than one <effect> " +
                        "on stat '$statStr'.")
                continue
            }

            effects[stat] = value
        }

        return effects.toMap()
    }

    /**
     * Reads the game area layer the item is drawn on while it is worn.
     *
     * @param itemNode the `<item>` tag being read.
     * @param itemId id of the item, for the log messages.
     * @param category category of the item, whose [ItemCategory.defaultLayer] is used when the item
     * names no layer of its own.
     * @return The layer the item belongs on, or null when neither the item nor its category puts it
     * into the game area.
     */
    private fun getLayer(
        itemNode: Element,
        itemId: String,
        category: ItemCategory
    ): GameLayer? {
        val layerStr = itemNode.getAttribute("layer")
        if (layerStr.isNullOrBlank()) {
            return category.defaultLayer
        }

        val layer = GameLayer.fromString(layerStr)
        if (layer == null) {
            Log.e(TAG, "Item with id '$itemId' names unknown layer '$layerStr'.")
            return category.defaultLayer
        }

        return layer
    }

    /**
     * Reads the ids of the item's variants out of its `<variants>` tag.
     *
     * The variants carry no paths of their own: an item's sprites are named after the item and the
     * variant (see [ItemSprites]), so the id is all the data has to tell.
     *
     * @param variantNodes the `<variant>` tags of the item.
     * @param itemId id of the item, for the log messages.
     * @return The variant ids, in the order the data declares them, without repeats.
     */
    private fun getVariants(
        variantNodes: NodeList,
        itemId: String
    ): List<String> {
        val variantIds = mutableListOf<String>()

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

            if (variantIds.contains(variantId)) {
                Log.e(TAG, "At least two variants of item with id '$itemId' " +
                        "share the same id: '$variantId'")
                continue
            }

            variantIds.add(variantId)
        }

        return variantIds.toList()
    }

}
