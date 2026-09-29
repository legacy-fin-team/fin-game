package com.legacy.fingame

import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.animals.Animal
import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.game.animals.Growth
import com.legacy.fingame.game.economy.Economy
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.game.stats.StatKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Closing the app and opening it again: the player finds the pet where it was left, however that
 * point in time was reached — by waiting, by skipping time in a demo build, or by a device clock
 * that has been moved since.
 *
 * Питомца здесь нарочно оставляют одного, а проверяются часы, поэтому игра идёт без правил ухода
 * ([NO_CARE_RULES]); сами правила проверяет [GameCareRulesTest].
 */
class RestartTest {

    /**
     * Builds a store holding a pet that was taken in at the moment the test's clock starts at, so
     * the pet is a newborn one with full bars and nothing has yet been let to pass over it.
     *
     * @param clock clock the game is played by.
     * @return A store standing for an app that was already played and closed.
     */
    private fun storeWithPet(clock: FakeGameClock) = FakePlayerStateStore(
        PlayerState(
            selection = AnimalSelection(animalId = "cat", variantId = "white"),
            stats = PetStats.FULL,
            statsUpdatedAtMillis = clock.millis,
            petBornAtMillis = clock.millis
        )
    )

    @Test
    fun `a pet grown while the app was open comes back grown up`() {
        val clock = FakeGameClock()
        val store = storeWithPet(clock)
        val firstRun = testGameViewModel(store = store, clock = clock, careTuning = NO_CARE_RULES)

        clock.millis += Growth.STAGE_MILLIS * 2
        firstRun.tick()
        assertEquals(Animal.FIRST_AGE + 2, firstRun.state.value.petAge)

        val nextRun = testGameViewModel(store = store, clock = clock, careTuning = NO_CARE_RULES)

        assertEquals(Animal.FIRST_AGE + 2, nextRun.state.value.petAge)
    }

    @Test
    fun `a pet grown by skipping time comes back grown up`() {
        val clock = FakeGameClock()
        val store = storeWithPet(clock)
        val firstRun = testGameViewModel(store = store, clock = clock, careTuning = NO_CARE_RULES)

        firstRun.fastForward(Growth.STAGE_MILLIS * 2)
        assertEquals(Animal.FIRST_AGE + 2, firstRun.state.value.petAge)

        val nextRun = testGameViewModel(store = store, clock = clock, careTuning = NO_CARE_RULES)

        assertEquals(Animal.FIRST_AGE + 2, nextRun.state.value.petAge)
    }

    @Test
    fun `the bars come back where skipping time left them`() {
        val clock = FakeGameClock()
        val store = storeWithPet(clock)
        val firstRun = testGameViewModel(store = store, clock = clock, careTuning = NO_CARE_RULES)

        firstRun.fastForward(PetStats.TICK_MILLIS * 3)
        val skippedTo = firstRun.state.value.stats

        val nextRun = testGameViewModel(store = store, clock = clock, careTuning = NO_CARE_RULES)

        assertEquals(skippedTo, nextRun.state.value.stats)
        StatKind.entries.forEach { stat ->
            assertEquals(
                PetStats.MAX_VALUE - stat.decayPerTick * 3,
                nextRun.state.value.stats[stat]
            )
        }
    }

    @Test
    fun `a device clock moved back does not make the pet younger`() {
        val clock = FakeGameClock()
        val store = storeWithPet(clock)
        val firstRun = testGameViewModel(store = store, clock = clock, careTuning = NO_CARE_RULES)

        clock.millis += Growth.FULL_GROWTH_MILLIS
        firstRun.tick()
        assertEquals(Growth.ADULT_AGE, firstRun.state.value.petAge)

        // The player turns the device's clock back a week between the two launches.
        clock.millis -= Growth.FULL_GROWTH_MILLIS * 3
        val nextRun = testGameViewModel(store = store, clock = clock, careTuning = NO_CARE_RULES)

        assertEquals(Growth.ADULT_AGE, nextRun.state.value.petAge)
    }

    @Test
    fun `a device clock moved back while the app is open does not make the pet younger`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(store = storeWithPet(clock), clock = clock, careTuning = NO_CARE_RULES)

        clock.millis += Growth.DAY_MILLIS * 11
        vm.tick()
        assertEquals(Animal.FIRST_AGE + 1, vm.state.value.petAge)

        clock.millis -= Growth.DAY_MILLIS * 22
        vm.tick()

        assertEquals(Animal.FIRST_AGE + 1, vm.state.value.petAge)
    }

    @Test
    fun `a moment behind the pet's own takes nothing off its bars`() {
        val clock = FakeGameClock()
        val store = storeWithPet(clock)
        val firstRun = testGameViewModel(store = store, clock = clock, careTuning = NO_CARE_RULES)

        firstRun.fastForward(PetStats.TICK_MILLIS * 2)
        val skippedTo = firstRun.state.value.stats

        // Both ways round a negative interval means the same: no time has passed for the pet.
        clock.millis -= PetStats.TICK_MILLIS * 10
        firstRun.tick()
        assertEquals(skippedTo, firstRun.state.value.stats)

        val nextRun = testGameViewModel(store = store, clock = clock, careTuning = NO_CARE_RULES)
        assertEquals(skippedTo, nextRun.state.value.stats)
    }

    @Test
    fun `the day a skip reached is the day the game comes back on`() {
        val clock = FakeGameClock()
        val store = storeWithPet(clock)
        val firstRun = testGameViewModel(store = store, clock = clock, careTuning = NO_CARE_RULES)

        firstRun.fastForward(Growth.DAY_MILLIS)
        assertEquals(clock.day + 1, firstRun.state.value.todayDay)

        val nextRun = testGameViewModel(store = store, clock = clock, careTuning = NO_CARE_RULES)

        assertEquals(clock.day + 1, nextRun.state.value.todayDay)
    }

    @Test
    fun `a skipped day that has already paid does not pay again after a restart`() {
        val clock = FakeGameClock()
        val store = storeWithPet(clock)
        val firstRun = testGameViewModel(store = store, clock = clock, careTuning = NO_CARE_RULES)

        firstRun.fastForward(Growth.DAY_MILLIS)
        assertTrue(firstRun.claimDailyBonus())
        val earned = firstRun.state.value.balance

        val nextRun = testGameViewModel(store = store, clock = clock, careTuning = NO_CARE_RULES)
        assertFalse(nextRun.state.value.dailyBonusAvailable)
        assertFalse(nextRun.claimDailyBonus())
        assertEquals(earned, nextRun.state.value.balance)

        // The bonus is delayed by the skipped day, not lost with it: the next day pays again.
        nextRun.fastForward(Growth.DAY_MILLIS)

        assertTrue(nextRun.state.value.dailyBonusAvailable)
        assertTrue(nextRun.claimDailyBonus())
        assertEquals(earned + Economy.DAILY_BONUS, nextRun.state.value.balance)
    }
}
