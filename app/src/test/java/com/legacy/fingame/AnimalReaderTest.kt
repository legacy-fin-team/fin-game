package com.legacy.fingame

import com.legacy.fingame.game.animals.Animal
import com.legacy.fingame.game.animals.AnimalReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnimalReaderTest {

    private fun readAnimals(xml: String): Map<String, Animal> {
        return AnimalReader().readAnimals(xml.trimIndent().byteInputStream())
    }

    @Test
    fun `reads animal title, age count and variant paths`() {
        val animals = readAnimals(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <animals>
                <animal id="cat" title="Кот" ages="3">
                    <variants path="animals/cat/">
                        <variant id="orange" />
                        <variant id="white" />
                    </variants>
                </animal>
            </animals>
            """
        )

        val cat = animals.getValue("cat")
        assertEquals("Кот", cat.title)
        assertEquals(3, cat.ageCount)
        assertEquals(
            mapOf("orange" to "animals/cat/orange", "white" to "animals/cat/white"),
            cat.variants
        )
    }

    @Test
    fun `different animals may have different numbers of age stages`() {
        val animals = readAnimals(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <animals>
                <animal id="cat" title="Кот" ages="3">
                    <variants path="animals/cat/">
                        <variant id="orange" />
                    </variants>
                </animal>
                <animal id="dog" title="Пёс" ages="2">
                    <variants path="animals/dog/">
                        <variant id="brown" />
                    </variants>
                </animal>
            </animals>
            """
        )

        assertEquals(3, animals.getValue("cat").ageCount)
        assertEquals(2, animals.getValue("dog").ageCount)
    }

    @Test
    fun `animal without ages attribute gets the default number of stages`() {
        val animals = readAnimals(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <animals>
                <animal id="cat" title="Кот">
                    <variants path="animals/cat/">
                        <variant id="orange" />
                    </variants>
                </animal>
            </animals>
            """
        )

        assertEquals(Animal.DEFAULT_AGE_COUNT, animals.getValue("cat").ageCount)
    }

    @Test
    fun `animal with an improper ages attribute is skipped`() {
        val animals = readAnimals(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <animals>
                <animal id="cat" title="Кот" ages="0">
                    <variants path="animals/cat/">
                        <variant id="orange" />
                    </variants>
                </animal>
                <animal id="dog" title="Пёс" ages="many">
                    <variants path="animals/dog/">
                        <variant id="brown" />
                    </variants>
                </animal>
            </animals>
            """
        )

        assertEquals(emptyMap<String, Animal>(), animals)
    }

    @Test
    fun `animal without title is skipped`() {
        val animals = readAnimals(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <animals>
                <animal id="cat" ages="2">
                    <variants path="animals/cat/">
                        <variant id="orange" />
                    </variants>
                </animal>
            </animals>
            """
        )

        assertNull(animals["cat"])
    }

    @Test
    fun `idle sprite path contains the age stage`() {
        val cat = Animal(
            id = "cat",
            title = "Кот",
            ageCount = 3,
            variants = mapOf("orange" to "animals/cat/orange")
        )

        assertEquals("animals/cat/orange/0/idle.webp", cat.getIdleSpritePath("orange", 0))
        assertEquals("animals/cat/orange/2/idle.webp", cat.getIdleSpritePath("orange", 2))
    }

    @Test
    fun `idle sprite path coerces the age into the stages the animal has`() {
        val dog = Animal(
            id = "dog",
            title = "Пёс",
            ageCount = 2,
            variants = mapOf("brown" to "animals/dog/brown")
        )

        assertEquals("animals/dog/brown/1/idle.webp", dog.getIdleSpritePath("brown", 5))
        assertEquals("animals/dog/brown/0/idle.webp", dog.getIdleSpritePath("brown", -1))
    }
}
