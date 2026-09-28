package com.legacy.fingame

import com.legacy.fingame.game.animals.Animal
import com.legacy.fingame.game.animals.AnimalRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimalRegistryTest {

    private val cat = Animal(
        id = "cat",
        name = "Кот",
        ageCount = 3,
        variants = mapOf("orange" to "animals/cat/orange")
    )

    @Test
    fun `registry of broken data has nothing to pick from`() {
        val registry = AnimalRegistry(emptyMap())

        assertEquals(emptyList<Animal>(), registry.getAllAnimals())
    }

    @Test
    fun `a renamed variant is not reported as present`() {
        val renamed = cat.copy(variants = mapOf("ginger" to "animals/cat/ginger"))
        val registry = AnimalRegistry(mapOf(renamed.id to renamed))

        assertFalse(registry.hasVariant("cat", "orange"))
        assertTrue(registry.hasVariant("cat", "ginger"))
    }

    @Test
    fun `a removed animal is not reported as present`() {
        val registry = AnimalRegistry(emptyMap())

        assertFalse(registry.hasVariant("cat", "orange"))
    }

    @Test
    fun `a pet grown past its last stage is drawn at the last one`() {
        val registry = AnimalRegistry(mapOf(cat.id to cat))

        assertEquals(Animal.FIRST_AGE, registry.coerceAge("cat", -1))
        assertEquals("animals/cat/orange/2/idle.webp", registry.getIdleSpritePath("cat", "orange", 9))
    }

    @Test
    fun `animal age is coerced to max age stage when age exceeds ageCount`() {
        val registry = AnimalRegistry(mapOf(cat.id to cat))

        assertEquals(0, cat.coerceAge(0))
        assertEquals(1, cat.coerceAge(1))
        assertEquals(2, cat.coerceAge(2))
        assertEquals(2, cat.coerceAge(3))
        assertEquals(2, cat.coerceAge(9))

        assertEquals(0, registry.coerceAge("cat", 0))
        assertEquals(1, registry.coerceAge("cat", 1))
        assertEquals(2, registry.coerceAge("cat", 2))
        assertEquals(2, registry.coerceAge("cat", 3))
        assertEquals(2, registry.coerceAge("cat", 9))
    }
}
