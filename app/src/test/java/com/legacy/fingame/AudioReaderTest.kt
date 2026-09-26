package com.legacy.fingame

import com.legacy.fingame.game.settings.AudioManager
import com.legacy.fingame.game.settings.AudioReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Фоновая музыка: как читается `data/audio.xml` и какой трек в итоге выбирается.
 */
class AudioReaderTest {

    private fun read(xml: String): List<String> =
        AudioReader().readBackgroundTracks(xml.trimIndent().byteInputStream())

    @Test
    fun `tracks are read in the declared order without blanks and repeats`() {
        val tracks = read(
            """
            <audio>
                <background>
                    <track file="music/background/b.ogg"/>
                    <track file=""/>
                    <track/>
                    <track file="music/background/a.ogg"/>
                    <track file="music/background/b.ogg"/>
                </background>
            </audio>
            """
        )
        assertEquals(listOf("music/background/b.ogg", "music/background/a.ogg"), tracks)
    }

    @Test
    fun `broken xml gives no tracks instead of a crash`() {
        assertTrue(read("<audio><background><track file=").isEmpty())
        assertTrue(read("").isEmpty())
    }

    @Test
    fun `xml without background gives no tracks`() {
        assertTrue(read("<audio/>").isEmpty())
    }

    @Test
    fun `the first declared track that exists wins`() {
        val chosen = AudioManager.chooseBackgroundTrack(
            declared = listOf("music/background/missing.ogg", "music/background/b.ogg"),
            folderFiles = listOf("a.ogg", "b.ogg")
        ) { it == "audio/music/background/b.ogg" }
        assertEquals("audio/music/background/b.ogg", chosen)
    }

    @Test
    fun `without declared tracks the first file of the folder by name is taken`() {
        val chosen = AudioManager.chooseBackgroundTrack(
            declared = emptyList(),
            folderFiles = listOf("z.ogg", "b.ogg", "c.ogg")
        ) { true }
        assertEquals("audio/music/background/b.ogg", chosen)
    }

    @Test
    fun `no tracks anywhere means no music`() {
        assertNull(AudioManager.chooseBackgroundTrack(emptyList(), emptyList()) { true })
    }

    @Test
    fun `the shipped data names a track that is in the assets`() {
        val dataFile = listOf(File("src/main/assets/data/audio.xml"), File("app/src/main/assets/data/audio.xml"))
            .first { it.isFile }
        val assets = dataFile.parentFile!!.parentFile!!
        val tracks = dataFile.inputStream().use { AudioReader().readBackgroundTracks(it) }
        assertTrue(tracks.isNotEmpty())
        assertTrue(File(assets, "audio/${tracks.first()}").isFile)
    }
}
