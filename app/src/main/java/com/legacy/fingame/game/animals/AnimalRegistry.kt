package com.legacy.fingame.game.animals

import android.content.Context

class AnimalRegistry(context: Context) {
    private val animalsMap: Map<String, Animal>

    init {
        val reader = AnimalReader()
        // Получаем доступ к файлу в assets/data/animals.xml
        animalsMap = context.assets.open("data/animals.xml").use { inputStream ->
            reader.readAnimals(inputStream)
        }
    }

    /**
     * Получить список всех животных.
     * Используется при создании питомца.
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
     * Получить само животное по его id.
     */
    fun getAnimalById(animalId: String): Animal {
        return animalsMap.getValue(animalId)
    }
}
