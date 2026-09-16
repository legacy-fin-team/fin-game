package com.legacy.fingame

import com.legacy.fingame.domain.ItemCategory
import com.legacy.fingame.domain.PurchaseKind
import com.legacy.fingame.domain.ShopCatalog
import com.legacy.fingame.game.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameViewModelTest {

    private val consumable = ShopCatalog.items.first { it.kind == PurchaseKind.CONSUMABLE }
    private val unique = ShopCatalog.items.first { it.kind == PurchaseKind.UNIQUE }

    @Test
    fun `increaseQty and decreaseQty change cart quantity without going below zero`() {
        val vm = GameViewModel()
        vm.selectCategory(consumable.category)

        vm.increaseQty(consumable.id)
        vm.increaseQty(consumable.id)
        var ui = vm.state.value.shopItems.first { it.item.id == consumable.id }
        assertEquals(2, ui.quantity)

        vm.decreaseQty(consumable.id)
        ui = vm.state.value.shopItems.first { it.item.id == consumable.id }
        assertEquals(1, ui.quantity)

        vm.decreaseQty(consumable.id)
        vm.decreaseQty(consumable.id) // не должно уйти в минус
        ui = vm.state.value.shopItems.first { it.item.id == consumable.id }
        assertEquals(0, ui.quantity)
    }

    @Test
    fun `toggleUnique adds and removes item from cart`() {
        val vm = GameViewModel()
        vm.selectCategory(unique.category)

        vm.toggleUnique(unique.id)
        var ui = vm.state.value.shopItems.first { it.item.id == unique.id }
        assertEquals(1, ui.quantity)
        assertFalse(ui.owned)

        vm.toggleUnique(unique.id)
        ui = vm.state.value.shopItems.first { it.item.id == unique.id }
        assertEquals(0, ui.quantity)
    }

    @Test
    fun `toggleGoal assigns and clears goal`() {
        val vm = GameViewModel()
        vm.selectCategory(unique.category)

        vm.toggleGoal(unique.id)
        assertEquals(unique.id, vm.state.value.goal?.itemId)
        assertTrue(vm.state.value.shopItems.first { it.item.id == unique.id }.isGoal)

        vm.toggleGoal(unique.id)
        assertNull(vm.state.value.goal)
    }

    @Test
    fun `checkout deducts balance clears cart and fills inventory and owned`() {
        val vm = GameViewModel()
        val startBalance = vm.state.value.balance

        vm.selectCategory(consumable.category)
        vm.increaseQty(consumable.id)
        vm.increaseQty(consumable.id)

        vm.selectCategory(unique.category)
        vm.toggleUnique(unique.id)

        val expectedTotal = consumable.price * 2 + unique.price
        assertEquals(expectedTotal, vm.state.value.cartTotal)
        assertTrue(vm.state.value.canCheckout)

        vm.checkout()

        val state = vm.state.value
        assertEquals(startBalance - expectedTotal, state.balance)
        assertEquals(0, state.cartTotal)
        assertEquals(2, state.inventory[consumable.id])
        assertTrue(state.ownedItemIds.contains(unique.id))
        assertEquals("Куплено на $expectedTotal ₽", state.toast)

        vm.consumeToast()
        assertNull(vm.state.value.toast)
    }

    @Test
    fun `canCheckout is false when not enough money`() {
        val vm = GameViewModel()
        val expensive = ShopCatalog.items.maxByOrNull { it.price }!!
        vm.selectCategory(expensive.category)

        // Тратим почти весь баланс на дорогой уникальный товар несколько раз невозможно (UNIQUE),
        // поэтому проверяем сценарий через расходуемый товар в большом количестве.
        val food = ShopCatalog.items.first { it.kind == PurchaseKind.CONSUMABLE }
        vm.selectCategory(food.category)
        val balance = vm.state.value.balance
        val timesNeeded = balance / food.price + 5
        repeat(timesNeeded) { vm.increaseQty(food.id) }

        assertTrue(vm.state.value.cartTotal > balance)
        assertFalse(vm.state.value.canCheckout)
    }

    @Test
    fun `nextSubLocation and prevSubLocation cycle through locations`() {
        val vm = GameViewModel()
        val count = vm.state.value.subLocations.size
        assertTrue(count > 0)

        vm.prevSubLocation()
        assertEquals(count - 1, vm.state.value.subLocationIndex)

        repeat(count) { vm.nextSubLocation() }
        assertEquals(count - 1, vm.state.value.subLocationIndex)
    }

    @Test
    fun `owned unique item cannot be added to cart again`() {
        val vm = GameViewModel()
        vm.selectCategory(unique.category)
        vm.toggleUnique(unique.id)
        vm.checkout()

        // товар уже куплен — повторное переключение не должно ничего изменить
        vm.toggleUnique(unique.id)
        val ui = vm.state.value.shopItems.first { it.item.id == unique.id }
        assertTrue(ui.owned)
        assertEquals(0, ui.quantity)
        assertEquals(0, vm.state.value.cartTotal)
    }
}
