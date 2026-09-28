package com.legacy.fingame

import com.legacy.fingame.game.rules.PetCareTuning
import com.legacy.fingame.game.rules.PetCareTuningReader
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Переопределение правил ухода из `care.xml`. */
class PetCareTuningReaderTest {

    private fun read(xml: String) = PetCareTuningReader.read(xml.byteInputStream())

    @Test
    fun `the file overrides what it names and keeps the rest`() {
        val tuning = read("""<care stopGrowthBelow="0.1" maxIncomePenaltyPercent="20" />""")

        assertEquals(PetCareTuning.DEFAULT.copy(stopGrowthBelow = 0.1, maxIncomePenaltyPercent = 20), tuning)
    }

    @Test
    fun `a typo keeps the default instead of breaking the game`() {
        val tuning = read(
            """<care stopGrowthBelow="много" slowGrowthBelow="1.5" incomePenaltyPercentPerDay="-5" />"""
        )

        assertEquals(PetCareTuning.DEFAULT, tuning)
    }

    @Test
    fun `an unreadable file means the default rules`() {
        assertEquals(PetCareTuning.DEFAULT, read("not xml at all"))
    }

    @Test
    fun `the shipped care file says the same as the defaults`() {
        // Файл в assets — справка для того, кто подкручивает правила: он не должен расходиться с
        // кодом, пока его не поменяли нарочно.
        val file = listOf(
            File("src/main/assets/data/care.xml"),
            File("app/src/main/assets/data/care.xml")
        ).first { it.exists() }

        assertEquals(PetCareTuning.DEFAULT, file.inputStream().use(PetCareTuningReader::read))
    }
}
