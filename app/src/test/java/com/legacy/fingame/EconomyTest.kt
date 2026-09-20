package com.legacy.fingame

import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.Screen
import com.legacy.fingame.game.economy.Economy
import com.legacy.fingame.game.items.Cart
import com.legacy.fingame.game.items.ItemSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The player's money: where it comes from, what it buys and what of it is still there on the next
 * launch.
 */
class EconomyTest {

    @Test
    fun `a player who never played starts with the starting balance`() {
        val vm = testGameViewModel()

        assertEquals(Economy.STARTING_BALANCE, vm.state.value.balance)
    }

    @Test
    fun `the cart costs what everything in it costs together`() {
        val vm = testGameViewModel()

        vm.increaseQty(TestItems.APPLE.id)
        vm.increaseQty(TestItems.APPLE.id)
        vm.increaseQty(TestItems.FISH.id)

        val expected = TestItems.APPLE.price * 2 + TestItems.FISH.price
        assertEquals(expected, vm.state.value.cartPrice)
        assertTrue(vm.state.value.canBuyCart)
    }

    @Test
    fun `buying the cart pays for it and hands the items over`() {
        val vm = testGameViewModel()

        vm.increaseQty(TestItems.APPLE.id)
        vm.increaseQty(TestItems.APPLE.id)
        vm.increaseQty(TestItems.HAT.id)
        val price = TestItems.APPLE.price * 2 + TestItems.HAT.price

        assertTrue(vm.buyCart())

        val state = vm.state.value
        assertEquals(Economy.STARTING_BALANCE - price, state.balance)
        assertEquals(2, state.owned[ItemSelection(TestItems.APPLE.id, "red")])
        assertEquals(1, state.owned[ItemSelection(TestItems.HAT.id, "black")])
        assertEquals(emptyMap<String, Int>(), state.quantities)
        assertEquals(0, state.cartPrice)
    }

    @Test
    fun `an item is bought in the variant that was picked for it`() {
        val vm = testGameViewModel()

        vm.pickVariant(TestItems.APPLE.id, "green")
        vm.increaseQty(TestItems.APPLE.id)

        assertTrue(vm.buyCart())

        assertEquals(1, vm.state.value.owned[ItemSelection(TestItems.APPLE.id, "green")])
    }

    @Test
    fun `a cart the player cannot afford buys nothing`() {
        val store = FakePlayerStateStore(PlayerState(balance = TestItems.HAT.price - 1))
        val vm = testGameViewModel(store)

        vm.increaseQty(TestItems.HAT.id)
        assertFalse(vm.state.value.canBuyCart)
        assertFalse(vm.buyCart())

        val state = vm.state.value
        assertEquals(TestItems.HAT.price - 1, state.balance)
        assertEquals(emptyMap<ItemSelection, Int>(), state.owned)
        assertEquals(1, state.quantities[TestItems.HAT.id])
    }

    @Test
    fun `an empty cart buys nothing`() {
        val vm = testGameViewModel()

        assertFalse(vm.state.value.canBuyCart)
        assertFalse(vm.buyCart())
        assertEquals(Economy.STARTING_BALANCE, vm.state.value.balance)
    }

    @Test
    fun `food is bought over and over, anything else only once`() {
        val vm = testGameViewModel()

        vm.increaseQty(TestItems.FISH.id)
        vm.increaseQty(TestItems.FISH.id)
        assertEquals(2, vm.state.value.quantities[TestItems.FISH.id])

        vm.increaseQty(TestItems.BALL.id)
        vm.increaseQty(TestItems.BALL.id)
        assertEquals(1, vm.state.value.quantities[TestItems.BALL.id])
    }

    @Test
    fun `an item already owned is not offered again, but its other variant is`() {
        val vm = testGameViewModel()

        vm.increaseQty(TestItems.HAT.id)
        assertTrue(vm.buyCart())

        vm.increaseQty(TestItems.HAT.id)
        assertEquals(0, vm.state.value.quantities[TestItems.HAT.id] ?: 0)

        vm.pickVariant(TestItems.HAT.id, "white")
        vm.increaseQty(TestItems.HAT.id)
        assertEquals(1, vm.state.value.quantities[TestItems.HAT.id])
        assertEquals(TestItems.HAT.price, vm.state.value.cartPrice)
    }

    @Test
    fun `switching to a variant already owned takes the item out of the cart`() {
        val store = FakePlayerStateStore(
            PlayerState(owned = mapOf(ItemSelection(TestItems.HAT.id, "black") to 1))
        )
        val vm = testGameViewModel(store)

        vm.pickVariant(TestItems.HAT.id, "white")
        vm.increaseQty(TestItems.HAT.id)
        assertEquals(1, vm.state.value.quantities[TestItems.HAT.id])

        vm.pickVariant(TestItems.HAT.id, "black")

        assertEquals(0, vm.state.value.quantities[TestItems.HAT.id])
        assertEquals(0, vm.state.value.cartPrice)
    }

    @Test
    fun `a variant the item does not have is not picked`() {
        val vm = testGameViewModel()

        vm.pickVariant(TestItems.HAT.id, "golden")

        assertEquals("black", vm.state.value.pickedVariantOf(TestItems.HAT))
    }

