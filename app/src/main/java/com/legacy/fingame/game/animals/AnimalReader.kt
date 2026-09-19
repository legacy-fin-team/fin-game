package com.legacy.fingame.game.animals

import android.util.Log
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class AnimalReader {
    companion object {
        private const val TAG = "AnimalReader"
    }

    /**
     * Reads XML document. Doesn't do path data validation.
     * @param inputStream stream that reads XML file.
     * @return Map of animals. The key is animal id. The value is the [Animal] data:
     * its title, its number of age stages and the map of paths to its variants.
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
            if (animalNode !is Element) {
                continue
            }

            val animalId = animalNode.getAttribute("id")
            if (animalId.isNullOrBlank()) {
                Log.e(TAG, "Tag <animal> doesn't have id attribute.")
                continue
            }

            if (animals.containsKey(animalId)) {
                Log.e(TAG, "At least two animals share the same id: '$animalId'")
                continue
            }

            val animalTitle = animalNode.getAttribute("title")
            if (animalTitle.isNullOrBlank()) {
                Log.e(TAG, "Animal with id '$animalId' doesn't have 'title' attribute.")
                continue
            }

            val ageCount = getAgeCount(animalNode, animalId)
            if (ageCount == null) {
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

            val variantNodes = variantsElement.getElementsByTagName("variant")
            val variantMap = getVariants(variantNodes, animalId, variantsPath)

            if (variantMap.isEmpty()) {
                Log.e(TAG, "Animal with id '$animalId' doesn't have any tag <variant>.")
                continue
            }

            animals[animalId] = Animal(
                id = animalId,
                title = animalTitle,
                ageCount = ageCount,
                variants = variantMap.toMap()
            )
            totalVariants += variantMap.size
        }

        Log.i(TAG, "Loaded animals: ${animals.size}. Loaded animal variants: $totalVariants.")

        return animals.toMap()
    }

    /**
     * Reads the number of age stages of an animal from the 'ages' attribute of its tag.
     * An animal that doesn't declare the attribute gets [Animal.DEFAULT_AGE_COUNT] stages.
     *
     * @param animalElement tag <animal> of an animal.
     * @param animalId id of that animal.
     * @return Number of age stages or null if the attribute is present but isn't a positive number.
     */
    private fun getAgeCount(animalElement: Element, animalId: String): Int? {
        val agesAttribute = animalElement.getAttribute("ages")
        if (agesAttribute.isNullOrBlank()) {
            return Animal.DEFAULT_AGE_COUNT
        }

        val ageCount = agesAttribute.toIntOrNull()
        if (ageCount == null || ageCount < 1) {
            Log.e(TAG, "Animal with id '$animalId' doesn't have proper 'ages' attribute: " +
                    "'$agesAttribute'")
            return null
        }

        return ageCount
    }

    private fun getVariants(
        variantNodes: NodeList,
        animalId: String,
        variantsPath: String
    ): Map<String, String> {
        val variantMap = mutableMapOf<String, String>()

        for (i in 0 until variantNodes.length) {
            val variantNode = variantNodes.item(i)
            if (variantNode !is Element) {
                continue
            }

            val variantId = variantNode.getAttribute("id")
            if (variantId.isNullOrBlank()) {
                Log.e(TAG, "Tag <variant> of animal with id '$animalId' " +
                        "doesn't have 'id' attribute.")
                continue
            }

            if (variantMap.containsKey(variantId)) {
                Log.e(TAG, "At least two variants of animal with id '$animalId' " +
                        "share the same id: '$variantId'")
                continue
            }

            val fullPath = if (variantsPath.endsWith("/")) {
                "$variantsPath$variantId"
            } else {
                "$variantsPath/$variantId"
            }

            variantMap[variantId] = fullPath
        }

        return variantMap.toMap()
    }

}
