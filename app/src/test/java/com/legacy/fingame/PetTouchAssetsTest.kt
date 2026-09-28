package com.legacy.fingame

import com.legacy.fingame.game.settings.AudioManager
import com.legacy.fingame.ui.components.Sprites
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The files patting the pet needs are in the assets and are what the code expects of them: the
 * heart sprite is 8×8 pixels, and the sound is a short WAV that [AudioManager] picks up by its key.
 */
class PetTouchAssetsTest {

    /**
     * @param path path relative to `app/src/main/assets/`.
     * @return The asset file, whichever directory Gradle runs the tests from.
     */
    private fun asset(path: String): File =
        listOf(File("src/main/assets/$path"), File("app/src/main/assets/$path"))
            .firstOrNull { it.isFile }
            ?: throw AssertionError("no asset $path")

    /** Reads a little-endian 32-bit number out of [bytes] at [offset]. */
    private fun int32(bytes: ByteArray, offset: Int): Int =
        ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int

    @Test
    fun `the heart is a lossless 8 by 8 sprite`() {
        val bytes = asset("textures/${Sprites.HEART}").readBytes()

        assertEquals("RIFF", String(bytes, 0, 4, Charsets.US_ASCII))
        assertEquals("WEBP", String(bytes, 8, 4, Charsets.US_ASCII))
        if (String(bytes, 12, 4, Charsets.US_ASCII) != "VP8L") fail("heart must be lossless WEBP")
        // VP8L: signature byte 0x2f, then 14 bits of width - 1 and 14 bits of height - 1.
        assertEquals(0x2f, bytes[20].toInt() and 0xff)
        val bits = int32(bytes, 21)
        assertEquals(8, (bits and 0x3fff) + 1)
        assertEquals(8, ((bits shr 14) and 0x3fff) + 1)
    }

    @Test
    fun `the fallback patting sounds are OGG files under their keys`() {
        val folder = asset("audio/sounds/animal/cat1.ogg").parentFile!!
        val keys = folder.list()!!.map { it.substringBeforeLast('.') }.distinct().sorted()
        val patKeys = AudioManager.animalSoundKeys(keys, AudioManager.SOUND_PAT)
        assertTrue(patKeys.isNotEmpty())
        for (key in patKeys) {
            assertOgg("audio/sounds/animal/$key.ogg")
        }
    }

    @Test
    fun `the cat has its own OGG sounds`() {
        val folder = asset("audio/sounds/animal/cat1.ogg").parentFile!!
        val keys = folder.list()!!.map { it.substringBeforeLast('.') }.distinct().sorted()
        assertEquals(listOf("cat1", "cat2"), AudioManager.animalSoundKeys(keys, "cat"))
        for (key in listOf("cat1", "cat2")) {
            assertOgg("audio/sounds/animal/$key.ogg")
        }
    }

    @Test
    fun `an animal's sounds are its id followed by digits only`() {
        val all = listOf("cat1", "cat2", "cat10", "category", "cat", "catx1", "dog1", "pat")
        assertEquals(listOf("cat1", "cat2", "cat10"), AudioManager.animalSoundKeys(all, "cat"))
        assertEquals(listOf("dog1"), AudioManager.animalSoundKeys(all, "dog"))
        assertTrue(AudioManager.animalSoundKeys(all, "fox").isEmpty())
        assertTrue(AudioManager.animalSoundKeys(all, "").isEmpty())
    }

    /** The asset at [path] is an OGG audio file starting with 'OggS'. */
    private fun assertOgg(path: String) {
        val bytes = asset(path).readBytes()
        assertEquals("OggS", String(bytes, 0, 4, Charsets.US_ASCII))
    }
}
