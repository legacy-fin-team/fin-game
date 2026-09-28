package com.legacy.fingame.game.animals

import android.content.Context

/**
 * The animals the game knows about, as read from the animal data on start.
 *
 * The data may well yield no animals at all — [AnimalReader] drops every animal whose tag it can't
 * make sense of — so callers have to be ready for an empty registry instead of assuming that there
 * is always something to play with.
 *
 * @property animalsMap animals by their id.
 */
class AnimalRegistry(private val animalsMap: Map<String, Animal>) {

    /**
     * Reads the animals from `assets/data/animals.xml`.
     *
     * @param context current local application context. Used to get access to /assets/ folder.
     */
    constructor(context: Context) : this(
        context.assets.open("data/animals.xml").use { inputStream ->
            AnimalReader().readAnimals(inputStream)
        }
    )

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
     * @param animalId id of an animal.
     * @param age age stage the pet has grown to (see [Growth.ageAt]).
     * @return Age stage of this animal the pet is drawn at, see [Animal.getAgeStage].
     */
    fun getAgeStage(animalId: String, age: Int): Int {
        val animal = animalsMap.getValue(animalId)
        return animal.getAgeStage(age)
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
