package com.legacy.fingame.game.animals

import android.content.Context

class AnimalRegistry(context: Context) {
    private val animalsMap: Map<String, Animal>

    init {
        val reader = AnimalReader()
        animalsMap = context.assets.open("data/animals.xml").use { inputStream ->
            reader.readAnimals(inputStream)
        }
    }

    /**
     * @return List of [Animal].
     */
    fun getAllAnimals(): List<Animal> {
        return animalsMap.values.toList()
    }

    /**
     * @param animalId id of an animal.
     * @param variantId id of an animal variant.
     * @return Path to animal variant relative to /assets/textures/.
     */
    fun getVariantPath(animalId: String, variantId: String): String {
        val animal = animalsMap.getValue(animalId)
        return animal.variants.getValue(variantId)
    }
    
    /**
     * @param animalId id of an animal.
     * @return List of animal variants
     */
    fun getAnimalById(animalId: String): Animal {
        return animalsMap.getValue(animalId)
    }
}
