package com.legacy.fingame.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.legacy.fingame.game.animals.Animal
import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.game.animals.Growth
import com.legacy.fingame.game.economy.Economy
import com.legacy.fingame.game.economy.FastForwardClock
import com.legacy.fingame.game.economy.GameClock
import com.legacy.fingame.game.items.Item
import com.legacy.fingame.game.items.ItemCatalog
import com.legacy.fingame.game.items.ItemCategory
import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.game.items.ItemUse
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.ui.DemoContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The screens the player can navigate between.
 *
 * Used as the value of [GameUiState.screen] and passed to [GameViewModel.openScreen]
 * to switch the currently displayed screen.
 */
enum class Screen {
    /** The main/home screen. Also the screen [GameViewModel.closeScreen] returns to. */
    MAIN,
    /** The shop screen, where items grouped by category can be purchased. */
    SHOP,
    /** The inventory screen. */
    INVENTORY,
    /** The quests screen. */
    QUESTS,
    /** The locations screen, which browses through sub-locations. */
    LOCATIONS,
    /** The options/settings screen. */
    OPTIONS
}

/**
 * UI state for the game screen flow.
 *
 * Part of it is the player's game and comes back on the next launch (see [PlayerState]), the rest
 * belongs to the current visit only and starts over every time the app is opened.
 *
 * @property screen the currently displayed [Screen]. Always starts at [Screen.MAIN]: the player
 * comes back to the main screen, not to wherever the app happened to be closed.
 * @property selection the pet the player picked, restored from [PlayerState.selection], or null
 * when no pet has been picked yet and the animal selection screen is shown instead of the game.
 * @property petName name the player gave the pet, restored from [PlayerState.petName], or an empty
 * string when the pet goes unnamed; the main screen then only names the sub-location above it.
 * @property balance coins the player can spend right now, restored from [PlayerState.balance].
 * @property dailyBonusAvailable whether the daily bonus is waiting to be claimed; recomputed
 * whenever the player comes back to [Screen.MAIN], so an app left open overnight offers it again.
 * @property lastDailyBonusDay day the bonus was last claimed on, kept here only so it can be saved
 * back into [PlayerState]; the screens ask [dailyBonusAvailable] instead.
 * @property selectedCategory the shop section currently shown. Defaults to the first
 * [ItemCategory], so the shop always opens on a section that exists.
 * @property quantities how many of each item the player put in the shop cart, keyed by item id.
 * Food is counted up to [GameViewModel.MAX_ITEM_QUANTITY]; anything else is either in the cart once
 * or not at all. The cart belongs to one visit and is dropped when the player leaves
 * [Screen.SHOP], so an unpaid pick never turns into a purchase later.
 * @property pickedVariants which variant of an item the player wants, keyed by item id; an item the
 * player didn't choose for is shown and bought in its [Item.defaultVariantId]. Part of the cart, so
 * it is dropped together with it.
 * @property cartPrice what everything in [quantities] costs together, kept in the state because
 * only the view model knows the prices.
 * @property owned how many of each item and variant the player already bought, restored from
 * [PlayerState.owned].
 * @property worn which of the owned items are on the pet right now, restored from
 * [PlayerState.worn]. What is drawn on the game area's layers is built out of it (see
 * [com.legacy.fingame.game.scene.GameScene]).
 * @property stats the pet's stat bars, restored from [PlayerState.stats] and brought up to date by
 * [GameViewModel.tick] as time passes.
 * @property statsUpdatedAtMillis moment [stats] were last brought up to date, kept here so it can be
 * saved back into [PlayerState]; the screens ask [stats] instead.
 * @property petAge age stage the pet has grown to, worked out from [petBornAtMillis] rather than
 * saved (see [Growth.ageAt]). Resolving it against the stages the animal actually has is left to
 * [com.legacy.fingame.game.animals.Animal.getIdleSpritePath].
 * @property petBornAtMillis moment the pet was taken in, kept here only so it can be saved back into
 * [PlayerState]; the screens ask [petAge] instead.
 * @property subLocationIndex index of the currently displayed sub-location within
 * [DemoContent.subLocationTitles], restored from [PlayerState.subLocationIndex].
 */
