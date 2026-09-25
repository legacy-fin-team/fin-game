package com.legacy.fingame

import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.animals.Animal
import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.game.animals.Growth
import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.game.stats.StatKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Looking after the pet: using what the player owns on it, dressing it up, and what time alone does
 * to it.
 */
class PetCareTest {

    /** Stats of a pet that has been neglected enough for every item to have room to work. */
    private val halfEmptyStats = PetStats(StatKind.entries.associateWith { 50 })

    /**
     * Builds a store holding one owned item and a pet whose bars are half empty.
     *
     * @param selection the item the player owns.
     * @param count how many of it.
     * @param worn whether the pet has it on.
     * @return A store standing for an app that was already played and closed.
     */
    private fun storeWith(
        selection: ItemSelection,
        count: Int = 1,
        worn: Boolean = false
    ) = FakePlayerStateStore(
        PlayerState(
            selection = AnimalSelection(animalId = "cat", variantId = "white"),
            owned = mapOf(selection to count),
            worn = if (worn) setOf(selection) else emptySet(),
            stats = halfEmptyStats,
            statsUpdatedAtMillis = FakeGameClock.DEFAULT_MILLIS,
            petBornAtMillis = FakeGameClock.DEFAULT_MILLIS
        )
    )

    @Test
    fun `food is gone once the pet has eaten it`() {
        val apple = ItemSelection(TestItems.APPLE.id, "red")
        val store = storeWith(apple, count = 2)
        val vm = testGameViewModel(store)

        assertTrue(vm.useItem(apple))
        assertEquals(1, vm.state.value.owned[apple])

        assertTrue(vm.useItem(apple))
        assertFalse(vm.state.value.owned.containsKey(apple))
        assertFalse(store.state.owned.containsKey(apple))
    }

    @Test
    fun `food the player does not have cannot be eaten`() {
        val apple = ItemSelection(TestItems.APPLE.id, "red")
        val vm = testGameViewModel()

        assertFalse(vm.useItem(apple))
        assertEquals(PetStats.FULL, vm.state.value.stats)
    }

    @Test
    fun `a toy stays in the inventory however often the pet plays with it`() {
        val ball = ItemSelection(TestItems.BALL.id, "red")
        val vm = testGameViewModel(storeWith(ball))

        repeat(3) { assertTrue(vm.useItem(ball)) }

        assertEquals(1, vm.state.value.owned[ball])
    }

    @Test
    fun `using an item moves the pet's bars by what the item does`() {
        val apple = ItemSelection(TestItems.APPLE.id, "red")
        val vm = testGameViewModel(storeWith(apple))

        assertTrue(vm.useItem(apple))

        val stats = vm.state.value.stats
        assertEquals(50 + 20, stats[StatKind.HUNGER])
        assertEquals(50 + 5, stats[StatKind.HEALTH])
        assertEquals(50, stats[StatKind.PLEASURE])
    }

    @Test
    fun `playing with a toy can cost the pet another bar`() {
        val ball = ItemSelection(TestItems.BALL.id, "red")
        val vm = testGameViewModel(storeWith(ball))

        assertTrue(vm.useItem(ball))

        val stats = vm.state.value.stats
        assertEquals(50 + 20, stats[StatKind.PLEASURE])
        assertEquals(50 - 5, stats[StatKind.HUNGER])
    }

    @Test
    fun `clothes are put on and taken off instead of being used up`() {
        val hat = ItemSelection(TestItems.HAT.id, "black")
        val store = storeWith(hat)
        val vm = testGameViewModel(store)

        assertFalse(vm.useItem(hat))

        assertTrue(vm.toggleWorn(hat))
        assertEquals(setOf(hat), vm.state.value.worn)
        assertEquals(1, vm.state.value.owned[hat])

        assertTrue(vm.toggleWorn(hat))
        assertEquals(emptySet<ItemSelection>(), vm.state.value.worn)
        assertEquals(1, vm.state.value.owned[hat])
    }

    @Test
    fun `putting something on the pet is remembered between runs`() {
        val hat = ItemSelection(TestItems.HAT.id, "black")
        val store = storeWith(hat)

        val firstRun = testGameViewModel(store)
        assertTrue(firstRun.toggleWorn(hat))
        assertEquals(setOf(hat), store.state.worn)

        val nextRun = testGameViewModel(store)
        assertEquals(setOf(hat), nextRun.state.value.worn)
    }

    @Test
    fun `what the pet wears is a look and nothing more`() {
        val hat = ItemSelection(TestItems.HAT.id, "black")
        val vm = testGameViewModel(storeWith(hat))

        assertTrue(vm.toggleWorn(hat))
        assertEquals(halfEmptyStats, vm.state.value.stats)

        assertTrue(vm.toggleWorn(hat))
        assertEquals(halfEmptyStats, vm.state.value.stats)
    }

