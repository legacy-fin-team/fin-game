package com.legacy.fingame.game.hints

import android.util.Log
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Читает подсказки к экранам из `assets/data/hints.xml` — так же, как
 * [com.legacy.fingame.game.help.HelpReader] читает справку.
 */
class HintReader {

    companion object {
        private const val TAG = "HintReader"

        /** Любая цепочка пробелов и переносов внутри абзаца превращается в один пробел. */
        private val Whitespace = Regex("\\s+")
    }

    /**
     * Читает XML. Подсказка без ключа, без заголовка или без единого абзаца пропускается с записью
     * в лог, остальные читаются целиком.
     *
     * @param inputStream поток XML-файла.
     * @return Подсказки в том порядке, в каком они записаны в файле.
     */
    fun readHints(inputStream: InputStream): List<Hint> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(inputStream)
        document.documentElement.normalize()

        val hints = mutableListOf<Hint>()
        val seenKeys = mutableSetOf<String>()

        val hintNodes = document.getElementsByTagName("hint")
        for (i in 0 until hintNodes.length) {
            val hintNode = hintNodes.item(i)
            if (hintNode !is Element) {
                continue
            }

            val key = hintNode.getAttribute("key")
            if (key.isNullOrBlank()) {
                Log.e(TAG, "Tag <hint> does not have 'key' attribute.")
                continue
            }

            if (!seenKeys.add(key)) {
                Log.e(TAG, "At least two hints share the same key: '$key'")
                continue
            }

            val title = hintNode.getAttribute("title")
            if (title.isNullOrBlank()) {
                Log.e(TAG, "Hint '$key' does not have 'title' attribute.")
                continue
            }

            val paragraphNodes = hintNode.getElementsByTagName("p")
            val paragraphs = (0 until paragraphNodes.length)
                .map { index -> paragraphNodes.item(index).textContent.orEmpty() }
                .map { text -> text.replace(Whitespace, " ").trim() }
                .filter { text -> text.isNotEmpty() }
            if (paragraphs.isEmpty()) {
                Log.e(TAG, "Hint '$key' has no text.")
                continue
            }

            hints.add(
                Hint(
                    key = key,
                    icon = hintNode.getAttribute("icon").orEmpty().trim(),
                    title = title.trim(),
                    paragraphs = paragraphs
                )
            )
        }

        Log.i(TAG, "Loaded hints: ${hints.size}.")
        return hints.toList()
    }
}
