package com.legacy.fingame

import com.legacy.fingame.game.GameViewModel.Companion.MAX_ITEM_QUANTITY
import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.Screen
import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.game.items.ItemCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameViewModelTest {

    @Test
    fun `openScreen and closeScreen switch screen`() {
        val vm = testGameViewModel()

        vm.openScreen(Screen.SHOP)
        assertEquals(Screen.SHOP, vm.state.value.screen)

        vm.closeScreen()
        assertEquals(Screen.MAIN, vm.state.value.screen)
    }

    @Test
    fun `selectCategory changes selected category`() {
        val vm = testGameViewModel()

        vm.selectCategory(ItemCategory.TOYS)
        assertEquals(ItemCategory.TOYS, vm.state.value.selectedCategory)

        vm.selectCategory(ItemCategory.DECOR)
        assertEquals(ItemCategory.DECOR, vm.state.value.selectedCategory)
    }

    @Test
    fun `increaseQty increases item counter`() {
        val vm = testGameViewModel()

        vm.increaseQty(TestItems.APPLE.id)
        vm.increaseQty(TestItems.APPLE.id)

        assertEquals(2, vm.state.value.quantities[TestItems.APPLE.id])
    }

    @Test
    fun `decreaseQty never goes below zero`() {
        val vm = testGameViewModel()

        vm.decreaseQty(TestItems.APPLE.id)
        assertEquals(0, vm.state.value.quantities[TestItems.APPLE.id] ?: 0)

        vm.increaseQty(TestItems.APPLE.id)
        vm.decreaseQty(TestItems.APPLE.id)
        vm.decreaseQty(TestItems.APPLE.id)
        assertEquals(0, vm.state.value.quantities[TestItems.APPLE.id])
    }

    @Test
    fun `quantities of different items are independent`() {
        val vm = testGameViewModel()

        vm.increaseQty(TestItems.APPLE.id)
        vm.increaseQty(TestItems.APPLE.id)
        vm.increaseQty(TestItems.FISH.id)

        assertEquals(2, vm.state.value.quantities[TestItems.APPLE.id])
        assertEquals(1, vm.state.value.quantities[TestItems.FISH.id])
    }

    @Test
    fun `increaseQty never goes above MAX_ITEM_QUANTITY`() {
        val vm = testGameViewModel()

        repeat(MAX_ITEM_QUANTITY + 5) { vm.increaseQty(TestItems.APPLE.id) }

        assertEquals(MAX_ITEM_QUANTITY, vm.state.value.quantities[TestItems.APPLE.id])
        assertEquals(99, MAX_ITEM_QUANTITY)
    }

    @Test
    fun `an item that is not on sale cannot be put into the cart`() {
        val vm = testGameViewModel()

        vm.increaseQty("item_nobody_sells")

        assertEquals(emptyMap<String, Int>(), vm.state.value.quantities)
        assertEquals(0, vm.state.value.cartPrice)
    }

    @Test
    fun `closeScreen resets quantities when leaving the shop`() {
        val vm = testGameViewModel()

        vm.openScreen(Screen.SHOP)
        vm.increaseQty(TestItems.APPLE.id)
        vm.increaseQty(TestItems.FISH.id)
        assertEquals(1, vm.state.value.quantities[TestItems.APPLE.id])

        vm.closeScreen()

        assertEquals(Screen.MAIN, vm.state.value.screen)
        assertEquals(emptyMap<String, Int>(), vm.state.value.quantities)
        assertEquals(0, vm.state.value.cartPrice)
    }

    @Test
    fun `openScreen to a non-shop screen resets quantities when leaving the shop`() {
        val vm = testGameViewModel()

        vm.openScreen(Screen.SHOP)
        vm.increaseQty(TestItems.APPLE.id)
        assertEquals(1, vm.state.value.quantities[TestItems.APPLE.id])

        vm.openScreen(Screen.INVENTORY)

        assertEquals(Screen.INVENTORY, vm.state.value.screen)
        assertEquals(emptyMap<String, Int>(), vm.state.value.quantities)
    }

    @Test
    fun `switching between non-shop screens does not reset quantities`() {
        val vm = testGameViewModel()

        vm.openScreen(Screen.INVENTORY)
        vm.increaseQty(TestItems.APPLE.id)
        assertEquals(1, vm.state.value.quantities[TestItems.APPLE.id])

        vm.openScreen(Screen.QUESTS)

        assertEquals(Screen.QUESTS, vm.state.value.screen)
        assertEquals(1, vm.state.value.quantities[TestItems.APPLE.id])
    }

    @Test
    fun `nextSubLocation and prevSubLocation cycle in both directions`() {
        val vm = testGameViewModel()
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

        val vm = testGameViewModel(store)

        assertEquals(pet, vm.state.value.selection)
    }

    @Test
    fun `nothing was ever saved so the player starts without a pet`() {
        val vm = testGameViewModel()

        assertNull(vm.state.value.selection)
        assertEquals(0, vm.state.value.subLocationIndex)
    }

    @Test
    fun `the picked pet is saved right away`() {
        val store = FakePlayerStateStore()
        val vm = testGameViewModel(store)
        val pet = AnimalSelection(animalId = "dog", variantId = "brown")

        vm.selectAnimal(pet)

        assertEquals(pet, store.state.selection)
        assertEquals(pet, vm.state.value.selection)
    }

    @Test
    fun `the name given to the pet is saved with it`() {
        val store = FakePlayerStateStore()
        val vm = testGameViewModel(store)
        val pet = AnimalSelection(animalId = "dog", variantId = "brown")

        vm.selectAnimal(pet, "  Бобик ")

        assertEquals("Бобик", store.state.petName)
        assertEquals("Бобик", vm.state.value.petName)
    }

    @Test
    fun `a pet named with nothing but spaces stays nameless`() {
        val store = FakePlayerStateStore()
        val vm = testGameViewModel(store)

        vm.selectAnimal(AnimalSelection(animalId = "dog", variantId = "brown"), "   ")

        assertEquals("", store.state.petName)
    }

    @Test
    fun `the name the pet was given in an earlier run is there on start`() {
        val store = FakePlayerStateStore(
            PlayerState(
                selection = AnimalSelection(animalId = "cat", variantId = "orange"),
                petName = "Барсик"
            )
        )

        val vm = testGameViewModel(store)

        assertEquals("Барсик", vm.state.value.petName)
    }

    @Test
    fun `the sub-location the pet was left in is restored`() {
        val store = FakePlayerStateStore(PlayerState(subLocationIndex = 2))

        val vm = testGameViewModel(store)

        assertEquals(2, vm.state.value.subLocationIndex)
    }

    @Test
    fun `moving the pet to another sub-location is saved`() {
        val store = FakePlayerStateStore()
        val vm = testGameViewModel(store)

        vm.nextSubLocation()
        assertEquals(1, store.state.subLocationIndex)

        vm.prevSubLocation()
        assertEquals(0, store.state.subLocationIndex)
    }

    @Test
    fun `moving the pet around keeps the saved pet itself`() {
        val pet = AnimalSelection(animalId = "cat", variantId = "white")
        val store = FakePlayerStateStore(PlayerState(selection = pet))
        val vm = testGameViewModel(store)

        vm.nextSubLocation()

        assertEquals(pet, store.state.selection)
    }

    @Test
    fun `a saved sub-location that no longer exists falls back into range`() {
        // The sub-locations may change between two launches, so a saved index can point past the
        // last one that is left; the pet then starts in the last existing sub-location.
        val count = 4 // matches DemoContent.subLocationCount

        val tooFar = testGameViewModel(
            FakePlayerStateStore(PlayerState(subLocationIndex = count + 7))
        )
        assertEquals(count - 1, tooFar.state.value.subLocationIndex)

        val negative = testGameViewModel(FakePlayerStateStore(PlayerState(subLocationIndex = -3)))
        assertEquals(0, negative.state.value.subLocationIndex)
    }

    @Test
    fun `the open screen and the shop picks are not remembered between runs`() {
        val store = FakePlayerStateStore()

        val firstRun = testGameViewModel(store)
        firstRun.openScreen(Screen.SHOP)
        firstRun.selectCategory(ItemCategory.TOYS)
        firstRun.increaseQty(TestItems.APPLE.id)

        val nextRun = testGameViewModel(store)

        assertEquals(Screen.MAIN, nextRun.state.value.screen)
        assertEquals(emptyMap<String, Int>(), nextRun.state.value.quantities)
        assertEquals(ItemCategory.entries.first(), nextRun.state.value.selectedCategory)
    }
}
