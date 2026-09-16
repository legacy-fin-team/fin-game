package com.legacy.fingame.game

import androidx.lifecycle.ViewModel
import com.legacy.fingame.domain.Goal
import com.legacy.fingame.domain.ItemCategory
import com.legacy.fingame.domain.Locations
import com.legacy.fingame.domain.PurchaseKind
import com.legacy.fingame.domain.ShopCatalog
import com.legacy.fingame.domain.ShopItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Товар в UI магазина: сколько добавлено в корзину, куплен ли (UNIQUE), выбран ли целью. */
data class ShopItemUi(
    val item: ShopItem,
    val quantity: Int,
    val owned: Boolean,
    val isGoal: Boolean
)

data class GameUiState(
    val balance: Int = 1200,
    val goal: Goal? = null,
    val goalProgress: Float = 0f,
    val selectedCategory: ItemCategory = ItemCategory.FOOD,
    val shopItems: List<ShopItemUi> = emptyList(),
    val cartTotal: Int = 0,
    val canCheckout: Boolean = false,
    val inventory: Map<String, Int> = emptyMap(),
    val ownedItemIds: Set<String> = emptySet(),
    val subLocationIndex: Int = 0,
    val subLocations: List<String> = emptyList(),
    val petName: String = "Барсик",
    val petEmoji: String = "🐱",
    val toast: String? = null
)

class GameViewModel : ViewModel() {

    // Корзина не входит в публичный контракт GameUiState (там только итог cartTotal),
    // поэтому храним её отдельно и пересчитываем производные поля синхронно при каждом изменении.
    private val cartQuantities = mutableMapOf<String, Int>() // для CONSUMABLE
    private val cartUniqueIds = mutableSetOf<String>()       // для UNIQUE, ещё не купленных

    private val _state = MutableStateFlow(
        GameUiState(
            selectedCategory = ItemCategory.FOOD,
            shopItems = buildShopItems(ItemCategory.FOOD, emptyMap(), emptySet(), null),
            subLocations = Locations.subLocations
        )
    )
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    fun selectCategory(category: ItemCategory) {
        _state.value = _state.value.copy(selectedCategory = category)
        recompute()
    }

    fun increaseQty(itemId: String) {
        val item = ShopCatalog.items.find { it.id == itemId } ?: return
        if (item.kind != PurchaseKind.CONSUMABLE) return
        cartQuantities[itemId] = (cartQuantities[itemId] ?: 0) + 1
        recompute()
    }

    fun decreaseQty(itemId: String) {
        val current = cartQuantities[itemId] ?: return
        if (current <= 1) cartQuantities.remove(itemId) else cartQuantities[itemId] = current - 1
        recompute()
    }

    fun toggleUnique(itemId: String) {
        val item = ShopCatalog.items.find { it.id == itemId } ?: return
        if (item.kind != PurchaseKind.UNIQUE) return
        if (_state.value.ownedItemIds.contains(itemId)) return // уже куплен — трогать нельзя
        if (!cartUniqueIds.add(itemId)) cartUniqueIds.remove(itemId)
        recompute()
    }

    fun toggleGoal(itemId: String) {
        val current = _state.value
        val newGoal = if (current.goal?.itemId == itemId) {
            null
        } else {
            val item = ShopCatalog.items.find { it.id == itemId } ?: return
            Goal(itemId = item.id, title = item.title, targetPrice = item.price, emoji = item.emoji)
        }
        _state.value = current.copy(goal = newGoal)
        recompute()
    }

    fun checkout() {
        val current = _state.value
        val total = calcCartTotal()
        if (!(total > 0 && total <= current.balance)) return

        val newInventory = current.inventory.toMutableMap()
        cartQuantities.forEach { (id, qty) -> newInventory[id] = (newInventory[id] ?: 0) + qty }
        val newOwned = current.ownedItemIds + cartUniqueIds

        cartQuantities.clear()
        cartUniqueIds.clear()

        _state.value = current.copy(
            balance = current.balance - total,
            inventory = newInventory,
            ownedItemIds = newOwned,
            toast = "Куплено на $total ₽"
        )
        recompute()
    }

    fun nextSubLocation() {
        val current = _state.value
        val size = current.subLocations.size
        if (size == 0) return
        _state.value = current.copy(subLocationIndex = (current.subLocationIndex + 1) % size)
    }

    fun prevSubLocation() {
        val current = _state.value
        val size = current.subLocations.size
        if (size == 0) return
        _state.value = current.copy(subLocationIndex = (current.subLocationIndex - 1 + size) % size)
    }

    fun consumeToast() {
        _state.value = _state.value.copy(toast = null)
    }

    // --- вспомогательные функции пересчёта производных полей ---

    private fun recompute() {
        val current = _state.value
        val total = calcCartTotal()
        _state.value = current.copy(
            shopItems = buildShopItems(current.selectedCategory, current.inventory, current.ownedItemIds, current.goal),
            cartTotal = total,
            canCheckout = total > 0 && total <= current.balance,
            goalProgress = current.goal?.let { (current.balance.toFloat() / it.targetPrice).coerceIn(0f, 1f) } ?: 0f
        )
    }

    private fun calcCartTotal(): Int = ShopCatalog.items.sumOf { item ->
        when (item.kind) {
            PurchaseKind.CONSUMABLE -> (cartQuantities[item.id] ?: 0) * item.price
            PurchaseKind.UNIQUE -> if (cartUniqueIds.contains(item.id)) item.price else 0
        }
    }

    private fun buildShopItems(
        category: ItemCategory,
        inventory: Map<String, Int>,
        ownedItemIds: Set<String>,
        goal: Goal?
    ): List<ShopItemUi> = ShopCatalog.items
        .filter { it.category == category }
        .map { item ->
            val quantity = when (item.kind) {
                PurchaseKind.CONSUMABLE -> cartQuantities[item.id] ?: 0
                PurchaseKind.UNIQUE -> if (cartUniqueIds.contains(item.id)) 1 else 0
            }
            ShopItemUi(
                item = item,
                quantity = quantity,
                owned = ownedItemIds.contains(item.id),
                isGoal = goal?.itemId == item.id
            )
        }
}
