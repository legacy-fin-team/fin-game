package com.legacy.fingame

import com.legacy.fingame.game.items.Inventory
import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.game.scene.GameLayer
import com.legacy.fingame.game.scene.GameScene
import com.legacy.fingame.game.scene.SceneSprite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The game area and the inventory as they are built out of the player's state: which layer a thing
 * ends up on, and what the inventory screen is handed to show.
 */
class GameSceneTest {

    private val catalog = FakeItemCatalog()
    private val background = SceneSprite(assetPath = "locations/0/background.webp", description = null)
    private val pet = SceneSprite(assetPath = "animals/cat/white/0/idle.webp", description = "Питомец")

    /**
     * @param worn items the pet has on.
     * @return The game area built out of them, with the scenery and the pet in place.
     */
    private fun sceneWith(worn: Set<ItemSelection>) = GameScene.of(
        background = background,
        pet = pet,
        worn = worn,
        catalog = catalog
    )

    @Test
    fun `the area is stacked out of five layers, from the scenery to the clothes`() {
        assertEquals(
            listOf(
                GameLayer.BACKGROUND,
                GameLayer.ENVIRONMENT_BACK,
                GameLayer.ANIMAL,
                GameLayer.ENVIRONMENT_FRONT,
                GameLayer.CLOTHES
            ),
            GameLayer.DRAW_ORDER
        )
        assertEquals(5, GameLayer.entries.size)
    }

    @Test
    fun `the scenery is drawn four times bigger than what stands in it, so pixels match`() {
        assertEquals(1f, GameLayer.BACKGROUND.sizeFraction, 0f)

        GameLayer.DRAW_ORDER.filter { it != GameLayer.BACKGROUND }.forEach { layer ->
            assertEquals(
                "$layer is not drawn on the same pixel grid as the scenery",
                0.25f,
                layer.sizeFraction,
                0f
            )
        }
    }

    @Test
    fun `the scenery and the pet stand on their own layers`() {
        val scene = sceneWith(emptySet())

        assertEquals(listOf(background), scene[GameLayer.BACKGROUND])
        assertEquals(listOf(pet), scene[GameLayer.ANIMAL])
        assertTrue(scene[GameLayer.CLOTHES].isEmpty())
    }

    @Test
    fun `clothes are drawn over the pet and a decoration where its own data puts it`() {
        val hat = ItemSelection(TestItems.HAT.id, "black")
        val lamp = ItemSelection(TestItems.LAMP.id, "default")

        val scene = sceneWith(setOf(hat, lamp))

        assertEquals(
            listOf(TestItems.HAT.getSpritePath("black")),
            scene[GameLayer.CLOTHES].map { it.assetPath }
        )
        assertEquals(
            listOf(TestItems.LAMP.getSpritePath("default")),
            scene[GameLayer.ENVIRONMENT_FRONT].map { it.assetPath }
        )
        assertTrue(scene[GameLayer.ENVIRONMENT_BACK].isEmpty())
    }

    @Test
    fun `an item that belongs on no layer is not drawn in the area at all`() {
        val apple = ItemSelection(TestItems.APPLE.id, "red")

        val scene = sceneWith(setOf(apple))

        GameLayer.entries.forEach { layer ->
            assertFalse(scene[layer].any { it.assetPath.contains(TestItems.APPLE.id) })
        }
    }

    @Test
    fun `an area without a pet or a scenery holds nothing at all`() {
        val scene = GameScene.of(
            background = null,
            pet = null,
            worn = emptySet(),
            catalog = catalog
        )

        GameLayer.entries.forEach { layer -> assertTrue(scene[layer].isEmpty()) }
    }

    @Test
    fun `the inventory shows what is owned, how many of it and what is on the pet`() {
        val hat = ItemSelection(TestItems.HAT.id, "black")
        val apple = ItemSelection(TestItems.APPLE.id, "red")

        val entries = Inventory.entriesOf(
            owned = mapOf(hat to 1, apple to 3),
            worn = setOf(hat),
            catalog = catalog
        )

        // Food comes before clothes, the way the shop sections are ordered.
        assertEquals(listOf(TestItems.APPLE.id, TestItems.HAT.id), entries.map { it.item.id })
        assertEquals(3, entries.first().count)
        assertFalse(entries.first().worn)
        assertTrue(entries.last().worn)
        assertEquals(TestItems.APPLE.getSpritePath("red"), entries.first().spritePath)
    }

    @Test
    fun `the inventory leaves out what the player has none of and what is no longer registered`() {
        val entries = Inventory.entriesOf(
            owned = mapOf(
                ItemSelection(TestItems.APPLE.id, "red") to 0,
                ItemSelection("item_nobody_sells", "default") to 2
            ),
            worn = emptySet(),
            catalog = catalog
        )

        assertTrue(entries.isEmpty())
    }
}
