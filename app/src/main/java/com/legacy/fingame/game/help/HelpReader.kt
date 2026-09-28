package com.legacy.fingame.game.help

import android.util.Log
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Reads the help glossary out of its XML data file (`assets/data/help.xml`), the same way
 * [com.legacy.fingame.game.items.ItemReader] reads the shop's.
 */
class HelpReader {

    companion object {
        private const val TAG = "HelpReader"
    }

    /**
     * Reads the XML document. A `<term>` whose data is broken is skipped with a log line rather
     * than shown to the player half-written; the rest of the glossary still comes out whole.
     *
     * @param inputStream stream that reads the XML file.
     * @return The terms, in the order the data declares them — the order the help screen lists
     * them in, since a glossary a child reads start to finish should not shuffle itself between
     * launches.
     */
    fun readEntries(inputStream: InputStream): List<HelpEntry> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(inputStream)
        document.documentElement.normalize()

        val entries = mutableListOf<HelpEntry>()
        val seenIds = mutableSetOf<String>()

        val termNodes = document.getElementsByTagName("term")
        for (i in 0 until termNodes.length) {
            val termNode = termNodes.item(i)
            if (termNode !is Element) {
                continue
            }

            val id = termNode.getAttribute("id")
            if (id.isNullOrBlank()) {
                Log.e(TAG, "Tag <term> does not have 'id' attribute.")
                continue
            }

            if (!seenIds.add(id)) {
                Log.e(TAG, "At least two terms share the same id: '$id'")
                continue
            }

            val title = termNode.getAttribute("title")
            if (title.isNullOrBlank()) {
                Log.e(TAG, "Term with id '$id' does not have 'title' attribute.")
                continue
            }

            val text = termNode.getAttribute("text")
            if (text.isNullOrBlank()) {
                Log.e(TAG, "Term with id '$id' does not have 'text' attribute.")
                continue
            }

            entries.add(HelpEntry(id = id, title = title, text = text))
        }

        Log.i(TAG, "Loaded help terms: ${entries.size}.")
        return entries.toList()
    }
}
