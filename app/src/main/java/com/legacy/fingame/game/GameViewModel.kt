package com.legacy.fingame.game

import androidx.lifecycle.ViewModel
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
 * @property screen the currently displayed [Screen].
 * @property selectedCategoryId id of the currently selected shop category, one of
 * [DemoContent.categoryIds]. Defaults to the first available category.
 * @property quantities per-item counters keyed by item id (see [DemoContent.itemIds]).
 * This is UI-only state (e.g. quantity pickers in the shop) and is not related to an
 * actual purchase/cart.
 * @property subLocationIndex index of the currently displayed sub-location within
 * [DemoContent.subLocationTitles].
 */
data class GameUiState(
    val screen: Screen = Screen.MAIN,
    val selectedCategoryId: String = DemoContent.categoryIds.first(),
    val quantities: Map<String, Int> = emptyMap(),
    val subLocationIndex: Int = 0
)

/**
 * UI-only state holder for the game screen: screen navigation, category selection,
 * item counters and sub-location index. Holds no domain logic (balance, cart, catalog).
 */
class GameViewModel : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())

    /** Current [GameUiState], observed by the UI. */
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    /**
     * Switches the currently displayed screen.
     *
     * To return to the main screen, prefer [closeScreen] instead of passing [Screen.MAIN] here.
     *
     * @param screen the screen to navigate to.
     */
    fun openScreen(screen: Screen) {
        _state.value = _state.value.copy(screen = screen)
    }

    /**
     * Returns the player to the main screen, equivalent to `openScreen(`[Screen.MAIN]`)`.
     */
    fun closeScreen() {
        _state.value = _state.value.copy(screen = Screen.MAIN)
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
     * Increments the quantity counter for the given item by one.
     *
     * @param itemId id of the item whose counter to increase, the same id the shop
     * uses to build the item's card (see [DemoContent.itemIds]).
     */
    fun increaseQty(itemId: String) {
        val current = _state.value.quantities
        val updated = current + (itemId to (current[itemId] ?: 0) + 1)
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
     * after the last one.
     */
    fun nextSubLocation() {
        val count = DemoContent.subLocationCount
        if (count == 0) return
        val current = _state.value.subLocationIndex
        _state.value = _state.value.copy(subLocationIndex = (current + 1) % count)
    }

    /**
     * Goes back to the previous sub-location, wrapping around to the last
     * sub-location when moving before the first one.
     */
    fun prevSubLocation() {
        val count = DemoContent.subLocationCount
        if (count == 0) return
        val current = _state.value.subLocationIndex
        _state.value = _state.value.copy(subLocationIndex = (current - 1 + count) % count)
    }
}
