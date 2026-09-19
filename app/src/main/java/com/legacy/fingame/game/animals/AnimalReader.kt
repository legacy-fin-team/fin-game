package com.legacy.fingame.game.animals

import android.util.Log
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class AnimalReader {
    companion object {
        private const val TAG = "AnimalReader"
    }

    /**
     * Reads XML document. Doesn't do path data validation.
     * @param inputStream stream that reads XML file.
     * @return Map of animals. The key is animal id.
     * The value is map of paths to animal variants.
     * The key is the animal variant id. The value is the path relative to the /asstets/textures/.
     */
    fun readAnimals(inputStream: InputStream): Map<String, Animal> {
        val animals = mutableMapOf<String, Animal>()
        var totalVariants = 0

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(inputStream)
        document.documentElement.normalize()

        val animalNodes = document.getElementsByTagName("animal")
        for (i in 0 until animalNodes.length) {
            val animalNode = animalNodes.item(i)
            if (animalNode is Element) {
                val animalId = animalNode.getAttribute("id")
                if (animalId.isNullOrBlank()) {
                    Log.e(TAG, "Tag <animal> doesn't have id attribute.")
                    continue
                }

                val variantsNodes = animalNode.getElementsByTagName("variants")
                if (variantsNodes.length == 0) {
                    Log.e(TAG, "Animal with id '$animalId' doesn't have tag <variants>.")
                    continue
                }

                val variantsElement = variantsNodes.item(0) as Element
                val variantsPath = variantsElement.getAttribute("path")
                
                if (variantsPath.isNullOrBlank()) {
                    Log.e(TAG, "Tag <variants> of animal with id '$animalId' " +
                            "doesn't have 'path' attribute.")
                    continue
                }

                val variantMap = mutableMapOf<String, String>()
                val variantNodes = variantsElement.getElementsByTagName("variant")

                for (j in 0 until variantNodes.length) {
                    val variantNode = variantNodes.item(j)
                    if (variantNode is Element) {
                        val variantId = variantNode.getAttribute("id")

                        if (variantId.isNullOrBlank()) {
                            Log.e(TAG, "Tag <variant> of animal with id '$animalId' " +
                                    "doesn't have 'id' attribute.")
                            continue
                        }
                        
                        val fullPath = if (variantsPath.endsWith("/")) {
                            "$variantsPath$variantId"
                        } else {
                            "$variantsPath/$variantId"
                        }
                        
                        variantMap[variantId] = fullPath
                    }
                }

                if (variantMap.isEmpty()) {
                    Log.e(TAG, "Animal with id '$animalId' doesn't have any tag <variant>.")
                    continue
                }

                animals[animalId] = Animal(id = animalId, variants = variantMap.toMap())
                totalVariants += variantMap.size
            }
        }

        Log.i(TAG, "Loaded animals: ${animals.size}. Loaded animal variants: $totalVariants.")
        
        return animals.toMap()
    }
}
