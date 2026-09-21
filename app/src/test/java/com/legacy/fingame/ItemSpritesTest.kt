package com.legacy.fingame

import com.legacy.fingame.game.items.ItemSprites
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Where the items' pictures are looked for: one icon per item, one worn sprite per animal and
 * variant for clothes, and one per variant for what stands in the room.
 */
class ItemSpritesTest {

    @Test
    fun `an item is shown in the shop and the inventory by one icon of its own folder`() {
        assertEquals("items/hat/icon.webp", ItemSprites.icon("hat"))
        assertEquals("items/apple/icon.webp", ItemSprites.icon("apple"))
    }

    @Test
    fun `clothes are painted per animal and per variant of the item itself`() {
        assertEquals(
            "items/hat/equipped-cat-black.webp",
            ItemSprites.equippedOnAnimal(itemId = "hat", animalId = "cat", variantId = "black")
        )
        assertEquals(
            "items/hat/equipped-dog-black.webp",
            ItemSprites.equippedOnAnimal(itemId = "hat", animalId = "dog", variantId = "black")
        )
    }

    @Test
    fun `a decoration is painted per variant and knows nothing of the animal`() {
        assertEquals(
            "items/rug/equipped-beige.webp",
            ItemSprites.equippedInScenery(itemId = "rug", variantId = "beige")
        )
    }

    @Test
    fun `an item hands out the path its layer calls for`() {
        assertEquals(ItemSprites.icon(TestItems.HAT.id), TestItems.HAT.iconPath)
        assertEquals(
            ItemSprites.equippedOnAnimal(TestItems.HAT.id, "cat", "white"),
            TestItems.HAT.getEquippedSpritePath(variantId = "white", animalId = "cat")
        )
        assertEquals(
            ItemSprites.equippedInScenery(TestItems.LAMP.id, "default"),
            TestItems.LAMP.getEquippedSpritePath(variantId = "default", animalId = "cat")
        )
    }

    @Test
    fun `food and toys have no worn look at all`() {
        assertNull(TestItems.APPLE.getEquippedSpritePath(variantId = "red", animalId = "cat"))
        assertNull(TestItems.BALL.getEquippedSpritePath(variantId = "red", animalId = "cat"))
    }

    @Test
    fun `a variant the item no longer has falls back to its first one`() {
        assertEquals(
            ItemSprites.equippedOnAnimal(TestItems.HAT.id, "cat", TestItems.HAT.defaultVariantId),
            TestItems.HAT.getEquippedSpritePath(variantId = "chartreuse", animalId = "cat")
        )
    }

    @Test
    fun `clothes cannot be drawn while no animal wears them`() {
        assertNull(TestItems.HAT.getEquippedSpritePath(variantId = "black", animalId = null))
    }
}
