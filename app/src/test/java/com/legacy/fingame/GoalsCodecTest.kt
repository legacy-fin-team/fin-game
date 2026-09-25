package com.legacy.fingame

import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.utils.GoalsCodec
import org.junit.Assert.assertEquals
import org.junit.Test

/** Кодек целей: цели переживают превращение в строку и обратно, в том же порядке. */
class GoalsCodecTest {

    private val blackHat = ItemSelection(itemId = "hat", variantId = "black")
    private val whiteHat = ItemSelection(itemId = "hat", variantId = "white")
    private val ball = ItemSelection(itemId = "ball", variantId = "red")

    @Test
    fun `no goals encode to an empty string`() {
        assertEquals("", GoalsCodec.encode(emptyList()))
    }

    @Test
    fun `nothing saved decodes to no goals`() {
        assertEquals(emptyList<ItemSelection>(), GoalsCodec.decode(null))
        assertEquals(emptyList<ItemSelection>(), GoalsCodec.decode(""))
        assertEquals(emptyList<ItemSelection>(), GoalsCodec.decode("   "))
    }

    @Test
    fun `one goal is the two ids and a unit separator between them`() {
        assertEquals("hat\u001Fwhite", GoalsCodec.encode(listOf(whiteHat)))
    }

    @Test
    fun `goals keep their order through a round trip`() {
        val goals = listOf(whiteHat, ball, blackHat)

        assertEquals(goals, GoalsCodec.decode(GoalsCodec.encode(goals)))
    }

    @Test
    fun `a malformed record is dropped, the rest are kept`() {
        val raw = listOf(
            "hat\u001Fblack",
            "broken",
            "\u001Fred",
            "ball\u001F",
            "ball\u001Fred\u001Fextra",
            "hat\u001Fwhite"
        ).joinToString("\u001E")

        assertEquals(listOf(blackHat, whiteHat), GoalsCodec.decode(raw))
    }

    @Test
    fun `a goal saved twice comes back once, where it first stood`() {
        val raw = listOf("hat\u001Fblack", "ball\u001Fred", "hat\u001Fblack").joinToString("\u001E")

        assertEquals(listOf(blackHat, ball), GoalsCodec.decode(raw))
    }

    @Test
    fun `a goal listed twice is written once`() {
        val encoded = GoalsCodec.encode(listOf(blackHat, ball, blackHat))

        assertEquals(listOf(blackHat, ball), GoalsCodec.decode(encoded))
        assertEquals("hat\u001Fblack\u001Eball\u001Fred", encoded)
    }
}
