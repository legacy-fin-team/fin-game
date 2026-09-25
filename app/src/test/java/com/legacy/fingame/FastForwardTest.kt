package com.legacy.fingame

import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.animals.Animal
import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.game.animals.Growth
import com.legacy.fingame.game.economy.Economy
import com.legacy.fingame.game.economy.FastForwardClock
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.game.stats.StatKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Skipping time for a demo: the clock the game is played by is pushed forward and the pet lives
 * through the skipped hours exactly as it would have lived through the wait.
 */
class FastForwardTest {

    /** Half a day, the step the demo's time button skips. */
    private val halfDay: Long = TimeUnit.HOURS.toMillis(12)

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
    fun `a pushed clock runs ahead of the one it was built over`() {
        val source = FakeGameClock()
        val clock = FastForwardClock(source)

        assertEquals(source.nowMillis(), clock.nowMillis())
        assertEquals(source.today(), clock.today())

        clock.fastForward(halfDay)

        assertEquals(source.nowMillis() + halfDay, clock.nowMillis())
    }

    @Test
    fun `a pushed clock keeps running with the device's own`() {
        val source = FakeGameClock()
        val clock = FastForwardClock(source)
        clock.fastForward(halfDay)

        source.millis += PetStats.TICK_MILLIS

        assertEquals(source.nowMillis() + halfDay, clock.nowMillis())
    }

    @Test
    fun `the calendar turns over once a whole day has been pushed past`() {
        val source = FakeGameClock()
        val clock = FastForwardClock(source)

        clock.fastForward(halfDay)
        assertEquals(source.today(), clock.today())

        clock.fastForward(halfDay)
        assertEquals(source.today() + 1, clock.today())
    }

    @Test
    fun `a clock never tells a day earlier than one it already told`() {
        val source = FakeGameClock()
        val clock = FastForwardClock(source)
        val day = clock.today()

        source.day -= 3

        assertEquals(day, clock.today())
    }

    @Test
    fun `a clock cannot be pushed backwards`() {
        val clock = FastForwardClock(FakeGameClock())

        clock.fastForward(-halfDay)
        clock.fastForward(0)

        assertEquals(FastForwardClock.NO_SHIFT, clock.shiftMillis)
    }

    @Test
    fun `a clock is picked up at the moment it was left at`() {
        val source = FakeGameClock()
        val clock = FastForwardClock(source)

        clock.fastForwardTo(source.nowMillis() + halfDay)

        assertEquals(halfDay, clock.shiftMillis)
    }

    @Test
    fun `a clock is not pushed to a moment it is already past`() {
        val source = FakeGameClock()
        val clock = FastForwardClock(source)

        clock.fastForwardTo(source.nowMillis() - halfDay)
        clock.fastForwardTo(PlayerState.CLOCK_NEVER_SAVED)

        assertEquals(FastForwardClock.NO_SHIFT, clock.shiftMillis)
    }

    @Test
    fun `a clock never tells a moment earlier than one it already told`() {
        val source = FakeGameClock()
        val clock = FastForwardClock(source)
        val told = clock.nowMillis()

        source.millis -= halfDay
        assertEquals(told, clock.nowMillis())

        // Time carries on from where it was left instead of being frozen until the device's own
        // clock has made the lost half a day up again.
        source.millis += halfDay / 2
        assertEquals(told + halfDay / 2, clock.nowMillis())
    }

    @Test
    fun `skipping time empties the bars as waiting would`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(store = storeWithPet(clock), clock = clock)

        vm.fastForward(PetStats.TICK_MILLIS * 4)

