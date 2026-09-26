package com.legacy.fingame

import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.game.items.CustomItemDraft
import com.legacy.fingame.game.items.CustomItemIcons
import com.legacy.fingame.game.items.ItemCategory
import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.game.stats.StatKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Свои предметы взрослого в модели: добавление, покупка ребёнком, удаление. */
class GameViewModelCustomItemsTest {

    private val clock = FakeGameClock()

    private val puzzle = CustomItemDraft(
        name = "Пазл",
        price = 40,
        category = ItemCategory.OTHER,
        iconPath = CustomItemIcons.ALL.first()
    )

    private val jumper = CustomItemDraft(
        name = "Прыгун",
        price = 30,
        category = ItemCategory.TOYS,
        effects = mapOf(StatKind.PLEASURE to 20),
        iconPath = CustomItemIcons.ALL.last()
    )

    private fun store(balance: Int = 500) = FakePlayerStateStore(
        PlayerState(
            selection = AnimalSelection(animalId = "cat", variantId = "white"),
            balance = balance,
            stats = PetStats(StatKind.entries.associateWith { 50 }),
            statsUpdatedAtMillis = clock.millis,
            petBornAtMillis = clock.millis
        )
    )

    @Test
    fun `only the adult can add an item`() {
        val vm = testGameViewModel(store = store(), clock = clock)

        assertFalse(vm.addCustomItem(puzzle))
        assertEquals(emptyList<Any>(), vm.state.value.customItems)
    }

    @Test
    fun `an added item is on the shelf and saved`() {
        val store = store()
        val vm = testGameViewModel(store = store, clock = clock)
        vm.enterAdultMode()

        assertTrue(vm.addCustomItem(puzzle))

        val item = vm.state.value.customItems.single()
        assertEquals("Пазл", item.name)
        assertEquals(listOf(item), vm.catalog.getItemsByCategory(ItemCategory.OTHER))
        assertEquals(listOf(item), store.state.customItems)
    }

    @Test
    fun `an invalid draft is not added`() {
        val vm = testGameViewModel(store = store(), clock = clock)
        vm.enterAdultMode()

        assertFalse(vm.addCustomItem(puzzle.copy(name = "")))
        assertEquals(emptyList<Any>(), vm.state.value.customItems)
    }

    @Test
    fun `two items added at the same moment get different ids`() {
        val vm = testGameViewModel(store = store(), clock = clock)
        vm.enterAdultMode()

        vm.addCustomItem(puzzle)
        vm.addCustomItem(jumper)

        val ids = vm.state.value.customItems.map { it.id }
        assertEquals(2, ids.toSet().size)
    }

    @Test
    fun `saved custom items come back on the next launch`() {
        val store = store()
        val first = testGameViewModel(store = store, clock = clock)
        first.enterAdultMode()
        first.addCustomItem(puzzle)

        val second = testGameViewModel(store = store, clock = clock)

        assertEquals(first.state.value.customItems, second.state.value.customItems)
        assertEquals(1, second.catalog.getItemsByCategory(ItemCategory.OTHER).size)
    }

    @Test
    fun `the child buys and uses a custom toy`() {
        val store = store()
        val vm = testGameViewModel(store = store, clock = clock)
        vm.enterAdultMode()
        vm.addCustomItem(jumper)
        vm.exitAdultMode()
        val toy = vm.state.value.customItems.single()

        vm.selectCategory(ItemCategory.TOYS)
        vm.increaseQty(toy.id)
        assertTrue(vm.buyCart())
        assertEquals(500 - 30, vm.state.value.balance)

        val selection = ItemSelection(toy.id, toy.defaultVariantId)
        assertEquals(1, vm.state.value.owned[selection])
        assertTrue(vm.useItem(selection))
        assertEquals(70, vm.state.value.stats[StatKind.PLEASURE])
    }

    @Test
    fun `removing an item takes it off the shelf but keeps the child's inventory safe`() {
        val vm = testGameViewModel(store = store(), clock = clock)
        vm.enterAdultMode()
        vm.addCustomItem(puzzle)
        vm.exitAdultMode()
        val item = vm.state.value.customItems.single()
        vm.selectCategory(ItemCategory.OTHER)
        vm.increaseQty(item.id)
        vm.buyCart()

        assertFalse(vm.removeCustomItem(item.id))
        vm.enterAdultMode()
        assertTrue(vm.removeCustomItem(item.id))
        assertFalse(vm.removeCustomItem(item.id))

        assertEquals(emptyList<Any>(), vm.state.value.customItems)
        assertEquals(emptyList<Any>(), vm.catalog.getItemsByCategory(ItemCategory.OTHER))
        // Раздел, в котором ничего не осталось, не остаётся выбранным.
        assertEquals(ItemCategory.entries.first(), vm.state.value.selectedCategory)
        assertEquals(
            emptyList<Any>(),
            com.legacy.fingame.game.items.Inventory.entriesOf(
                vm.state.value.owned,
                vm.state.value.worn,
                vm.catalog
            )
        )
    }
}
