package com.legacy.fingame

import androidx.compose.runtime.saveable.SaverScope
import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.ui.screens.AnimalSelectionSaver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnimalSelectionSaverTest {

    private val saverScope = SaverScope { true }

    private fun save(selection: AnimalSelection?): Any? {
        return with(AnimalSelectionSaver) { saverScope.save(selection) }
    }

    @Test
    fun `a picked card comes back after the screen is recreated`() {
        val picked = AnimalSelection(animalId = "cat", variantId = "orange")

        val saved = save(picked)

        assertEquals(picked, AnimalSelectionSaver.restore(saved!!))
    }

    @Test
    fun `saved ids are plain values a Bundle can hold`() {
        val picked = AnimalSelection(animalId = "cat", variantId = "orange")

        assertEquals(listOf("cat", "orange"), save(picked))
    }

    @Test
    fun `nothing picked leaves nothing to restore`() {
        // An empty list is what a saver saves as "no value at all": the screen comes back with
        // its button disabled, exactly as it was before the player tapped anything.
        assertNull(save(null))
    }
}
