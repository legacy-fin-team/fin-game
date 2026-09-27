package com.legacy.fingame

import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.Screen
import com.legacy.fingame.game.items.ItemCategory
import com.legacy.fingame.game.items.ItemSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Цели игрока: звёздочка в магазине отмечает товар в выбранном варианте, цель сохраняется сразу,
 * покупка её закрывает, а цель на то, чего в магазине больше нет, при запуске отбрасывается.
 */
class GameViewModelGoalsTest {

    private val blackHat = ItemSelection(TestItems.HAT.id, "black")
    private val whiteHat = ItemSelection(TestItems.HAT.id, "white")
    private val ball = ItemSelection(TestItems.BALL.id, "red")
    private val lamp = ItemSelection(TestItems.LAMP.id, "default")
    private val redApple = ItemSelection(TestItems.APPLE.id, "red")
    private val ghost = ItemSelection("ghost", "default")

    @Test
    fun `a star adds a goal and a second one takes it away`() {
        val vm = testGameViewModel()

        assertTrue(vm.toggleGoal(ball))
        assertEquals(listOf(ball), vm.state.value.goals)

        assertTrue(vm.toggleGoal(ball))
        assertEquals(emptyList<ItemSelection>(), vm.state.value.goals)
    }

    @Test
    fun `goals keep the order they were added in`() {
        val vm = testGameViewModel()

        vm.toggleGoal(lamp)
        vm.toggleGoal(ball)
        vm.toggleGoal(whiteHat)
        assertEquals(listOf(lamp, ball, whiteHat), vm.state.value.goals)

        // Снятая цель уходит, не сдвигая соседей местами, а отмеченная снова встаёт в конец.
        vm.toggleGoal(ball)
        assertEquals(listOf(lamp, whiteHat), vm.state.value.goals)
        vm.toggleGoal(ball)
        assertEquals(listOf(lamp, whiteHat, ball), vm.state.value.goals)
    }

    @Test
    fun `every variant is a goal of its own`() {
        val vm = testGameViewModel()

        vm.toggleGoal(blackHat)
        vm.toggleGoal(whiteHat)

        assertEquals(listOf(blackHat, whiteHat), vm.state.value.goals)
    }

    @Test
    fun `a goal is saved right away`() {
        val store = FakePlayerStateStore()
        val vm = testGameViewModel(store = store)

        vm.toggleGoal(lamp)
        vm.toggleGoal(ball)

        assertEquals(listOf(lamp, ball), store.state.goals)
    }

    @Test
    fun `nothing the shop does not sell becomes a goal`() {
        val vm = testGameViewModel()

        assertFalse(vm.toggleGoal(ghost))
        assertFalse(vm.toggleGoal(ItemSelection(TestItems.HAT.id, "purple")))
        assertEquals(emptyList<ItemSelection>(), vm.state.value.goals)
    }

    @Test
    fun `a thing the player owns is no goal, food still is`() {
        val vm = testGameViewModel(
            store = FakePlayerStateStore(PlayerState(owned = mapOf(blackHat to 1, redApple to 3)))
        )

        assertFalse(vm.toggleGoal(blackHat))
        assertTrue(vm.toggleGoal(whiteHat))
        assertTrue(vm.toggleGoal(redApple))
        assertEquals(listOf(whiteHat, redApple), vm.state.value.goals)
    }

    @Test
    fun `buying a goal reaches it, the other variant stays`() {
        val store = FakePlayerStateStore()
        val vm = testGameViewModel(store = store)
        vm.toggleGoal(ball)
        vm.toggleGoal(blackHat)
        vm.toggleGoal(whiteHat)

        // Шляпа идёт в корзину в варианте по умолчанию — чёрной.
        vm.increaseQty(TestItems.HAT.id)
        assertTrue(vm.buyCart())

        assertEquals(listOf(ball, whiteHat), vm.state.value.goals)
        assertEquals(listOf(ball, whiteHat), store.state.goals)
    }

    @Test
    fun `bought food is no longer a goal`() {
        val vm = testGameViewModel()
        vm.toggleGoal(redApple)

        vm.increaseQty(TestItems.APPLE.id)
        vm.increaseQty(TestItems.APPLE.id)
        assertTrue(vm.buyCart())

        assertEquals(emptyList<ItemSelection>(), vm.state.value.goals)
    }

    @Test
    fun `a purchase that fails keeps the goals`() {
        val vm = testGameViewModel()
        vm.toggleGoal(lamp)

        // 150 + 100 при 200 на счету: не хватает, и ничего не покупается.
        vm.increaseQty(TestItems.LAMP.id)
        vm.increaseQty(TestItems.HAT.id)
        assertFalse(vm.buyCart())

        assertEquals(listOf(lamp), vm.state.value.goals)
    }

    @Test
    fun `goals come back after a restart in the same order`() {
        val store = FakePlayerStateStore()
        val firstRun = testGameViewModel(store = store)
        firstRun.toggleGoal(lamp)
        firstRun.toggleGoal(ball)
        firstRun.toggleGoal(whiteHat)

        val nextRun = testGameViewModel(store = store)

        assertEquals(listOf(lamp, ball, whiteHat), nextRun.state.value.goals)
    }

    @Test
    fun `a saved goal on something the shop lost is dropped on launch`() {
        val store = FakePlayerStateStore(
            PlayerState(
                goals = listOf(ghost, ball, ItemSelection(TestItems.HAT.id, "purple"), lamp)
            )
        )

        val vm = testGameViewModel(store = store)

        assertEquals(listOf(ball, lamp), vm.state.value.goals)
    }

    @Test
    fun `a saved goal the player already owns is dropped on launch`() {
        val store = FakePlayerStateStore(
            PlayerState(owned = mapOf(blackHat to 1), goals = listOf(blackHat, whiteHat))
        )

        val vm = testGameViewModel(store = store)

        assertEquals(listOf(whiteHat), vm.state.value.goals)
    }

    @Test
    fun `the star follows the variant picked in the shop`() {
        val vm = testGameViewModel()
        vm.toggleGoal(whiteHat)

        assertFalse(vm.state.value.isGoal(TestItems.HAT))

        vm.pickVariant(TestItems.HAT.id, "white")
        assertTrue(vm.state.value.isGoal(TestItems.HAT))
    }

    @Test
    fun `a tap on a goal opens the shop on its shelf with its variant picked`() {
        val vm = testGameViewModel()
        vm.toggleGoal(whiteHat)

        vm.openGoal(whiteHat)

        val state = vm.state.value
        assertEquals(Screen.SHOP, state.screen)
        assertEquals(ItemCategory.CLOTHES, state.selectedCategory)
        assertEquals("white", state.pickedVariantOf(TestItems.HAT))
        assertTrue(state.isGoal(TestItems.HAT))
    }

    @Test
    fun `a goal the shop lost still opens the shop`() {
        val vm = testGameViewModel()

        vm.openGoal(ghost)

        assertEquals(Screen.SHOP, vm.state.value.screen)
        assertEquals(ItemCategory.entries.first(), vm.state.value.selectedCategory)
    }
}
