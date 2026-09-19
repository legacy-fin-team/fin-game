package com.legacy.fingame.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.legacy.fingame.game.animals.AnimalSelection
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
 * @property selectedCategoryId id of the currently selected shop category, one of
 * [DemoContent.categoryIds]. Defaults to the first available category.
 * @property quantities per-item counters keyed by item id (see [DemoContent.itemIds]).
 * This is UI-only state (e.g. quantity pickers in the shop) and is not related to an
 * actual purchase/cart. Each counter is capped at [GameViewModel.MAX_ITEM_QUANTITY] and
 * is reset whenever the player navigates away from [Screen.SHOP], so an unpurchased
 * selection does not persist across shop visits.
 * @property subLocationIndex index of the currently displayed sub-location within
 * [DemoContent.subLocationTitles], restored from [PlayerState.subLocationIndex].
 */
data class GameUiState(
    val screen: Screen = Screen.MAIN,
    val selection: AnimalSelection? = null,
    val selectedCategoryId: String = DemoContent.categoryIds.first(),
    val quantities: Map<String, Int> = emptyMap(),
    val subLocationIndex: Int = 0
)

/**
 * State holder for the game screen: the pet the player plays with, screen navigation, category
 * selection, item counters and sub-location index. Holds no domain logic (balance, cart, catalog).
 *
 * The player's game is restored from [store] when the view model is created and written back to it
 * on every change, so closing the app — or having its process killed — doesn't lose the pet or the
 * sub-location it was left in.
 *
 * @param store where the player's state is restored from and saved to.
 */
class GameViewModel(private val store: PlayerStateStore) : ViewModel() {

    /**
     * Constant limits for [GameViewModel]'s UI state, and the way it is built outside of tests.
     */
    companion object {
        /**
         * Maximum value a [GameUiState.quantities] counter can reach. Keeps the quantity
         * text short enough to always fit on screen.
         */
        const val MAX_ITEM_QUANTITY = 99

        /**
         * Builds a [GameViewModel] over a store, for `viewModel(factory = ...)`: the view model
         * needs the store the moment it is created, since that is when the player's game is
         * restored.
         *
         * @param store where the player's state is restored from and saved to.
         * @return A factory creating a [GameViewModel] backed by [store].
         */
        fun factory(store: PlayerStateStore): ViewModelProvider.Factory = viewModelFactory {
            initializer { GameViewModel(store) }
        }
    }

    private val _state = MutableStateFlow(restoredState())

    /** Current [GameUiState], observed by the UI. */
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    /**
     * Switches the currently displayed screen.
     *
     * To return to the main screen, prefer [closeScreen] instead of passing [Screen.MAIN] here.
     * When navigating away from [Screen.SHOP] to a different screen, any unpurchased
     * [GameUiState.quantities] picked in the shop are reset.
     *
     * @param screen the screen to navigate to.
     */
    fun openScreen(screen: Screen) {
        _state.value = stateForNavigatingTo(screen)
    }

    /**
     * Returns the player to the main screen, equivalent to `openScreen(`[Screen.MAIN]`)`.
     * If the player was on [Screen.SHOP], any unpurchased [GameUiState.quantities] picked
     * in the shop are reset.
     */
    fun closeScreen() {
        _state.value = stateForNavigatingTo(Screen.MAIN)
    }

    /**
     * Builds the state resulting from navigating to [screen], resetting
     * [GameUiState.quantities] when the current screen is [Screen.SHOP] and [screen] is not.
     *
     * @param screen the screen to navigate to.
     */
    private fun stateForNavigatingTo(screen: Screen): GameUiState {
        val previous = _state.value
        val quantities = if (previous.screen == Screen.SHOP && screen != Screen.SHOP) {
            emptyMap()
        } else {
            previous.quantities
        }
        return previous.copy(screen = screen, quantities = quantities)
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
     * Selects the shop category currently shown to the player.
     *
     * @param categoryId id of the category to select, one of [DemoContent.categoryIds].
     */
    fun selectCategory(categoryId: String) {
        _state.value = _state.value.copy(selectedCategoryId = categoryId)
    }

    /**
     * Increments the quantity counter for the given item by one, up to
     * [MAX_ITEM_QUANTITY]. Calling this when the counter is already at
     * [MAX_ITEM_QUANTITY] has no effect.
     *
     * @param itemId id of the item whose counter to increase, the same id the shop
     * uses to build the item's card (see [DemoContent.itemIds]).
     */
    fun increaseQty(itemId: String) {
        val current = _state.value.quantities
        val newQty = minOf((current[itemId] ?: 0) + 1, MAX_ITEM_QUANTITY)
        val updated = current + (itemId to newQty)
        _state.value = _state.value.copy(quantities = updated)
    }

    /**
     * Decrements the quantity counter for the given item by one. The counter does
     * not go below zero; calling this when the counter is already zero has no effect.
     *
     * @param itemId id of the item whose counter to decrease, the same id the shop
     * uses to build the item's card (see [DemoContent.itemIds]).
     */
    fun decreaseQty(itemId: String) {
        val current = _state.value.quantities
        val newQty = (current[itemId] ?: 0) - 1
        if (newQty < 0) return
        val updated = current + (itemId to newQty)
        _state.value = _state.value.copy(quantities = updated)
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
     * Builds the state the app starts with out of the [PlayerState] the previous run left behind.
     *
     * @return The initial [GameUiState]: the player's game as it was saved, everything else fresh.
     */
    private fun restoredState(): GameUiState {
        val saved = store.load()
        return GameUiState(
            selection = saved.selection,
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
                subLocationIndex = current.subLocationIndex
            )
        )
    }
}
