package com.legacy.fingame.game

import androidx.lifecycle.ViewModel
import com.legacy.fingame.ui.DemoContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Screen { MAIN, SHOP, INVENTORY, QUESTS, LOCATIONS, OPTIONS }

data class GameUiState(
    val screen: Screen = Screen.MAIN,
    val selectedCategoryId: String = DemoContent.categoryIds.first(),
    val quantities: Map<String, Int> = emptyMap(),
    val subLocationIndex: Int = 0
)

// UI-only state holder: screen navigation, category selection, item counters
// and sub-location index. No domain logic (balance, cart, catalog) belongs here.
class GameViewModel : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    fun openScreen(screen: Screen) {
        _state.value = _state.value.copy(screen = screen)
    }

    fun closeScreen() {
        _state.value = _state.value.copy(screen = Screen.MAIN)
    }

    fun selectCategory(categoryId: String) {
        _state.value = _state.value.copy(selectedCategoryId = categoryId)
    }

    fun increaseQty(itemId: String) {
        val current = _state.value.quantities
        val updated = current + (itemId to (current[itemId] ?: 0) + 1)
        _state.value = _state.value.copy(quantities = updated)
    }

    fun decreaseQty(itemId: String) {
        val current = _state.value.quantities
        val newQty = (current[itemId] ?: 0) - 1
        if (newQty < 0) return
        val updated = current + (itemId to newQty)
        _state.value = _state.value.copy(quantities = updated)
    }

    fun nextSubLocation() {
        val count = DemoContent.subLocationCount
        if (count == 0) return
        val current = _state.value.subLocationIndex
        _state.value = _state.value.copy(subLocationIndex = (current + 1) % count)
    }

    fun prevSubLocation() {
        val count = DemoContent.subLocationCount
        if (count == 0) return
        val current = _state.value.subLocationIndex
        _state.value = _state.value.copy(subLocationIndex = (current - 1 + count) % count)
    }
}
