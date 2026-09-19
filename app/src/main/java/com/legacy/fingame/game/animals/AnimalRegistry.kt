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
     * @param variantId id of an animal variant.
     * @param age age stage of the animal, coerced into the stages this animal actually has.
     * @return Path to the idle sprite of the animal variant relative to /assets/textures/.
     */
    fun getIdleSpritePath(animalId: String, variantId: String, age: Int): String {
        val animal = animalsMap.getValue(animalId)
        return animal.getIdleSpritePath(variantId, age)
    }

    /**
     * Checks that an animal variant is still present in the data, e.g. before using the
     * animal the player picked during an earlier run.
     *
     * @param animalId id of an animal.
     * @param variantId id of an animal variant.
     * @return true if the animal exists and has such a variant.
     */
    fun hasVariant(animalId: String, variantId: String): Boolean {
        return animalsMap[animalId]?.variants?.containsKey(variantId) == true
    }

    /**
     * @param animalId id of an animal.
     * @return [Animal] data.
     */
    fun getAnimalVariants(animalId: String): Animal {
        return animalsMap.getValue(animalId)
    }
}
