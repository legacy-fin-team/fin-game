package com.legacy.fingame

import android.content.Context
import java.io.InputStream

class AnimalStorage(private val context: Context) {

    private val xmlReader = AnimalXmlReader()
    private var animals: List<Animal> = emptyList()

    /**
     * Инициализация хранилища.
     * @param inputStream Поток с XML-данными (например, из assets или res/raw).
     */
    fun initialize(inputStream: InputStream) {
        animals = xmlReader.readAnimals(inputStream)
    }

    /**
     * Инициализация хранилища из файла в папке assets.
     */
    fun initializeFromAssets(fileName: String) {
        try {
            val inputStream = context.assets.open(fileName)
            initialize(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Получение всех животных.
     * Используется при создании питомца.
     */
    fun getAllAnimals(): List<Animal> {
        return animals
    }

    /**
     * Получение конкретных ассетов животного по их id.
     * @param animalId id животного
     * @param variantId id вида (варианта)
     */
    fun getAnimalAsset(animalId: String, variantId: String): AnimalVariant? {
        val animal = animals.find { it.id == animalId }
        return animal?.variants?.find { it.id == variantId }
    }
}