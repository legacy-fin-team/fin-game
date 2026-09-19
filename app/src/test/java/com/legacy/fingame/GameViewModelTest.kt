package com.legacy.fingame

import com.legacy.fingame.game.GameViewModel
import com.legacy.fingame.game.GameViewModel.Companion.MAX_ITEM_QUANTITY
import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.PlayerStateStore
import com.legacy.fingame.game.Screen
import com.legacy.fingame.game.animals.AnimalSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [PlayerStateStore] that keeps the state in memory instead of in SharedPreferences, so the
 * restoring and the saving can be checked without an Android device. A store built with a state
 * stands for an app that was already played and closed.
 *
 * @property state the state the store currently holds, i.e. what the next launch would restore.
 */
private class FakePlayerStateStore(var state: PlayerState = PlayerState()) : PlayerStateStore {

    override fun load(): PlayerState = state

    override fun save(state: PlayerState) {
        this.state = state
    }
}

class GameViewModelTest {

    @Test
    fun `openScreen and closeScreen switch screen`() {
        val vm = GameViewModel(FakePlayerStateStore())

        vm.openScreen(Screen.SHOP)
        assertEquals(Screen.SHOP, vm.state.value.screen)

        vm.closeScreen()
        assertEquals(Screen.MAIN, vm.state.value.screen)
    }

    @Test
    fun `selectCategory changes selected category`() {
        val vm = GameViewModel(FakePlayerStateStore())

        vm.selectCategory("toys")
        assertEquals("toys", vm.state.value.selectedCategoryId)

        vm.selectCategory("decor")
        assertEquals("decor", vm.state.value.selectedCategoryId)
    }

    @Test
    fun `increaseQty increases item counter`() {
        val vm = GameViewModel(FakePlayerStateStore())

        vm.increaseQty("item_01")
        vm.increaseQty("item_01")

        assertEquals(2, vm.state.value.quantities["item_01"])
    }

    @Test
    fun `decreaseQty never goes below zero`() {
        val vm = GameViewModel(FakePlayerStateStore())

        vm.decreaseQty("item_01")
        assertEquals(0, vm.state.value.quantities["item_01"] ?: 0)

        vm.increaseQty("item_01")
        vm.decreaseQty("item_01")
        vm.decreaseQty("item_01")
        assertEquals(0, vm.state.value.quantities["item_01"])
    }

    @Test
    fun `quantities of different items are independent`() {
        val vm = GameViewModel(FakePlayerStateStore())

        vm.increaseQty("item_01")
        vm.increaseQty("item_01")
        vm.increaseQty("item_02")

        assertEquals(2, vm.state.value.quantities["item_01"])
        assertEquals(1, vm.state.value.quantities["item_02"])
    }

    @Test
    fun `increaseQty never goes above MAX_ITEM_QUANTITY`() {
        val vm = GameViewModel(FakePlayerStateStore())

        repeat(MAX_ITEM_QUANTITY + 5) { vm.increaseQty("item_01") }

        assertEquals(MAX_ITEM_QUANTITY, vm.state.value.quantities["item_01"])
        assertEquals(99, MAX_ITEM_QUANTITY)
    }

    @Test
    fun `closeScreen resets quantities when leaving the shop`() {
        val vm = GameViewModel(FakePlayerStateStore())

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
        val vm = GameViewModel(FakePlayerStateStore())

        vm.openScreen(Screen.SHOP)
        vm.increaseQty("item_01")
        assertEquals(1, vm.state.value.quantities["item_01"])

        vm.openScreen(Screen.INVENTORY)

        assertEquals(Screen.INVENTORY, vm.state.value.screen)
        assertEquals(emptyMap<String, Int>(), vm.state.value.quantities)
    }

