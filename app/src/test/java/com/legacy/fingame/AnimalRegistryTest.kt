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
        title = "Кот",
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
}
