package com.legacy.fingame.game.animals

import java.io.InputStream

class AnimalRegistry(inputStream: InputStream) {
    // Словарь для хранения животных, где ключ - это id животного
    private val animalsMap: Map<String, Animal>

    init {
        val reader = AnimalReader()
        val animalsList = reader.readAnimals(inputStream)
        
        // Преобразуем список в словарь
        animalsMap = animalsList.associateBy { it.id }
    }

    /**
     * Получить список всех животных.
     * Используется при создании питомца.
     */
    fun getAllAnimals(): List<Animal> {
        return animalsMap.values.toList()
    }

    /**
     * Получить конкретный вариант (ассеты) животного по его id и id варианта (вида).
     * Используется в основной игре, чтобы отобразить животного на экране.
     */
    fun getAnimalVariant(animalId: String, variantId: String): AnimalVariant? {
        val animal = animalsMap[animalId]
        return animal?.variants?.find { it.id == variantId }
    }
    
    /**
     * Получить само животное по его id.
     */
    fun getAnimalById(animalId: String): Animal? {
        return animalsMap[animalId]
    }
}
