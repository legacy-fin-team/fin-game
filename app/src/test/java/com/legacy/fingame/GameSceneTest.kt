package com.legacy.fingame

import com.legacy.fingame.game.animals.Animal
import com.legacy.fingame.game.animals.AnimalRegistry
import com.legacy.fingame.game.animals.Growth
import com.legacy.fingame.game.items.Inventory
import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.game.items.ItemSprites
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
    private val animalId = "cat"
    private val animalAge = 2

    /**
     * @param worn items the pet has on.
     * @return The game area built out of them, with the scenery and the pet in place.
     */
    private fun sceneWith(worn: Set<ItemSelection>) = GameScene.of(
        background = background,
        pet = pet,
        animalId = animalId,
        worn = worn,
        catalog = catalog,
        animalAge = animalAge
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
    fun `the pet is drawn four times smaller than the room, so pixels match`() {
        // The room and everything standing in it are painted at the same resolution, so they take
        // up the area as a whole.
        listOf(
            GameLayer.BACKGROUND,
            GameLayer.ENVIRONMENT_BACK,
            GameLayer.ENVIRONMENT_FRONT
        ).forEach { layer ->
            assertEquals("$layer is not drawn at the size of the room", 1f, layer.sizeFraction, 0f)
        }

        // The pet and what it wears are painted four times finer, hence a quarter of the area.
        listOf(GameLayer.ANIMAL, GameLayer.CLOTHES).forEach { layer ->
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
            listOf(
                ItemSprites.equippedOnAnimal(TestItems.HAT.id, "black", animalId, animalAge)
            ),
            scene[GameLayer.CLOTHES].map { it.assetPath }
        )
        assertEquals(
            listOf(ItemSprites.placedInScenery(TestItems.LAMP.id, "default")),
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
            animalId = null,
            worn = emptySet(),
            catalog = catalog
        )

        GameLayer.entries.forEach { layer -> assertTrue(scene[layer].isEmpty()) }
    }

    @Test
    fun `clothes are not drawn while there is no pet to cut them for`() {
        val scene = GameScene.of(
            background = background,
            pet = null,
            animalId = null,
            worn = setOf(
                ItemSelection(TestItems.HAT.id, "black"),
                ItemSelection(TestItems.LAMP.id, "default")
            ),
            catalog = catalog
        )

        assertTrue(scene[GameLayer.CLOTHES].isEmpty())
        // A decoration belongs to the room and stands there pet or no pet.
        assertEquals(
            listOf(ItemSprites.placedInScenery(TestItems.LAMP.id, "default")),
            scene[GameLayer.ENVIRONMENT_FRONT].map { it.assetPath }
        )
    }

    @Test
    fun `the clothes of the pet grow with it, the room around it does not`() {
        val worn = setOf(
            ItemSelection(TestItems.HAT.id, "black"),
            ItemSelection(TestItems.LAMP.id, "default")
        )

        val grown = GameScene.of(
            background = background,
            pet = pet,
            animalId = animalId,
            worn = worn,
            catalog = catalog,
            animalAge = animalAge + 1
        )

        assertEquals(
            listOf(
                ItemSprites.equippedOnAnimal(TestItems.HAT.id, "black", animalId, animalAge + 1)
            ),
            grown[GameLayer.CLOTHES].map { it.assetPath }
        )
        assertEquals(
            sceneWith(worn)[GameLayer.ENVIRONMENT_FRONT].map { it.assetPath },
            grown[GameLayer.ENVIRONMENT_FRONT].map { it.assetPath }
        )
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
        assertEquals(ItemSprites.icon(TestItems.APPLE.id, "red"), entries.first().iconPath)
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

    @Test
    fun `clothes on a pet grown past its last stage are drawn at the stage the pet itself is`() {
        // A cat is painted at three stages, but it keeps growing: nine days of growth make it
        // "nine stages old", and its clothes used to be asked for as equipped-cat-9.
        val cat = Animal(
            id = animalId,
            name = "Кот",
            ageCount = 3,
            variants = mapOf("white" to "animals/cat/white")
        )
        val registry = AnimalRegistry(mapOf(cat.id to cat))
        val age = Growth.ageOf(growthMillis = 9 * Growth.STAGE_MILLIS)
        val stage = registry.coerceAge(animalId, age)

        val scene = GameScene.of(
            background = background,
            pet = SceneSprite(
                assetPath = registry.getIdleSpritePath(animalId, "white", stage),
                description = "Питомец"
            ),
            animalId = animalId,
            worn = setOf(ItemSelection(TestItems.HAT.id, "black")),
            catalog = catalog,
            animalAge = stage
        )

        assertEquals(9, age)
        assertEquals(cat.ageCount - 1, scene.animalAge)
        assertEquals("animals/cat/white/2/idle.webp", scene[GameLayer.ANIMAL].single().assetPath)
        assertEquals(
            listOf(ItemSprites.equippedOnAnimal(TestItems.HAT.id, "black", animalId, 2)),
            scene[GameLayer.CLOTHES].map { it.assetPath }
        )
    }
}