    @Test
    fun `switching between non-shop screens does not reset quantities`() {
        val vm = GameViewModel(FakePlayerStateStore())

        vm.openScreen(Screen.INVENTORY)
        vm.increaseQty("item_01")
        assertEquals(1, vm.state.value.quantities["item_01"])

        vm.openScreen(Screen.QUESTS)

        assertEquals(Screen.QUESTS, vm.state.value.screen)
        assertEquals(1, vm.state.value.quantities["item_01"])
    }

    @Test
    fun `nextSubLocation and prevSubLocation cycle in both directions`() {
        val vm = GameViewModel(FakePlayerStateStore())
        val count = 4 // matches DemoContent.subLocationCount

        vm.prevSubLocation()
        assertEquals(count - 1, vm.state.value.subLocationIndex)

        repeat(count) { vm.nextSubLocation() }
        assertEquals(count - 1, vm.state.value.subLocationIndex)

        vm.nextSubLocation()
        assertEquals(0, vm.state.value.subLocationIndex)
    }

    @Test
    fun `the pet picked in an earlier run is there on start`() {
        val pet = AnimalSelection(animalId = "cat", variantId = "orange")
        val store = FakePlayerStateStore(PlayerState(selection = pet))

        val vm = GameViewModel(store)

        assertEquals(pet, vm.state.value.selection)
    }

    @Test
    fun `nothing was ever saved so the player starts without a pet`() {
        val vm = GameViewModel(FakePlayerStateStore())

        assertNull(vm.state.value.selection)
        assertEquals(0, vm.state.value.subLocationIndex)
    }

    @Test
    fun `the picked pet is saved right away`() {
        val store = FakePlayerStateStore()
        val vm = GameViewModel(store)
        val pet = AnimalSelection(animalId = "dog", variantId = "brown")

        vm.selectAnimal(pet)

        assertEquals(pet, store.state.selection)
        assertEquals(pet, vm.state.value.selection)
    }

    @Test
    fun `the sub-location the pet was left in is restored`() {
        val store = FakePlayerStateStore(PlayerState(subLocationIndex = 2))

        val vm = GameViewModel(store)

        assertEquals(2, vm.state.value.subLocationIndex)
    }

    @Test
    fun `moving the pet to another sub-location is saved`() {
        val store = FakePlayerStateStore()
        val vm = GameViewModel(store)

        vm.nextSubLocation()
        assertEquals(1, store.state.subLocationIndex)

        vm.prevSubLocation()
        assertEquals(0, store.state.subLocationIndex)
    }

    @Test
    fun `moving the pet around keeps the saved pet itself`() {
        val pet = AnimalSelection(animalId = "cat", variantId = "white")
        val store = FakePlayerStateStore(PlayerState(selection = pet))
        val vm = GameViewModel(store)

        vm.nextSubLocation()

        assertEquals(pet, store.state.selection)
    }

    @Test
    fun `a saved sub-location that no longer exists falls back into range`() {
        // The sub-locations may change between two launches, so a saved index can point past the
        // last one that is left; the pet then starts in the last existing sub-location.
        val count = 4 // matches DemoContent.subLocationCount

        val tooFar = GameViewModel(FakePlayerStateStore(PlayerState(subLocationIndex = count + 7)))
        assertEquals(count - 1, tooFar.state.value.subLocationIndex)

        val negative = GameViewModel(FakePlayerStateStore(PlayerState(subLocationIndex = -3)))
        assertEquals(0, negative.state.value.subLocationIndex)
    }

    @Test
    fun `the open screen and the shop picks are not remembered between runs`() {
        val store = FakePlayerStateStore()

        val firstRun = GameViewModel(store)
        firstRun.openScreen(Screen.SHOP)
        firstRun.selectCategory("toys")
        firstRun.increaseQty("item_01")

        val nextRun = GameViewModel(store)

        assertEquals(Screen.MAIN, nextRun.state.value.screen)
        assertEquals(emptyMap<String, Int>(), nextRun.state.value.quantities)
        assertEquals("food", nextRun.state.value.selectedCategoryId)
    }
}
