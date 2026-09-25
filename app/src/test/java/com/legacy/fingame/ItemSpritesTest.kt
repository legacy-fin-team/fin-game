package com.legacy.fingame

import com.legacy.fingame.game.animals.Animal
import com.legacy.fingame.game.items.ItemSprites
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Where the items' pictures are looked for: every variant has a folder of its own, holding the icon
 * it is sold by, the clothes as they sit on a pet of one species and age, or the decoration as it
 * stands in the room.
 */
class ItemSpritesTest {

    @Test
    fun `a variant is shown in the shop and the inventory by an icon of its own folder`() {
        assertEquals("items/hat/black/icon.webp", ItemSprites.icon("hat", "black"))
        assertEquals("items/hat/white/icon.webp", ItemSprites.icon("hat", "white"))
        assertEquals("items/apple/default/icon.webp", ItemSprites.icon("apple", "default"))
    }

    @Test
    fun `clothes are painted per animal and per age stage, inside the variant's folder`() {
        assertEquals(
            "items/hat/black/equipped-cat-0.webp",
            ItemSprites.equippedOnAnimal(
                itemId = "hat",
                variantId = "black",
                animalId = "cat",
                animalAge = 0
            )
        )
        assertEquals(
            "items/hat/black/equipped-dog-0.webp",
            ItemSprites.equippedOnAnimal(
                itemId = "hat",
                variantId = "black",
                animalId = "dog",
                animalAge = 0
            )
        )
        assertEquals(
            "items/hat/white/equipped-cat-2.webp",
            ItemSprites.equippedOnAnimal(
                itemId = "hat",
                variantId = "white",
                animalId = "cat",
                animalAge = 2
            )
        )
    }

    @Test
    fun `a decoration is one picture per variant and knows nothing of the animal`() {
        assertEquals(
            "items/rug/beige/placed.webp",
            ItemSprites.placedInScenery(itemId = "rug", variantId = "beige")
        )
    }

    @Test
    fun `an item hands out the path its layer calls for`() {
        assertEquals(
            ItemSprites.icon(TestItems.HAT.id, "white"),
            TestItems.HAT.getIconPath("white")
        )
        assertEquals(
            ItemSprites.equippedOnAnimal(TestItems.HAT.id, "white", "cat", Animal.FIRST_AGE),
            TestItems.HAT.getEquippedSpritePath(
                variantId = "white",
                animalId = "cat",
                animalAge = Animal.FIRST_AGE
            )
        )
        assertEquals(
            ItemSprites.placedInScenery(TestItems.LAMP.id, "default"),
            TestItems.LAMP.getEquippedSpritePath(
                variantId = "default",
                animalId = "cat",
                animalAge = Animal.FIRST_AGE
            )
        )
    }

    @Test
    fun `food and toys have no worn look at all`() {
        assertNull(
            TestItems.APPLE.getEquippedSpritePath(
                variantId = "red",
                animalId = "cat",
                animalAge = Animal.FIRST_AGE
            )
        )
        assertNull(
            TestItems.BALL.getEquippedSpritePath(
                variantId = "red",
                animalId = "cat",
                animalAge = Animal.FIRST_AGE
            )
        )
    }

    @Test
    fun `a variant the item no longer has falls back to its first one`() {
        assertEquals(
            ItemSprites.icon(TestItems.HAT.id, TestItems.HAT.defaultVariantId),
            TestItems.HAT.getIconPath("chartreuse")
        )
        assertEquals(
            ItemSprites.equippedOnAnimal(
                TestItems.HAT.id,
                TestItems.HAT.defaultVariantId,
                "cat",
                Animal.FIRST_AGE
            ),
            TestItems.HAT.getEquippedSpritePath(
                variantId = "chartreuse",
                animalId = "cat",
                animalAge = Animal.FIRST_AGE
            )
        )
    }

    @Test
    fun `clothes cannot be drawn while no animal wears them`() {
        assertNull(
            TestItems.HAT.getEquippedSpritePath(
                variantId = "black",
                animalId = null,
                animalAge = Animal.FIRST_AGE
            )
        )
    }
}