    @Test
    fun `earned coins are added and saved right away`() {
        val store = FakePlayerStateStore()
        val vm = testGameViewModel(store)

        vm.earn(120)

        assertEquals(Economy.STARTING_BALANCE + 120, vm.state.value.balance)
        assertEquals(Economy.STARTING_BALANCE + 120, store.state.balance)
    }

    @Test
    fun `earning nothing leaves the balance alone`() {
        val vm = testGameViewModel()

        vm.earn(0)
        vm.earn(-50)

        assertEquals(Economy.STARTING_BALANCE, vm.state.value.balance)
    }

    @Test
    fun `the daily bonus is paid once a day`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(clock = clock)

        assertTrue(vm.state.value.dailyBonusAvailable)
        assertTrue(vm.claimDailyBonus())
        assertEquals(Economy.STARTING_BALANCE + Economy.DAILY_BONUS, vm.state.value.balance)
        assertFalse(vm.state.value.dailyBonusAvailable)

        assertFalse(vm.claimDailyBonus())
        assertEquals(Economy.STARTING_BALANCE + Economy.DAILY_BONUS, vm.state.value.balance)
    }

    @Test
    fun `the next day the bonus is waiting again`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(clock = clock)
        assertTrue(vm.claimDailyBonus())

        clock.day += 1
        // The app may have been left open past midnight, so coming back to the main screen asks
        // again whether the bonus is there.
        vm.openScreen(Screen.QUESTS)
        vm.closeScreen()

        assertTrue(vm.state.value.dailyBonusAvailable)
        assertTrue(vm.claimDailyBonus())
        assertEquals(
            Economy.STARTING_BALANCE + Economy.DAILY_BONUS * 2,
            vm.state.value.balance
        )
    }

    @Test
    fun `moving the clock back does not pay the bonus again`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(clock = clock)
        assertTrue(vm.claimDailyBonus())

        clock.day -= 3
        vm.closeScreen()

        assertFalse(vm.state.value.dailyBonusAvailable)
        assertFalse(vm.claimDailyBonus())
        assertEquals(Economy.STARTING_BALANCE + Economy.DAILY_BONUS, vm.state.value.balance)
    }

    @Test
    fun `paying for the cart closes the shop`() {
        val vm = testGameViewModel()

        vm.openScreen(Screen.SHOP)
        vm.increaseQty(TestItems.APPLE.id)
        assertTrue(vm.buyCart())

        assertEquals(Screen.MAIN, vm.state.value.screen)
    }

    @Test
    fun `a cart that cannot be paid for leaves the player in the shop`() {
        val store = FakePlayerStateStore(PlayerState(balance = TestItems.HAT.price - 1))
        val vm = testGameViewModel(store)

        vm.openScreen(Screen.SHOP)
        vm.increaseQty(TestItems.HAT.id)
        assertFalse(vm.buyCart())

        assertEquals(Screen.SHOP, vm.state.value.screen)
        assertEquals(1, vm.state.value.quantities[TestItems.HAT.id])
    }

    @Test
    fun `the cart is listed item by item, each in the variant picked for it`() {
        val vm = testGameViewModel()

        vm.pickVariant(TestItems.APPLE.id, "green")
        vm.increaseQty(TestItems.APPLE.id)
        vm.increaseQty(TestItems.APPLE.id)
        vm.increaseQty(TestItems.HAT.id)

        val state = vm.state.value
        val lines = Cart.linesOf(state.quantities, state.pickedVariants, FakeItemCatalog())

        assertEquals(listOf(TestItems.APPLE, TestItems.HAT), lines.map { it.item })
        assertEquals(listOf("green", "black"), lines.map { it.variantId })
        assertEquals(listOf(2, 1), lines.map { it.quantity })
        assertEquals(state.cartPrice, lines.sumOf { it.price })
    }

    @Test
    fun `the cart listing leaves out what is not on sale any more`() {
        val lines = Cart.linesOf(
            quantities = mapOf(TestItems.APPLE.id to 1, "item_nobody_sells" to 3),
            pickedVariants = emptyMap(),
            catalog = FakeItemCatalog()
        )

        assertEquals(listOf(TestItems.APPLE.id), lines.map { it.item.id })
        assertEquals("red", lines.single().variantId)
    }

    @Test
    fun `the coins and the items bought are there on the next launch`() {
        val store = FakePlayerStateStore()
        val clock = FakeGameClock()

        val firstRun = testGameViewModel(store = store, clock = clock)
        firstRun.claimDailyBonus()
        firstRun.increaseQty(TestItems.APPLE.id)
        firstRun.increaseQty(TestItems.HAT.id)
        assertTrue(firstRun.buyCart())
        val balanceLeft = firstRun.state.value.balance

        val nextRun = testGameViewModel(store = store, clock = clock)

        val state = nextRun.state.value
        assertEquals(balanceLeft, state.balance)
        assertEquals(1, state.owned[ItemSelection(TestItems.APPLE.id, "red")])
        assertEquals(1, state.owned[ItemSelection(TestItems.HAT.id, "black")])
        assertFalse(state.dailyBonusAvailable)
    }
}
