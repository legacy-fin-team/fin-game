package com.legacy.fingame

import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.animals.Animal
import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.game.animals.Growth
import com.legacy.fingame.game.economy.Economy
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.game.items.CustomItemDraft
import com.legacy.fingame.game.items.CustomItemIcons
import com.legacy.fingame.game.items.ItemCategory
import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.game.rules.PetCare
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.game.stats.StatKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Правила ухода в игре: рост питомца, бонус дня и цены в магазине зависят от того, как ребёнок
 * заботится о питомце, а сохранение помнит серию дней без заботы.
 */
class GameCareRulesTest {

    private val day = Growth.DAY_MILLIS
    private val fish = ItemSelection(TestItems.FISH.id, "default")
    private val ball = ItemSelection(TestItems.BALL.id, "red")

    /** Бонус дня после серии из двух дней без заботы: care.xml отнимает 10% за день серии. */
    private val reducedBonus = Economy.DAILY_BONUS * 80 / 100

    /** Питомец, которого только что взяли, с запасом рыбы и мячиком, чтобы о нём заботиться. */
    private fun storeWithPet(
        clock: FakeGameClock,
        care: PetCare? = PetCare(),
        balance: Int = Economy.STARTING_BALANCE
    ) = FakePlayerStateStore(
        PlayerState(
            selection = AnimalSelection(animalId = "cat", variantId = "white"),
            balance = balance,
            owned = mapOf(fish to 99, ball to 1),
            stats = PetStats.FULL,
            statsUpdatedAtMillis = clock.millis,
            petBornAtMillis = clock.millis,
            care = care
        )
    )

    /** Сутки проходят, а питомцем никто не занимается; календарный день идёт вместе с ними. */
    private fun leaveAlone(vm: com.legacy.fingame.game.GameViewModel, clock: FakeGameClock, days: Int) {
        repeat(days) {
            clock.millis += day
            clock.day += 1
            vm.tick()
        }
    }

    /** Кормит питомца досыта и играет с ним: индекс ухода выше порога «хорошего» дня. */
    private fun careFor(vm: com.legacy.fingame.game.GameViewModel) {
        repeat(3) { vm.useItem(fish) }
        repeat(4) { vm.useItem(ball) }
    }

    @Test
    fun `a pet left alone stops growing after its first day`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(store = storeWithPet(clock), clock = clock)

        leaveAlone(vm, clock, days = 3)

