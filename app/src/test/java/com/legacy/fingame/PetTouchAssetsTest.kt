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

    /** Reads a little-endian 16-bit number out of [bytes] at [offset]. */
    private fun int16(bytes: ByteArray, offset: Int): Int =
        ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()

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
    fun `the pat sound is a short 16-bit mono WAV under its key`() {
        val bytes = asset("audio/sounds/animal/${AudioManager.SOUND_PAT}.wav").readBytes()

        assertEquals("RIFF", String(bytes, 0, 4, Charsets.US_ASCII))
        assertEquals("WAVE", String(bytes, 8, 4, Charsets.US_ASCII))
        assertEquals(1, int16(bytes, 20))          // PCM
        assertEquals(1, int16(bytes, 22))          // mono
        assertEquals(22050, int32(bytes, 24))      // Hz
        assertEquals(16, int16(bytes, 34))         // bits per sample
        assertEquals("data", String(bytes, 36, 4, Charsets.US_ASCII))
        val millis = int32(bytes, 40) / 2 * 1000 / 22050
        assertTrue("$millis ms", millis in 150..200)
    }
}
