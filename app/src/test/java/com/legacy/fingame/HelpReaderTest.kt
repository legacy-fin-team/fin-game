package com.legacy.fingame

import com.legacy.fingame.game.help.HelpEntry
import com.legacy.fingame.game.help.HelpReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The help glossary as it is read out of the XML: a normal term, one dropped for missing data, and
 * that the data file the app ships with reads clean.
 */
class HelpReaderTest {

    private fun readEntries(xml: String): List<HelpEntry> =
        HelpReader().readEntries(xml.trimIndent().byteInputStream())

    /**
     * @return The glossary of the real data file the app ships in its assets, read the way the app
     * reads it at startup.
     */
    private fun readShippedEntries(): List<HelpEntry> {
        // The unit tests run from the module directory, but a run from the root of the build tree
        // has to find the same file.
        val file = listOf(
            File("src/main/assets/data/help.xml"),
            File("app/src/main/assets/data/help.xml")
        ).firstOrNull { it.exists() }
        assertTrue("data/help.xml is not in the assets", file != null)

        return file!!.inputStream().use { stream -> HelpReader().readEntries(stream) }
    }

    @Test
    fun `reads a term's id, title and text`() {
        val entries = readEntries(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <help>
                <term id="budget" title="Бюджет" text="План на период." />
            </help>
            """
        )

        assertEquals(listOf(HelpEntry(id = "budget", title = "Бюджет", text = "План на период.")), entries)
    }

    @Test
    fun `terms come out in the order the data declares them`() {
        val entries = readEntries(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <help>
                <term id="second" title="Второй" text="Текст второго." />
                <term id="first" title="Первый" text="Текст первого." />
            </help>
            """
        )

        assertEquals(listOf("second", "first"), entries.map { it.id })
    }

    @Test
    fun `a term missing its title or text is dropped, the rest of the glossary is read`() {
        val entries = readEntries(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <help>
                <term id="no_title" text="Текст без заголовка." />
                <term id="no_text" title="Заголовок без текста" />
                <term id="fine" title="Заголовок" text="Текст." />
            </help>
            """
        )

        assertEquals(listOf(HelpEntry(id = "fine", title = "Заголовок", text = "Текст.")), entries)
    }

    @Test
    fun `a term without an id is dropped`() {
        val entries = readEntries(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <help>
                <term title="Без id" text="Текст." />
                <term id="fine" title="Заголовок" text="Текст." />
            </help>
            """
        )

        assertEquals(listOf("fine"), entries.map { it.id })
    }

    @Test
    fun `the second of two terms sharing an id is dropped`() {
        val entries = readEntries(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <help>
                <term id="budget" title="Бюджет" text="Первый текст." />
                <term id="budget" title="Другой" text="Второй текст." />
            </help>
            """
        )

        assertEquals(listOf(HelpEntry(id = "budget", title = "Бюджет", text = "Первый текст.")), entries)
    }

    @Test
    fun `the data the app ships with reads clean, one term per real game term`() {
        val entries = readShippedEntries()

        assertTrue("help.xml has no terms", entries.isNotEmpty())
        entries.forEach { entry ->
            assertTrue("'${entry.id}' has a blank title", entry.title.isNotBlank())
            assertTrue("'${entry.id}' has a blank text", entry.text.isNotBlank())
        }

        // Ids are unique in the shipped data too, not just under the reader's own rule.
        assertEquals(entries.size, entries.map { it.id }.distinct().size)

        // The terms a player actually runs into elsewhere in the game are all explained.
        val ids = entries.map { it.id }
        listOf(
            "budget", "daily_bonus", "must_spend", "want_spend", "savings",
            "deposit", "goal", "log", "shop", "inventory", "quest"
        ).forEach { expected ->
            assertTrue("'$expected' is missing from help.xml", expected in ids)
        }
    }
}
