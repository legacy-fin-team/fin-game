package com.legacy.fingame

import com.legacy.fingame.game.GameViewModel
import com.legacy.fingame.game.GameViewModel.Companion.MAX_ITEM_QUANTITY
import com.legacy.fingame.game.Screen
import org.junit.Assert.assertEquals
import org.junit.Test

class GameViewModelTest {

    @Test
    fun `openScreen and closeScreen switch screen`() {
        val vm = GameViewModel()

        vm.openScreen(Screen.SHOP)
        assertEquals(Screen.SHOP, vm.state.value.screen)

        vm.closeScreen()
        assertEquals(Screen.MAIN, vm.state.value.screen)
    }

    @Test
    fun `selectCategory changes selected category`() {
        val vm = GameViewModel()

        vm.selectCategory("toys")
        assertEquals("toys", vm.state.value.selectedCategoryId)

        vm.selectCategory("decor")
        assertEquals("decor", vm.state.value.selectedCategoryId)
    }

    @Test
    fun `increaseQty increases item counter`() {
        val vm = GameViewModel()

        vm.increaseQty("item_01")
        vm.increaseQty("item_01")

        assertEquals(2, vm.state.value.quantities["item_01"])
    }

    @Test
    fun `decreaseQty never goes below zero`() {
        val vm = GameViewModel()

        vm.decreaseQty("item_01")
        assertEquals(0, vm.state.value.quantities["item_01"] ?: 0)

        vm.increaseQty("item_01")
        vm.decreaseQty("item_01")
        vm.decreaseQty("item_01")
        assertEquals(0, vm.state.value.quantities["item_01"])
    }

    @Test
    fun `quantities of different items are independent`() {
        val vm = GameViewModel()

        vm.increaseQty("item_01")
        vm.increaseQty("item_01")
        vm.increaseQty("item_02")

        assertEquals(2, vm.state.value.quantities["item_01"])
        assertEquals(1, vm.state.value.quantities["item_02"])
    }

    @Test
    fun `increaseQty never goes above MAX_ITEM_QUANTITY`() {
        val vm = GameViewModel()

        repeat(MAX_ITEM_QUANTITY + 5) { vm.increaseQty("item_01") }

        assertEquals(MAX_ITEM_QUANTITY, vm.state.value.quantities["item_01"])
        assertEquals(99, MAX_ITEM_QUANTITY)
    }

    @Test
    fun `closeScreen resets quantities when leaving the shop`() {
        val vm = GameViewModel()

        vm.openScreen(Screen.SHOP)
        vm.increaseQty("item_01")
        vm.increaseQty("item_02")
        assertEquals(1, vm.state.value.quantities["item_01"])

        vm.closeScreen()

        assertEquals(Screen.MAIN, vm.state.value.screen)
        assertEquals(emptyMap<String, Int>(), vm.state.value.quantities)
    }

    @Test
    fun `openScreen to a non-shop screen resets quantities when leaving the shop`() {
        val vm = GameViewModel()

        vm.openScreen(Screen.SHOP)
        vm.increaseQty("item_01")
        assertEquals(1, vm.state.value.quantities["item_01"])

        vm.openScreen(Screen.INVENTORY)

        assertEquals(Screen.INVENTORY, vm.state.value.screen)
        assertEquals(emptyMap<String, Int>(), vm.state.value.quantities)
    }

    @Test
    fun `switching between non-shop screens does not reset quantities`() {
        val vm = GameViewModel()

        vm.openScreen(Screen.INVENTORY)
        vm.increaseQty("item_01")
        assertEquals(1, vm.state.value.quantities["item_01"])

        vm.openScreen(Screen.QUESTS)

        assertEquals(Screen.QUESTS, vm.state.value.screen)
        assertEquals(1, vm.state.value.quantities["item_01"])
    }

    @Test
    fun `nextSubLocation and prevSubLocation cycle in both directions`() {
        val vm = GameViewModel()
        val count = 4 // matches DemoContent.subLocationCount

        vm.prevSubLocation()
        assertEquals(count - 1, vm.state.value.subLocationIndex)

        repeat(count) { vm.nextSubLocation() }
        assertEquals(count - 1, vm.state.value.subLocationIndex)

        vm.nextSubLocation()
        assertEquals(0, vm.state.value.subLocationIndex)
    }
}