data class GameUiState(
    val screen: Screen = Screen.MAIN,
    val selection: AnimalSelection? = null,
    val petName: String = "",
    val balance: Int = Economy.STARTING_BALANCE,
    val dailyBonusAvailable: Boolean = false,
    val lastDailyBonusDay: Long = Economy.NEVER_CLAIMED,
    val selectedCategory: ItemCategory = ItemCategory.entries.first(),
    val quantities: Map<String, Int> = emptyMap(),
    val pickedVariants: Map<String, String> = emptyMap(),
    val cartPrice: Int = 0,
    val owned: Map<ItemSelection, Int> = emptyMap(),
    val worn: Set<ItemSelection> = emptySet(),
    val stats: PetStats = PetStats.FULL,
    val statsUpdatedAtMillis: Long = PlayerState.NEVER_UPDATED,
    val petAge: Int = Animal.FIRST_AGE,
    val petBornAtMillis: Long = Growth.NOT_BORN,
    val subLocationIndex: Int = 0
) {
    /** Whether the cart holds something the player can actually pay for. */
    val canBuyCart: Boolean get() = cartPrice in 1..balance

    /**
     * @param item item shown in the shop.
     * @return The variant the player picked for [item], or its default one while nothing is picked.
     */
    fun pickedVariantOf(item: Item): String = pickedVariants[item.id] ?: item.defaultVariantId

    /**
     * @param item item shown in the shop.
     * @return How many of [item] the player owns in the variant currently picked for it.
     */
    fun ownedCountOf(item: Item): Int =
        owned[ItemSelection(item.id, pickedVariantOf(item))] ?: 0
}

/**
 * State holder for the game screen: the pet the player plays with, how it is doing and what it wears,
 * screen navigation, the shop cart and the player's money.
 *
 * The player's game is restored from [store] when the view model is created and written back to it
 * on every change, so closing the app — or having its process killed — doesn't lose the pet, the
 * sub-location it was left in, the coins earned, the items bought with them or the outfit the pet
 * was left in. The pet, on the other hand, does not stand still while the app is closed: its stat
 * bars fall and it grows up with the clock, which [tick] catches up with.
 *
 * @param store where the player's state is restored from and saved to.
 * @param catalog what is on sale; the view model needs it to know what the cart costs, to tell food
 * (bought by the handful) from items that are bought once, and to know what using an item does to
 * the pet.
 * @param clock where the current day comes from, for the once-a-day bonus, and the current moment,
 * for the pet's stats and its growth. The view model reads it through a [FastForwardClock], so a
 * demo build can push the game's time forward (see [fastForward]) without the rest of the game
 * knowing about it.
 */