        StatKind.entries.forEach { stat ->
            assertEquals(
                PetStats.MAX_VALUE - stat.decayPerTick * 4,
                vm.state.value.stats[stat]
            )
        }
    }

    @Test
    fun `skipping time grows the pet up`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(store = storeWithPet(clock), clock = clock)
        assertEquals(Animal.FIRST_AGE, vm.state.value.petAge)

        vm.fastForward(halfDay)
        assertEquals(Animal.FIRST_AGE, vm.state.value.petAge)

        vm.fastForward(halfDay)
        assertEquals(Animal.FIRST_AGE + 1, vm.state.value.petAge)
        assertEquals(Growth.STAGE_MILLIS, halfDay * 2)
    }

    @Test
    fun `a skipped day brings the daily bonus back`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(store = storeWithPet(clock), clock = clock)
        assertTrue(vm.claimDailyBonus())

        vm.fastForward(halfDay)
        assertFalse(vm.state.value.dailyBonusAvailable)

        vm.fastForward(halfDay)
        assertTrue(vm.state.value.dailyBonusAvailable)
        assertTrue(vm.claimDailyBonus())
        assertEquals(
            Economy.STARTING_BALANCE + Economy.DAILY_BONUS * 2,
            vm.state.value.balance
        )
    }

    @Test
    fun `the time skipped is kept for as long as the app is open`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(store = storeWithPet(clock), clock = clock)

        vm.fastForward(PetStats.TICK_MILLIS * 2)
        // The device's own clock moves on afterwards: the skipped hours are not undone by it, they
        // are added to it, so the pet keeps getting hungry from where the skip left it.
        clock.millis += PetStats.TICK_MILLIS
        vm.tick()

        assertEquals(
            PetStats.MAX_VALUE - StatKind.HUNGER.decayPerTick * 3,
            vm.state.value.stats[StatKind.HUNGER]
        )
    }

    @Test
    fun `what the pet lived through while time was skipped is saved`() {
        val clock = FakeGameClock()
        val store = storeWithPet(clock)
        val vm = testGameViewModel(store = store, clock = clock)

        vm.fastForward(PetStats.TICK_MILLIS * 2)

        assertEquals(vm.state.value.stats, store.state.stats)
        assertEquals(
            clock.millis + PetStats.TICK_MILLIS * 2,
            store.state.statsUpdatedAtMillis
        )
    }

    @Test
    fun `the real time between two runs is added to the skipped time, not taken off it`() {
        val clock = FakeGameClock()
        val store = storeWithPet(clock)
        val firstRun = testGameViewModel(store = store, clock = clock)

        val skipped = PetStats.TICK_MILLIS * 12
        firstRun.fastForward(skipped)
        val hungerWhenClosed = firstRun.state.value.stats[StatKind.HUNGER]

        // The app is closed and less real time passes than the demo had skipped: the very case in
        // which the skipped hours used to be eaten by the wait instead of being kept under it.
        val away = PetStats.TICK_MILLIS * 6
        clock.millis += away
        val nextRun = testGameViewModel(store = store, clock = clock)

        assertEquals(
            hungerWhenClosed - StatKind.HUNGER.decayPerTick * 6,
            nextRun.state.value.stats[StatKind.HUNGER]
        )
        assertEquals(clock.millis + skipped, nextRun.state.value.statsUpdatedAtMillis)
    }

    @Test
    fun `the day does not fall back after a restart with a skip left over`() {
        val clock = FakeGameClock()
        val store = storeWithPet(clock)
        val firstRun = testGameViewModel(store = store, clock = clock)

        firstRun.fastForward(halfDay)
        firstRun.fastForward(halfDay)
        val dayWhenClosed = firstRun.state.value.todayDay
        assertEquals(clock.day + 1, dayWhenClosed)

        // Half an hour of real time passes, over which the device's own calendar does not turn.
        clock.millis += TimeUnit.MINUTES.toMillis(30)
        val nextRun = testGameViewModel(store = store, clock = clock)

        assertEquals(dayWhenClosed, nextRun.state.value.todayDay)
    }

    @Test
    fun `skipping no time at all changes nothing`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(store = storeWithPet(clock), clock = clock)

        vm.fastForward(0)
        vm.fastForward(-Growth.STAGE_MILLIS)

        assertEquals(PetStats.FULL, vm.state.value.stats)
        assertEquals(Animal.FIRST_AGE, vm.state.value.petAge)
    }
}
