package com.legacy.fingame

import com.legacy.fingame.game.animals.Animal
import com.legacy.fingame.game.items.Item
import com.legacy.fingame.game.items.ItemCategory
import com.legacy.fingame.game.items.ItemReader
import com.legacy.fingame.game.scene.GameLayer
import com.legacy.fingame.game.stats.StatKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The item data as it is read out of the XML: what a `<variant>` now tells (its id alone, since the
 * sprites are named after the item), and that the data the app ships with says the same.
 */
class ItemReaderTest {

    private fun readItems(xml: String): Map<String, Item> =
        ItemReader().readItems(xml.trimIndent().byteInputStream())
            .values
            .fold(emptyMap()) { all, ofCategory -> all + ofCategory }

    /**
     * @return The items of the real data file the app ships in its assets, read the way the app
     * reads them at startup.
     */
    private fun readShippedItems(): Map<String, Item> {
        // The unit tests run from the module directory, but a run from the root of the build tree
        // has to find the same file.
        val file = listOf(
            File("src/main/assets/data/items.xml"),
            File("app/src/main/assets/data/items.xml")
        ).firstOrNull { it.exists() }
        assertTrue("data/items.xml is not in the assets", file != null)

        return file!!.inputStream().use { stream ->
            ItemReader().readItems(stream)
                .values
                .fold(emptyMap<String, Item>()) { all, ofCategory -> all + ofCategory }
        }
    }

    @Test
    fun `reads item name, price, variants and effects`() {
        val items = readItems(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <items>
                <item id="cake" name="Пирожное" price="40" category="food">
                    <variants>
                        <variant id="default" />
                    </variants>
                    <effects>
                        <effect stat="hunger" value="30" />
                        <effect stat="health" value="-5" />
                    </effects>
                </item>
            </items>
            """
        )

        val cake = items.getValue("cake")
        assertEquals("Пирожное", cake.name)
        assertEquals(40, cake.price)
        assertEquals(ItemCategory.FOOD, cake.category)
        assertEquals(listOf("default"), cake.variantIds)
        assertEquals(mapOf(StatKind.HUNGER to 30, StatKind.HEALTH to -5), cake.effects)
    }

    @Test
    fun `effects declared on something the pet wears are read and then ignored`() {
        // Data written before clothes and decorations became a look and nothing more is still read
        // without an error — the item is simply worn to no effect.
        val items = readItems(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <items>
                <item id="hat" name="Шляпа" price="100" category="clothes">
                    <variants>
                        <variant id="black" />
                        <variant id="white" />
                    </variants>
                    <effects>
                        <effect stat="pleasure" value="10" />
                    </effects>
                </item>
                <item id="rug" name="Коврик" price="90" category="decor">
                    <variants>
                        <variant id="beige" />
                    </variants>
                    <effects>
                        <effect stat="pleasure" value="5" />
                    </effects>
                </item>
            </items>
            """
        )

        val hat = items.getValue("hat")
        assertEquals("Шляпа", hat.name)
        assertEquals(listOf("black", "white"), hat.variantIds)
        assertEquals(emptyMap<StatKind, Int>(), hat.effects)
        assertEquals(emptyMap<StatKind, Int>(), items.getValue("rug").effects)
    }

    @Test
    fun `a variant is its id alone, the sprites are found by the variant's own folder`() {
        val items = readItems(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <items>
                <item id="rug" name="Коврик" price="90" category="decor">
                    <variants>
                        <variant id="beige" />
                        <variant id="blue" />
                    </variants>
                </item>
            </items>
            """
        )

        val rug = items.getValue("rug")
        assertEquals("items/rug/blue/icon.webp", rug.getIconPath("blue"))
        assertEquals(
            "items/rug/blue/placed.webp",
            rug.getEquippedSpritePath(
                variantId = "blue",
                animalId = "cat",
                animalAge = Animal.FIRST_AGE
            )
        )
    }

    @Test
    fun `an item without variants is not read at all`() {
        val items = readItems(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <items>
                <item id="ball" name="Мячик" price="60" category="toys">
                    <variants>
                    </variants>
                </item>
                <item id="teddy" name="Мишка" price="120" category="toys">
                    <variants>
                        <variant id="default" />
                    </variants>
                </item>
            </items>
            """
        )

        assertNull(items["ball"])
        assertEquals(listOf("default"), items.getValue("teddy").variantIds)
    }

    @Test
    fun `an item is put on the layer its data names, or on its category's own`() {
        val items = readItems(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <items>
                <item id="lamp" name="Лампа" price="150" category="decor" layer="environment-front">
                    <variants>
                        <variant id="default" />
                    </variants>
                </item>
                <item id="rug" name="Коврик" price="90" category="decor">
                    <variants>
                        <variant id="beige" />
                    </variants>
                </item>
                <item id="apple" name="Яблоко" price="15" category="food">
                    <variants>
                        <variant id="default" />
                    </variants>
                </item>
            </items>
            """
        )

        assertEquals(GameLayer.ENVIRONMENT_FRONT, items.getValue("lamp").layer)
        assertEquals(GameLayer.ENVIRONMENT_BACK, items.getValue("rug").layer)
        assertNull(items.getValue("apple").layer)
    }

    @Test
    fun `the data the app ships with is read the same way`() {
        val items = readShippedItems()

        val bow = items.getValue("bow")
        assertEquals(ItemCategory.CLOTHES, bow.category)
        assertEquals(listOf("red", "green", "violet"), bow.variantIds)
        assertEquals("items/bow/red/icon.webp", bow.getIconPath("red"))
        assertEquals(
            "items/bow/violet/equipped-cat-1.webp",
            bow.getEquippedSpritePath(variantId = "violet", animalId = "cat", animalAge = 1)
        )

        val apple = items.getValue("apple")
        assertEquals(listOf("default"), apple.variantIds)
        assertNull(
            apple.getEquippedSpritePath(
                variantId = "default",
                animalId = "cat",
                animalAge = Animal.FIRST_AGE
            )
        )

        // Every item of the shipped data is still drawable: it has at least one variant, and every
        // variant is looked for in a folder of its own.
        items.values.forEach { item ->
            assertTrue("${item.id} has no variants", item.variantIds.isNotEmpty())
            item.variantIds.forEach { variantId ->
                assertEquals(
                    "items/${item.id}/$variantId/icon.webp",
                    item.getIconPath(variantId)
                )
            }
        }
    }

    @Test
    fun `the data the app ships with gives the pet nothing to feel about what it wears`() {
        val items = readShippedItems()

        // Clothes and decorations are bought to be looked at: their data declares no effect at all
        // any more, and the rule holds even if one crept back in.
        items.values.filter { it.isWearable }.forEach { item ->
            assertEquals(
                "${item.id} declares effects the pet cannot feel",
                emptyMap<StatKind, Int>(),
                item.declaredEffects
            )
            assertEquals(emptyMap<StatKind, Int>(), item.effects)
        }

        // Food and toys are the other half of the bargain: they are bought for what they do.
        assertEquals(
            mapOf(StatKind.HUNGER to 20, StatKind.HEALTH to 5),
            items.getValue("apple").effects
        )
        assertEquals(
            mapOf(StatKind.HUNGER to 50, StatKind.PLEASURE to 10, StatKind.HEALTH to 10),
            items.getValue("kibble").effects
        )
        assertEquals(
            mapOf(StatKind.PLEASURE to 20, StatKind.HUNGER to -5),
            items.getValue("ball").effects
        )
    }
}
