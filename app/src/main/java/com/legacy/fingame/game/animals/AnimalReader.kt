package com.legacy.fingame.game.animals

import android.util.Log
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class AnimalReader {
    private val TAG = "AnimalReader"

    /**
     * Считывает XML документ и возвращает список животных.
     * Не проверяет реальное существование путей.
     */
    fun readAnimals(inputStream: InputStream): List<Animal> {
        val animals = mutableListOf<Animal>()
        var totalVariants = 0

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(inputStream)
        document.documentElement.normalize()

        val animalNodes = document.getElementsByTagName("animal")
        for (i in 0 until animalNodes.length) {
            val animalNode = animalNodes.item(i)
            if (animalNode is Element) {
                // Считываем обязательный атрибут id животного
                val animalId = animalNode.getAttribute("id")
                if (animalId.isNullOrBlank()) {
                    val msg = "Отсутствует обязательный атрибут 'id' у тега <animal>."
                    Log.e(TAG, msg)
                    throw IllegalArgumentException(msg)
                }

                // Считываем тег variants
                val variantsNodes = animalNode.getElementsByTagName("variants")
                if (variantsNodes.length == 0) {
                    val msg = "Отсутствует обязательный тег <variants> у животного с id = '$animalId'"
                    Log.e(TAG, msg)
                    throw IllegalArgumentException(msg)
                }

                val variantsElement = variantsNodes.item(0) as Element
                val variantsPath = variantsElement.getAttribute("path")
                
                if (variantsPath.isNullOrBlank()) {
                    val msg = "У тега <variants> отсутствует обязательный атрибут 'path' (animal id: $animalId)"
                    Log.e(TAG, msg)
                    throw IllegalArgumentException(msg)
                }

                val variantList = mutableListOf<AnimalVariant>()
                val variantNodes = variantsElement.getElementsByTagName("variant")

                for (j in 0 until variantNodes.length) {
                    val variantNode = variantNodes.item(j)
                    if (variantNode is Element) {
                        val variantId = variantNode.getAttribute("id")

                        if (variantId.isNullOrBlank()) {
                            val msg = "У тега <variant> отсутствует обязательный атрибут 'id' (animal id: $animalId)"
                            Log.e(TAG, msg)
                            throw IllegalArgumentException(msg)
                        }
                        variantList.add(AnimalVariant(id = variantId))
                    }
                }

                // =========================================================================
                // Место для считывания новых тегов.
                // Система легко расширяется: просто добавьте парсинг новых тегов здесь, 
                // не затрагивая уже написанный код для <variants>.
                // =========================================================================

                animals.add(Animal(id = animalId, variantsPath = variantsPath, variants = variantList))
                totalVariants += variantList.size
            }
        }

        // Логирование количества загруженных животных и их вариантов
        Log.i(TAG, "Успешно загружено животных: ${animals.size}. Всего вариантов загружено: $totalVariants.")
        
        return animals
    }
}