    @Test
    fun `only one variant of the same item can be on the pet at a time`() {
        val black = ItemSelection(TestItems.HAT.id, "black")
        val white = ItemSelection(TestItems.HAT.id, "white")
        val store = FakePlayerStateStore(PlayerState(owned = mapOf(black to 1, white to 1)))
        val vm = testGameViewModel(store)

        assertTrue(vm.toggleWorn(black))
        assertTrue(vm.toggleWorn(white))

        assertEquals(setOf(white), vm.state.value.worn)
    }

    @Test
    fun `things the pet cannot be wearing are taken off on start`() {
        val hat = ItemSelection(TestItems.HAT.id, "black")
        val soldOut = ItemSelection(TestItems.HAT.id, "white")
        val notWearable = ItemSelection(TestItems.APPLE.id, "red")
        val store = FakePlayerStateStore(
            PlayerState(
                owned = mapOf(hat to 1, notWearable to 1),
                worn = setOf(hat, soldOut, notWearable)
            )
        )

        val vm = testGameViewModel(store)

        assertEquals(setOf(hat), vm.state.value.worn)
    }

    @Test
    fun `an item nobody registers any more can neither be used nor worn`() {
        val gone = ItemSelection("item_nobody_sells", "default")
        val store = FakePlayerStateStore(PlayerState(owned = mapOf(gone to 1)))
        val vm = testGameViewModel(store)

        assertFalse(vm.useItem(gone))
        assertFalse(vm.toggleWorn(gone))
    }

    @Test
    fun `the bars fall while the player is away`() {
        val clock = FakeGameClock()
        val store = FakePlayerStateStore(
            PlayerState(
                stats = PetStats.FULL,
                statsUpdatedAtMillis = clock.millis
            )
        )
        val firstRun = testGameViewModel(store = store, clock = clock)
        assertEquals(PetStats.FULL, firstRun.state.value.stats)

        clock.millis += PetStats.TICK_MILLIS * 4
        val nextRun = testGameViewModel(store = store, clock = clock)

        StatKind.entries.forEach { stat ->
            assertEquals(
                PetStats.MAX_VALUE - stat.decayPerTick * 4,
                nextRun.state.value.stats[stat]
            )
        }
    }

    @Test
    fun `the bars fall while the player watches`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(clock = clock)

        clock.millis += PetStats.TICK_MILLIS
        vm.tick()

        assertEquals(
            PetStats.MAX_VALUE - StatKind.HUNGER.decayPerTick,
            vm.state.value.stats[StatKind.HUNGER]
        )
    }

    @Test
    fun `time too short to count is kept for the next tick instead of being lost`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(clock = clock)
        val step = PetStats.TICK_MILLIS / 2

        clock.millis += step
        vm.tick()
        assertEquals(PetStats.MAX_VALUE, vm.state.value.stats[StatKind.HUNGER])

        clock.millis += step
        vm.tick()
        assertEquals(
            PetStats.MAX_VALUE - StatKind.HUNGER.decayPerTick,
            vm.state.value.stats[StatKind.HUNGER]
        )
    }

    @Test
    fun `a freshly picked pet is a newborn one with full bars`() {
        val clock = FakeGameClock()
        val store = FakePlayerStateStore(
            PlayerState(stats = halfEmptyStats, statsUpdatedAtMillis = clock.millis)
        )
        val vm = testGameViewModel(store = store, clock = clock)

        vm.selectAnimal(AnimalSelection(animalId = "dog", variantId = "brown"))

        assertEquals(PetStats.FULL, vm.state.value.stats)
        assertEquals(Animal.FIRST_AGE, vm.state.value.petAge)
        assertEquals(clock.millis, store.state.petBornAtMillis)
    }

    @Test
    fun `the pet grows up as the days pass`() {
        val clock = FakeGameClock()
        val store = FakePlayerStateStore()
        val vm = testGameViewModel(store = store, clock = clock)
        vm.selectAnimal(AnimalSelection(animalId = "cat", variantId = "white"))

        clock.millis += Growth.STAGE_MILLIS * 2
        vm.tick()

        assertEquals(Animal.FIRST_AGE + 2, vm.state.value.petAge)
    }

    @Test
    fun `a pet grown up between two launches comes back grown up`() {
        val clock = FakeGameClock()
        val store = FakePlayerStateStore(
            PlayerState(
                selection = AnimalSelection(animalId = "cat", variantId = "white"),
                petBornAtMillis = clock.millis,
                statsUpdatedAtMillis = clock.millis
            )
        )

        clock.millis += Growth.STAGE_MILLIS * 3
        val nextRun = testGameViewModel(store = store, clock = clock)

        assertEquals(Animal.FIRST_AGE + 3, nextRun.state.value.petAge)
    }

    @Test
    fun `a pet saved before the game kept track of time starts living now`() {
        // Such a save holds no timestamps at all, and reading them as "since the epoch" would greet
        // the player with an ancient pet whose bars are empty.
        val clock = FakeGameClock()
        val store = FakePlayerStateStore(
            PlayerState(selection = AnimalSelection(animalId = "cat", variantId = "white"))
        )

        val vm = testGameViewModel(store = store, clock = clock)

        assertEquals(PetStats.FULL, vm.state.value.stats)
        assertEquals(Animal.FIRST_AGE, vm.state.value.petAge)
    }
}