        // Первый день питомец начал сытым — он засчитан, дальше шкалы пустые.
        assertEquals(day, vm.state.value.care.growthMillis)
        assertEquals(Animal.FIRST_AGE, vm.state.value.petAge)
        assertEquals(2, vm.state.value.care.neglectStreak)
    }

    @Test
    fun `a pet cared for every day grows a day of growth a day`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(store = storeWithPet(clock), clock = clock)

        repeat(3) {
            leaveAlone(vm, clock, days = 1)
            careFor(vm)
        }

        assertEquals(day * 3, vm.state.value.care.growthMillis)
        assertEquals(Animal.FIRST_AGE, vm.state.value.petAge)
        assertEquals(0, vm.state.value.care.neglectStreak)
    }

    @Test
    fun `care comes back and wipes the streak out`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(store = storeWithPet(clock), clock = clock)
        leaveAlone(vm, clock, days = 3)
        assertEquals(2, vm.state.value.care.neglectStreak)

        careFor(vm)
        leaveAlone(vm, clock, days = 1)

        assertEquals(0, vm.state.value.care.neglectStreak)
        assertEquals(day * 2, vm.state.value.care.growthMillis)
    }

    @Test
    fun `the daily bonus is smaller after days without care`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(store = storeWithPet(clock), clock = clock)
        leaveAlone(vm, clock, days = 3)

        // Серия из двух дней: минус 20% бонуса.
        assertEquals(reducedBonus, vm.state.value.dailyIncome)
        assertTrue(reducedBonus < Economy.DAILY_BONUS)
        assertTrue(vm.claimDailyBonus())

        assertEquals(Economy.STARTING_BALANCE + reducedBonus, vm.state.value.balance)
        val entry = vm.state.value.moneyLog.entries.first()
        assertEquals(MoneyLog.REASON_DAILY_BONUS, entry.reason)
        assertEquals(reducedBonus, entry.delta)
    }

    @Test
    fun `the bonus catches up with the pet before it is paid`() {
        // Таймер ещё не успел закрыть дни питомца, а бонус уже берут: штраф всё равно учтён.
        val clock = FakeGameClock()
        val vm = testGameViewModel(store = storeWithPet(clock), clock = clock)
        clock.millis += day * 3
        clock.day += 3

        assertTrue(vm.claimDailyBonus())

        assertEquals(Economy.STARTING_BALANCE + reducedBonus, vm.state.value.balance)
    }

    @Test
    fun `a pet looked after keeps the full bonus`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(store = storeWithPet(clock), clock = clock)

        assertEquals(Economy.DAILY_BONUS, vm.state.value.dailyIncome)
        assertTrue(vm.claimDailyBonus())
        assertEquals(Economy.STARTING_BALANCE + Economy.DAILY_BONUS, vm.state.value.balance)
    }

    @Test
    fun `optional goods cost more after days without care and necessary ones do not`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(store = storeWithPet(clock), clock = clock)
        leaveAlone(vm, clock, days = 3)

        assertEquals(120, vm.shopCatalog.findItemById(TestItems.HAT.id)?.price)
        assertEquals(TestItems.APPLE.price, vm.shopCatalog.findItemById(TestItems.APPLE.id)?.price)

        vm.openScreen(com.legacy.fingame.game.Screen.SHOP)
        vm.increaseQty(TestItems.HAT.id)
        vm.increaseQty(TestItems.APPLE.id)
        assertEquals(120 + TestItems.APPLE.price, vm.state.value.cartPrice)

        assertTrue(vm.buyCart())
        assertEquals(
            Economy.STARTING_BALANCE - 120 - TestItems.APPLE.price,
            vm.state.value.balance
        )
    }

    @Test
    fun `an adult's own reward costs more too, like the game's own optional goods`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(store = storeWithPet(clock), clock = clock)
        vm.enterAdultMode()
        assertTrue(
            vm.addCustomItem(
                CustomItemDraft(
                    name = "Поход в кино",
                    price = 100,
                    category = ItemCategory.OTHER,
                    iconPath = CustomItemIcons.ALL.first()
                )
            )
        )
        vm.exitAdultMode()
        val cinema = vm.state.value.customItems.single()
        leaveAlone(vm, clock, days = 3)

        assertEquals(100, vm.catalog.findItemById(cinema.id)?.price)
        assertEquals(120, vm.shopCatalog.findItemById(cinema.id)?.price)
        assertEquals(
            listOf(120),
            vm.shopCatalog.getItemsByCategory(ItemCategory.OTHER).map { it.price }
        )
    }

    @Test
    fun `a new game keeps playing by the same care rules`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(
            store = storeWithPet(clock),
            clock = clock,
            careTuning = NO_CARE_RULES
        )

        vm.resetProgress()

        assertEquals(NO_CARE_RULES, vm.state.value.careTuning)
    }

    @Test
    fun `a cart whose price went up is not paid at the old price`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(
            store = storeWithPet(clock, balance = TestItems.HAT.price),
            clock = clock
        )
        vm.openScreen(com.legacy.fingame.game.Screen.SHOP)
        vm.increaseQty(TestItems.HAT.id)

        leaveAlone(vm, clock, days = 2)

        assertFalse(vm.buyCart())
        assertEquals(TestItems.HAT.price, vm.state.value.balance)
    }

    @Test
    fun `the child is told why, and only when something is wrong`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(store = storeWithPet(clock), clock = clock)
        assertNull(vm.state.value.careHint)

        leaveAlone(vm, clock, days = 2)

        val hint = vm.state.value.careHint
        assertNotNull(hint)
        assertTrue(hint!!.contains("перестал расти"))
        assertTrue(hint.contains("1 день без заботы"))
    }

    @Test
    fun `the streak outlives a restart`() {
        val clock = FakeGameClock()
        val store = storeWithPet(clock)
        val firstRun = testGameViewModel(store = store, clock = clock)
        leaveAlone(firstRun, clock, days = 3)

        val nextRun = testGameViewModel(store = store, clock = clock)

        assertEquals(2, store.state.care?.neglectStreak)
        assertEquals(2, nextRun.state.value.care.neglectStreak)
        assertEquals(day, nextRun.state.value.care.growthMillis)
    }

    @Test
    fun `days that passed while the app was closed are judged on the next launch`() {
        val clock = FakeGameClock()
        val store = storeWithPet(clock)

        clock.millis += day * 4
        val vm = testGameViewModel(store = store, clock = clock)

        assertEquals(3, vm.state.value.care.neglectStreak)
        assertEquals(day, vm.state.value.care.growthMillis)
    }

    @Test
    fun `a save from before the care rules keeps its pet's age and starts with no streak`() {
        val clock = FakeGameClock()
        val born = clock.millis
        val store = storeWithPet(clock, care = null)
        clock.millis += day * 2 + day / 3

        val vm = testGameViewModel(store = store, clock = clock)

        assertEquals(day * 2, vm.state.value.care.growthMillis)
        assertEquals(0, vm.state.value.care.neglectStreak)
        assertEquals(Economy.DAILY_BONUS, vm.state.value.dailyIncome)
        assertEquals(born, store.state.petBornAtMillis)

        // С этого запуска дни судятся по новым правилам: брошенный питомец дальше не растёт.
        leaveAlone(vm, clock, days = 2)
        assertEquals(day * 2, vm.state.value.care.growthMillis)
        assertEquals(2, vm.state.value.care.neglectStreak)
    }

    @Test
    fun `a freshly picked pet starts with a clean slate`() {
        val clock = FakeGameClock()
        val store = storeWithPet(clock, care = PetCare(neglectStreak = 4, growthMillis = day * 2))
        val vm = testGameViewModel(store = store, clock = clock)

        vm.selectAnimal(AnimalSelection(animalId = "fish", variantId = "gold"))

        assertEquals(PetCare(), vm.state.value.care)
        assertEquals(PetCare(), store.state.care)
    }

    @Test
    fun `a cared for day makes the stats bar the best of the day`() {
        val clock = FakeGameClock()
        val store = FakePlayerStateStore(
            PlayerState(
                selection = AnimalSelection(animalId = "cat", variantId = "white"),
                owned = mapOf(fish to 3, ball to 1),
                stats = PetStats(StatKind.entries.associateWith { 0 }),
                statsUpdatedAtMillis = clock.millis,
                petBornAtMillis = clock.millis,
                care = PetCare(dayBestCare = 0.0)
            )
        )
        val vm = testGameViewModel(store = store, clock = clock)

        careFor(vm)

        assertTrue(vm.state.value.care.dayBestCare >= 0.5)
    }

    @Test
    fun `the demo time button grows a well cared for pet up in 21 days and not sooner`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(store = storeWithPet(clock), clock = clock)
        val pressesPerDay = (day / DemoMode.FAST_FORWARD_MILLIS).toInt()

        // Кнопка «+12 ч» дважды в сутки; с началом каждого нового дня питомца ребёнок кормит его
        // и играет с ним — раз в день, как и настоящий игрок (рыбы в запасе на 33 дня).
        var presses = 0
        fun press(times: Int) = repeat(times) {
            vm.fastForward(DemoMode.FAST_FORWARD_MILLIS)
            presses++
            if (presses % pressesPerDay == 0) careFor(vm)
        }

        press(10 * pressesPerDay)
        assertEquals(Animal.FIRST_AGE, vm.state.value.petAge)
        press(1 * pressesPerDay)
        assertEquals(Animal.FIRST_AGE + 1, vm.state.value.petAge)
        press(9 * pressesPerDay)
        assertTrue(vm.state.value.petAge < Growth.ADULT_AGE)
        press(1 * pressesPerDay)
        assertEquals(Growth.ADULT_AGE, vm.state.value.petAge)
        assertEquals(Growth.FULL_GROWTH_DAYS * pressesPerDay, 42)
        assertEquals(0, vm.state.value.care.neglectStreak)
    }

    @Test
    fun `the demo time button does not grow a pet nobody cares for`() {
        val clock = FakeGameClock()
        val vm = testGameViewModel(store = storeWithPet(clock), clock = clock)

        repeat(Growth.FULL_GROWTH_DAYS * 2) { vm.fastForward(DemoMode.FAST_FORWARD_MILLIS) }

        // Первый день питомец начал сытым и вырос, дальше шкалы пустые, и он не растёт.
        assertEquals(day, vm.state.value.care.growthMillis)
        assertEquals(Animal.FIRST_AGE, vm.state.value.petAge)
    }
}
