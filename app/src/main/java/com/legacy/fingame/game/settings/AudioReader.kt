package com.legacy.fingame.game.settings

import android.util.Log
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Читает описание аудио из `assets/data/audio.xml` — по образцу [com.legacy.fingame.game.items.ItemReader].
 *
 * Формат:
 * ```
 * <audio>
 *     <background>
 *         <track file="music/background/music_for_kids.ogg"/>
 *     </background>
 * </audio>
 * ```
 * Пути треков — относительно `assets/audio/`.
 */
class AudioReader {

    companion object {
        private const val TAG = "AudioReader"
    }

    /**
     * Читает список треков фоновой музыки в порядке объявления.
     *
     * Битый XML или отсутствие `<background>` не роняют игру: возвращается пустой список,
     * и тогда музыка берётся из папки `audio/music/background/`.
     *
     * @param inputStream поток с XML.
     * @return Пути треков относительно `assets/audio/`, без пустых и повторов.
     */
    fun readBackgroundTracks(inputStream: InputStream): List<String> {
        val document = try {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось прочитать audio.xml", e)
            return emptyList()
        }
        document.documentElement.normalize()

        val tracks = mutableListOf<String>()
        val backgrounds = document.getElementsByTagName("background")
        for (b in 0 until backgrounds.length) {
            val background = backgrounds.item(b) as? Element ?: continue
            val trackNodes = background.getElementsByTagName("track")
            for (i in 0 until trackNodes.length) {
                val track = trackNodes.item(i) as? Element ?: continue
                val file = track.getAttribute("file").trim()
                if (file.isEmpty()) {
                    Log.e(TAG, "У <track> нет атрибута 'file'.")
                    continue
                }
                if (file !in tracks) tracks.add(file)
            }
        }
        Log.i(TAG, "Треков фоновой музыки в данных: ${tracks.size}")
        return tracks.toList()
    }
}
