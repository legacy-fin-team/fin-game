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
     * Получить список всех животных.
     */
    fun getAllAnimals(): List<Animal> {
        return animalsMap.values.toList()
    }

    /**
     * Получить путь до ассетов конкретного варианта животного.
     */
    fun getVariantPath(animalId: String, variantId: String): String {
        val animal = animalsMap.getValue(animalId)
        return animal.variants.getValue(variantId)
    }
    
    /**
     * Получить список вариантов животного по id животного.
     */
    fun getAnimalById(animalId: String): Animal {
        return animalsMap.getValue(animalId)
    }
}
