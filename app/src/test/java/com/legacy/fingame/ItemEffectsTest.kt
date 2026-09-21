package com.legacy.fingame

import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.items.Item
import com.legacy.fingame.game.items.ItemCategory
import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.game.items.ShopShelf
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.game.stats.StatKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What an item does to the pet: which items are allowed to do anything at all, and how much room the
 * shop keeps to say so.
 */
class ItemEffectsTest {

    @Test
    fun `food and toys do to the pet what their data says`() {
        assertEquals(
            mapOf(StatKind.HUNGER to 20, StatKind.HEALTH to 5),
            TestItems.APPLE.effects
        )
        assertEquals(
            mapOf(StatKind.PLEASURE to 20, StatKind.HUNGER to -5),
            TestItems.BALL.effects
        )
    }

    @Test
    fun `an item the pet wears does nothing to it, whatever its data declares`() {
        // Both of these still declare an effect, the way an older data file does; the rule is the
        // item's own, so neither the clothes nor the decoration carry it.
        assertTrue(TestItems.HAT.isWearable)
        assertEquals(mapOf(StatKind.PLEASURE to 10), TestItems.HAT.declaredEffects)
        assertEquals(emptyMap<StatKind, Int>(), TestItems.HAT.effects)

        assertTrue(TestItems.LAMP.isWearable)
        assertEquals(emptyMap<StatKind, Int>(), TestItems.LAMP.effects)
    }

    @Test
    fun `a decoration that is never drawn anywhere is not worn and keeps its effects`() {
        // Wearing is what makes an item a look and nothing more; an item of a wearable category
        // that ends up on no layer at all is never worn, so nothing is dropped from it either.
        val item = Item(
            id = "poster",
            name = "Плакат",
            price = 10,
            category = ItemCategory.DECOR,
            variantIds = listOf("default"),
            declaredEffects = mapOf(StatKind.PLEASURE to 5),
            layer = null
        )

        assertEquals(mapOf(StatKind.PLEASURE to 5), item.effects)
    }

    @Test
    fun `a pet that was dressed up in an earlier run comes back as it was left`() {
        // Nothing of an outfit is kept apart from the outfit itself: the bars were saved as numbers
        // and are restored as numbers, so a save made when clothes still lifted the mood is read
        // without either taking that lift back or handing it out again.
        val hat = ItemSelection(TestItems.HAT.id, "black")
        val lamp = ItemSelection(TestItems.LAMP.id, "default")
        val stats = PetStats(StatKind.entries.associateWith { 60 })
        val store = FakePlayerStateStore(
            PlayerState(
                owned = mapOf(hat to 1, lamp to 1),
                worn = setOf(hat, lamp),
                stats = stats,
                statsUpdatedAtMillis = FakeGameClock.DEFAULT_MILLIS
            )
        )

        val vm = testGameViewModel(store)

        assertEquals(setOf(hat, lamp), vm.state.value.worn)
        assertEquals(stats, vm.state.value.stats)
    }

    @Test
    fun `a shelf keeps room for as many rows as its busiest item needs`() {
        val food = listOf(TestItems.APPLE, TestItems.FISH, TestItems.CAKE)

        // Two chips to a row: the apple and the fish fill one row, the cake takes two, and every
        // card of the shelf keeps two so they end at the same height.
        assertEquals(2, ShopShelf.effectRowsOf(food, chipsPerRow = 2))
        assertEquals(1, ShopShelf.effectRowsOf(listOf(TestItems.APPLE, TestItems.FISH), 2))
        assertEquals(3, ShopShelf.effectRowsOf(food, chipsPerRow = 1))
    }

    @Test
    fun `a shelf of items the pet feels nothing about keeps no room at all`() {
        assertEquals(0, ShopShelf.effectRowsOf(listOf(TestItems.HAT, TestItems.LAMP), 2))
        assertEquals(0, ShopShelf.effectRowsOf(emptyList(), 2))
    }

    @Test
    fun `a row that holds nothing is read as a row that holds one`() {
        // Nothing asks for this, but a size read off a layout is a number like any other and must
        // not divide by zero.
        assertEquals(3, ShopShelf.effectRowsOf(listOf(TestItems.CAKE), chipsPerRow = 0))
    }
}
