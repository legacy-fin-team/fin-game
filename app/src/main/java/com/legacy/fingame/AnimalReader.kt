package com.legacy.fingame

import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

class AnimalXmlReader {

    private val TAG = "AnimalXmlReader"

    /**
     * Считывает XML-документ и возвращает список животных.
     * Класс спроектирован с учетом легкой расширяемости:
     * Для добавления новых тегов достаточно добавить новый case в блок when (parser.name).
     */
    fun readAnimals(inputStream: InputStream): List<Animal> {
        val animals = mutableListOf<Animal>()
        var totalVariants = 0

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(inputStream, null)

            var eventType = parser.eventType

            var currentAnimalId: String? = null
            var currentAnimalName: String? = null
            var currentVariants = mutableListOf<AnimalVariant>()
            var variantsTagFound = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "animal" -> {
                                currentAnimalId = parser.getAttributeValue(null, "id")
                                // Пример расширения атрибута
                                currentAnimalName = parser.getAttributeValue(null, "name")

                                if (currentAnimalId.isNullOrBlank()) {
                                    Log.e(TAG, "Ошибка: Отсутствует обязательный атрибут 'id' у тега <animal>")
                                }

                                currentVariants = mutableListOf()
                                variantsTagFound = false
                            }
                            "variants" -> {
                                variantsTagFound = true
                            }
                            "variant" -> {
                                val variantId = parser.getAttributeValue(null, "id")
                                val variantPath = parser.getAttributeValue(null, "path")

                                if (variantId.isNullOrBlank() || variantPath.isNullOrBlank()) {
                                    Log.e(
                                        TAG,
                                        "Ошибка: Отсутствует обязательный атрибут ('id' или 'path') у тега <variant> для животного id=${currentAnimalId ?: "unknown"}"
                                    )
                                }

                                if (!variantId.isNullOrBlank() && !variantPath.isNullOrBlank()) {
                                    currentVariants.add(AnimalVariant(variantId, variantPath))
                                    totalVariants++
                                }
                            }
                            // Добавляйте новые теги сюда для легкого расширения
                            // "new_tag" -> { ... }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "animal" -> {
                                if (!currentAnimalId.isNullOrBlank()) {
                                    if (!variantsTagFound) {
                                        Log.w(
                                            TAG,
                                            "Предупреждение: Отсутствует тег <variants> для животного с id: $currentAnimalId"
                                        )
                                    }
                                    animals.add(Animal(currentAnimalId, currentAnimalName, currentVariants))
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            Log.i(
                TAG,
                "Успешно загружено животных: ${animals.size}, всего загружено вариантов (ассетов): $totalVariants"
            )

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при парсинге XML: ${e.message}", e)
        } finally {
            try {
                inputStream.close()
            } catch (e: Exception) {
                // Игнорируем ошибку закрытия потока
            }
        }

        return animals
    }
}