class GameViewModel(
    private val store: PlayerStateStore,
    private val catalog: ItemCatalog,
    clock: GameClock = GameClock.DEVICE
) : ViewModel() {

    /**
     * The clock the game is played by: the one it was given, plus whatever [fastForward] has pushed
     * it by. Untouched, it is the given clock itself.
     */
    private val clock = FastForwardClock(clock)

    /**
     * Constant limits for [GameViewModel]'s UI state, and the way it is built outside of tests.
     */
    companion object {
        /**
         * Maximum value a [GameUiState.quantities] counter can reach for food. Keeps the quantity
         * text short enough to always fit on screen.
         */
        const val MAX_ITEM_QUANTITY = 99

        /**
         * How many of a non-food item can be in the cart: such an item is owned or not owned, so
         * buying a second one would pay for nothing.
         */
        const val SINGLE_ITEM_QUANTITY = 1

        /**
         * Builds a [GameViewModel] over a store and a catalog, for `viewModel(factory = ...)`: the
         * view model needs both the moment it is created, since that is when the player's game is
         * restored.
         *
         * @param store where the player's state is restored from and saved to.
         * @param catalog what is on sale.
         * @param clock where the current day comes from; defaults to the device's calendar day.
         * @return A factory creating a [GameViewModel] backed by [store] and [catalog].
         */
        fun factory(
            store: PlayerStateStore,
            catalog: ItemCatalog,
            clock: GameClock = GameClock.DEVICE
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { GameViewModel(store, catalog, clock) }
        }
    }

    private val _state = MutableStateFlow(restoredState())

    /** Current [GameUiState], observed by the UI. */
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    /**
     * Switches the currently displayed screen.
     *
     * To return to the main screen, prefer [closeScreen] instead of passing [Screen.MAIN] here.
     * When navigating away from [Screen.SHOP] to a different screen, the unpaid shop cart is
     * dropped.
     *
     * @param screen the screen to navigate to.
     */
    fun openScreen(screen: Screen) {
        _state.value = stateForNavigatingTo(screen)
    }

    /**
     * Returns the player to the main screen, equivalent to `openScreen(`[Screen.MAIN]`)`.
     * If the player was on [Screen.SHOP], the unpaid shop cart is dropped.
     */
    fun closeScreen() {
        _state.value = stateForNavigatingTo(Screen.MAIN)
    }

    /**
     * Builds the state resulting from navigating to [screen]: the unpaid cart is dropped when the
     * player leaves [Screen.SHOP], and whether the daily bonus is waiting is asked again, since the
     * app may have been sitting open since yesterday.
     *
     * @param screen the screen to navigate to.
     */
    private fun stateForNavigatingTo(screen: Screen): GameUiState {
        val previous = _state.value
        val leavingShop = previous.screen == Screen.SHOP && screen != Screen.SHOP
        return previous.copy(
            screen = screen,
            quantities = if (leavingShop) emptyMap() else previous.quantities,
            pickedVariants = if (leavingShop) emptyMap() else previous.pickedVariants,
            cartPrice = if (leavingShop) 0 else previous.cartPrice,
            dailyBonusAvailable = Economy.isDailyBonusAvailable(
                lastClaimedDay = previous.lastDailyBonusDay,
                today = clock.today()
            )
        )
    }

    /**
     * Takes the pet the player picked on the animal selection screen, for this run and for every
     * run after it.
     *
     * A freshly picked pet is a newborn one: it starts at the youngest age stage with full stat bars,
     * and grows and gets hungry from this moment on.
     *
     * @param selection the animal and the variant the player picked.
     * @param name the name the player gave it on the second step of the selection; the surrounding
     * spaces are dropped, and a name that is nothing but spaces counts as no name at all.
     */
    fun selectAnimal(selection: AnimalSelection, name: String = "") {
        val now = clock.nowMillis()
        _state.value = _state.value.copy(
            selection = selection,
            petName = name.trim(),
            stats = PetStats.FULL,
            statsUpdatedAtMillis = now,
            petAge = Animal.FIRST_AGE,
            petBornAtMillis = now
        )
        persist()
    }

    /**
     * Brings the pet up to date with the clock: lets the stat bars fall for the time that has passed
     * and lets the pet grow into the age stage it has reached by now.
     *
     * Called both when the app comes back — the pet lived on while it was closed — and on a timer
     * while the player is watching, so the bars go down in front of them. Calling it more often than
     * the pet actually changes costs nothing and changes nothing: the leftover time below one
     * [PetStats.TICK_MILLIS] is kept for the next call instead of being dropped.
     */
    fun tick() {
        val current = _state.value
        val now = clock.nowMillis()
        val ticks = PetStats.ticksBetween(current.statsUpdatedAtMillis, now)
        val age = Growth.ageAt(current.petBornAtMillis, now)
        if (ticks == 0L && age == current.petAge) return

        _state.value = current.copy(
            stats = current.stats.decayedBy(ticks),
            statsUpdatedAtMillis = current.statsUpdatedAtMillis + ticks * PetStats.TICK_MILLIS,
            petAge = age
        )
        persist()
    }

    /**
     * Skips the given amount of time: the game's clock is pushed forward and the pet is brought up
     * to date with it, so it gets as hungry, grows as much and comes as close to its next daily
     * bonus as it would have by the player simply waiting.
     *
     * This is what the demo build's time button does (see
     * [com.legacy.fingame.DemoMode.FAST_FORWARD_MILLIS]) and it is the only thing it does: nothing is
     * written into the pet by hand, the stats fall through the very same [tick] the waiting player's
     * pet falls through. The skipped time lives for as long as the app is open; the moments saved
     * for the next launch are the skipped-to ones, so a pet pushed into the future simply stands
     * still until the device's own clock catches up with it.
     *
     * @param millis how much time to skip; zero or less skips nothing.
     */
    fun fastForward(millis: Long) {
        if (millis <= 0) return
        clock.fastForward(millis)

        val current = _state.value
        _state.value = current.copy(
            dailyBonusAvailable = Economy.isDailyBonusAvailable(
                lastClaimedDay = current.lastDailyBonusDay,
                today = clock.today()
            )
        )
        tick()
    }

    /**
     * Uses an item the player owns on the pet: the pet eats the food, plays with the toy, and its
     * stat bars move by the item's [Item.effects] either way.
     *
     * Food is gone once it is eaten, a toy stays in the inventory to be played with again, and
     * anything the pet can wear is not used at all — it is put on and taken off through
     * [toggleWorn].
     *
     * @param selection the item and the variant of it to use, as the inventory holds it.
     * @return True when the item was used, false when the player doesn't own it, it is not registered
     * any more, or it is worn rather than used.
     */
    fun useItem(selection: ItemSelection): Boolean {
        val current = _state.value
        val item = catalog.findItemById(selection.itemId) ?: return false
        if (item.isWearable) return false

        val count = current.owned[selection] ?: 0
        if (count <= 0) return false

        val owned = if (item.category.use == ItemUse.CONSUMED) {
            if (count == 1) current.owned - selection else current.owned + (selection to count - 1)
        } else {
            current.owned
        }

        _state.value = current.copy(
            owned = owned,
            stats = current.stats.changedBy(item.effects)
        )
        persist()
        return true
    }

    /**
     * Puts a piece of clothing or a decoration on the pet, or takes it off again. What is on the pet
     * is the player's own and comes back on the next launch.
     *
     * Only one variant of the same item can be on at a time: putting on the white hat takes the black
     * one off, since the pet has but one head. Putting an item on is using it, so the item's
     * [Item.effects] are applied then; taking it off does not take them back — the pet was happy to
     * wear it while it did.
     *
     * @param selection the item and the variant of it to put on or take off.
     * @return True when the item was put on or taken off, false when the player doesn't own it, it is
     * not registered any more, or it is not something the pet can wear.
     */
    fun toggleWorn(selection: ItemSelection): Boolean {
        val current = _state.value
        val item = catalog.findItemById(selection.itemId) ?: return false
        if (!item.isWearable) return false
        if ((current.owned[selection] ?: 0) <= 0) return false

        val takingOff = selection in current.worn
        val worn = if (takingOff) {
            current.worn - selection
        } else {
            current.worn.filterNot { it.itemId == selection.itemId }.toSet() + selection
        }

        _state.value = current.copy(
            worn = worn,
            stats = if (takingOff) current.stats else current.stats.changedBy(item.effects)
        )
        persist()
        return true
    }

    /**
     * Selects the shop section currently shown to the player.
     *
     * @param category the category to show.
     */
    fun selectCategory(category: ItemCategory) {
        _state.value = _state.value.copy(selectedCategory = category)
    }

    /**
     * Puts one more of the given item into the shop cart, as far as the item allows: food goes up to
     * [MAX_ITEM_QUANTITY], anything else up to [SINGLE_ITEM_QUANTITY] and not at all once the player
     * owns it in the picked variant. Calling this at the limit — or for an item that is not on sale
     * — has no effect.
     *
     * @param itemId id of the item to add.
     */
    fun increaseQty(itemId: String) {
        val current = _state.value
        val item = catalog.findItemById(itemId) ?: return
        val quantity = current.quantities[itemId] ?: 0
        if (quantity >= maxQuantityOf(item, current)) return

        _state.value = current.withCart(current.quantities + (itemId to quantity + 1))
    }

    /**
     * Takes one of the given item out of the shop cart. The counter does not go below zero; calling
     * this when the counter is already zero has no effect.
     *
     * @param itemId id of the item to remove.
     */
    fun decreaseQty(itemId: String) {
        val current = _state.value
        val quantity = (current.quantities[itemId] ?: 0) - 1
        if (quantity < 0) return

        _state.value = current.withCart(current.quantities + (itemId to quantity))
    }

    /**
     * Picks the variant an item is shown and bought in.
     *
     * Switching to a variant the player already owns takes the item out of the cart: it is that
     * very variant that would have been paid for.
     *
     * @param itemId id of the item to pick a variant for.
     * @param variantId id of the variant, one this item actually has; anything else is ignored, as
     * it could only come from data that changed under the player's feet.
     */
    fun pickVariant(itemId: String, variantId: String) {
        val current = _state.value
        val item = catalog.findItemById(itemId) ?: return
        if (variantId !in item.variantIds) return

        val picked = current.copy(pickedVariants = current.pickedVariants + (itemId to variantId))
        val quantity = picked.quantities[itemId] ?: 0
        val allowed = quantity.coerceAtMost(maxQuantityOf(item, picked))

        _state.value = picked.withCart(picked.quantities + (itemId to allowed))
    }

    /**
     * Pays for everything in the shop cart at once: the coins leave [GameUiState.balance] and the
     * items — each in the variant it was picked in — join [GameUiState.owned] for good.
     *
     * An empty cart, or one the player cannot afford, buys nothing at all: a purchase is never
     * partial, so the player either gets the whole cart or keeps the money.
     *
     * A paid-for cart ends the visit: the shop closes and the player is back on [Screen.MAIN] with
     * the pet, which is what the purchase was for. The cart is emptied either way, so nothing of it
     * is left to be paid for twice.
     *
     * @return True when the purchase went through, false when there was nothing to buy or not
     * enough money for it.
     */
    fun buyCart(): Boolean {
        val current = _state.value
        if (!current.canBuyCart) return false

        val owned = current.owned.toMutableMap()
        current.quantities.forEach { (itemId, quantity) ->
            if (quantity <= 0) return@forEach
            val item = catalog.findItemById(itemId) ?: return@forEach
            val key = ItemSelection(itemId, current.pickedVariantOf(item))
            owned[key] = (owned[key] ?: 0) + quantity
        }

        _state.value = stateForNavigatingTo(Screen.MAIN).copy(
            balance = current.balance - current.cartPrice,
            owned = owned.toMap(),
            quantities = emptyMap(),
            pickedVariants = emptyMap(),
            cartPrice = 0
        )
        persist()
        return true
    }

    /**
     * Adds coins the player earned to the balance and remembers them right away, so money is never
     * lost to the app being closed.
     *
     * @param amount coins to add; zero or less is ignored, as spending goes through [buyCart].
     */
    fun earn(amount: Int) {
        if (amount <= 0) return
        _state.value = _state.value.let { it.copy(balance = it.balance + amount) }
        persist()
    }

    /**
     * Hands the player the daily bonus, once per calendar day (see [Economy.isDailyBonusAvailable]).
     *
     * @return True when the bonus was paid out, false when this day has already paid.
     */
    fun claimDailyBonus(): Boolean {
        val current = _state.value
        val today = clock.today()
        if (!Economy.isDailyBonusAvailable(current.lastDailyBonusDay, today)) return false

        _state.value = current.copy(
            balance = current.balance + Economy.DAILY_BONUS,
            lastDailyBonusDay = today,
            dailyBonusAvailable = false
        )
        persist()
        return true
    }

    /**
     * Advances to the next sub-location, wrapping back to the first sub-location
     * after the last one. The pet stays there until the player moves it again, including
     * across app launches.
     */
    fun nextSubLocation() {
        val count = DemoContent.subLocationCount
        if (count == 0) return
        val current = _state.value.subLocationIndex
        _state.value = _state.value.copy(subLocationIndex = (current + 1) % count)
        persist()
    }

    /**
     * Goes back to the previous sub-location, wrapping around to the last
     * sub-location when moving before the first one. The pet stays there until the player
     * moves it again, including across app launches.
     */
    fun prevSubLocation() {
        val count = DemoContent.subLocationCount
        if (count == 0) return
        val current = _state.value.subLocationIndex
        _state.value = _state.value.copy(subLocationIndex = (current - 1 + count) % count)
        persist()
    }

    /**
     * How many of an item may be in the cart at once.
     *
     * @param item the item in question.
     * @param state state the picked variants and the owned items are read from.
     * @return [MAX_ITEM_QUANTITY] for an item that is used up and bought again, i.e. food;
     * [SINGLE_ITEM_QUANTITY] for anything else, or zero once the player owns it in the picked variant.
     */
    private fun maxQuantityOf(item: Item, state: GameUiState): Int = when {
        item.category.use == ItemUse.CONSUMED -> MAX_ITEM_QUANTITY
        state.ownedCountOf(item) > 0 -> 0
        else -> SINGLE_ITEM_QUANTITY
    }

    /**
     * Puts a changed cart into the state together with what it now costs, so the price and the
     * counters can never drift apart.
     *
     * @param quantities the cart contents.
     * @return This state with the new cart and its price.
     */
    private fun GameUiState.withCart(quantities: Map<String, Int>): GameUiState =
        copy(quantities = quantities, cartPrice = priceOf(quantities))

    /**
     * @param quantities cart contents, keyed by item id.
     * @return What the cart costs; items that are no longer on sale count as nothing, since the
     * player cannot be charged for what the shop can't hand over.
     */
    private fun priceOf(quantities: Map<String, Int>): Int =
        quantities.entries.sumOf { (itemId, quantity) ->
            if (quantity <= 0) 0 else (catalog.findItemById(itemId)?.price ?: 0) * quantity
        }

    /**
     * Builds the state the app starts with out of the [PlayerState] the previous run left behind.
     *
     * @return The initial [GameUiState]: the player's game as it was saved, everything else fresh.
     */
    private fun restoredState(): GameUiState {
        val saved = store.load()
        val now = clock.nowMillis()
        // A pet saved before the game kept track of time starts living now: the alternative is to
        // treat it as having been neglected since the epoch and greet the player with an empty pet.
        val statsUpdatedAt = saved.statsUpdatedAtMillis.takeUnless {
            it == PlayerState.NEVER_UPDATED
        } ?: now
        val bornAt = saved.petBornAtMillis.takeUnless {
            it == Growth.NOT_BORN && saved.selection != null
        } ?: now
        val ticks = PetStats.ticksBetween(statsUpdatedAt, now)

        return GameUiState(
            selection = saved.selection,
            petName = saved.petName,
            balance = saved.balance,
            lastDailyBonusDay = saved.lastDailyBonusDay,
            dailyBonusAvailable = Economy.isDailyBonusAvailable(
                lastClaimedDay = saved.lastDailyBonusDay,
                today = clock.today()
            ),
            owned = saved.owned,
            worn = wearableOf(saved.worn, saved.owned),
            stats = saved.stats.decayedBy(ticks),
            statsUpdatedAtMillis = statsUpdatedAt + ticks * PetStats.TICK_MILLIS,
            petAge = Growth.ageAt(bornAt, now),
            petBornAtMillis = bornAt,
            subLocationIndex = existingSubLocation(saved.subLocationIndex)
        )
    }

    /**
     * Keeps on the pet only what it can still be wearing: the items may change between two launches,
     * and something the player no longer owns — or that is no longer worn at all — cannot stay on.
     *
     * @param worn items the pet had on as they were saved.
     * @param owned items the player owns.
     * @return The saved items the pet is still allowed to wear.
     */
    private fun wearableOf(
        worn: Set<ItemSelection>,
        owned: Map<ItemSelection, Int>
    ): Set<ItemSelection> = worn.filterTo(mutableSetOf()) { selection ->
        (owned[selection] ?: 0) > 0 && catalog.findItemById(selection.itemId)?.isWearable == true
    }

    /**
     * Keeps a restored sub-location index pointing at a sub-location that is actually there: the
     * saved one may be gone, since the sub-locations can change between two launches of the app.
     *
     * @param index sub-location index as it was saved.
     * @return The nearest index within the sub-locations that exist now, or the first one when
     * there are no sub-locations at all.
     */
    private fun existingSubLocation(index: Int): Int {
        val count = DemoContent.subLocationCount
        if (count == 0) return 0
        return index.coerceIn(0, count - 1)
    }

    /**
     * Hands the part of the current state that belongs to the player's game over to [store], so
     * the next launch finds it in place.
     */
    private fun persist() {
        val current = _state.value
        store.save(
            PlayerState(
                selection = current.selection,
                petName = current.petName,
                subLocationIndex = current.subLocationIndex,
                balance = current.balance,
                lastDailyBonusDay = current.lastDailyBonusDay,
                owned = current.owned,
                worn = current.worn,
                stats = current.stats,
                statsUpdatedAtMillis = current.statsUpdatedAtMillis,
                petBornAtMillis = current.petBornAtMillis
            )
        )
    }
}
