package com.legacy.fingame.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.game.economy.Economy
import com.legacy.fingame.game.economy.GameClock
import com.legacy.fingame.game.items.Item
import com.legacy.fingame.game.items.ItemCatalog
import com.legacy.fingame.game.items.ItemCategory
import com.legacy.fingame.game.items.ItemSelection
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
 * @property subLocationIndex index of the currently displayed sub-location within
 * [DemoContent.subLocationTitles], restored from [PlayerState.subLocationIndex].
 */
data class GameUiState(
    val screen: Screen = Screen.MAIN,
    val selection: AnimalSelection? = null,
    val balance: Int = Economy.STARTING_BALANCE,
    val dailyBonusAvailable: Boolean = false,
    val lastDailyBonusDay: Long = Economy.NEVER_CLAIMED,
    val selectedCategory: ItemCategory = ItemCategory.entries.first(),
    val quantities: Map<String, Int> = emptyMap(),
    val pickedVariants: Map<String, String> = emptyMap(),
    val cartPrice: Int = 0,
    val owned: Map<ItemSelection, Int> = emptyMap(),
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
 * State holder for the game screen: the pet the player plays with, screen navigation, the shop cart
 * and the player's money.
 *
 * The player's game is restored from [store] when the view model is created and written back to it
 * on every change, so closing the app — or having its process killed — doesn't lose the pet, the
 * sub-location it was left in, the coins earned or the items bought with them.
 *
 * @param store where the player's state is restored from and saved to.
 * @param catalog what is on sale; the view model needs it to know what the cart costs and to tell
 * food (bought by the handful) from items that are bought once.
 * @param clock where the current day comes from, for the once-a-day bonus.
 */
class GameViewModel(
    private val store: PlayerStateStore,
    private val catalog: ItemCatalog,
    private val clock: GameClock = GameClock.DEVICE
) : ViewModel() {

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
     * @param selection the animal and the variant the player picked.
     */
    fun selectAnimal(selection: AnimalSelection) {
        _state.value = _state.value.copy(selection = selection)
        persist()
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
        if (!item.variants.containsKey(variantId)) return

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

        _state.value = current.copy(
            balance = current.balance - current.cartPrice,
            owned = owned.toMap(),
            quantities = emptyMap(),
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
     * @return [MAX_ITEM_QUANTITY] for food, which is eaten and bought again; [SINGLE_ITEM_QUANTITY]
     * for anything else, or zero once the player owns it in the picked variant.
     */
    private fun maxQuantityOf(item: Item, state: GameUiState): Int = when {
        item.category == ItemCategory.FOOD -> MAX_ITEM_QUANTITY
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
        return GameUiState(
            selection = saved.selection,
            balance = saved.balance,
            lastDailyBonusDay = saved.lastDailyBonusDay,
            dailyBonusAvailable = Economy.isDailyBonusAvailable(
                lastClaimedDay = saved.lastDailyBonusDay,
                today = clock.today()
            ),
            owned = saved.owned,
            subLocationIndex = existingSubLocation(saved.subLocationIndex)
        )
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
                subLocationIndex = current.subLocationIndex,
                balance = current.balance,
                lastDailyBonusDay = current.lastDailyBonusDay,
                owned = current.owned
            )
        )
    }
}
