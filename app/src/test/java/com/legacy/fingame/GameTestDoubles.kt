package com.legacy.fingame

import com.legacy.fingame.game.GameViewModel
import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.PlayerStateStore
import com.legacy.fingame.game.economy.GameClock
import com.legacy.fingame.game.items.Item
import com.legacy.fingame.game.items.ItemCatalog
import com.legacy.fingame.game.items.ItemCategory

/**
 * [PlayerStateStore] that keeps the state in memory instead of in SharedPreferences, so the
 * restoring and the saving can be checked without an Android device. A store built with a state
 * stands for an app that was already played and closed.
 *
 * @property state the state the store currently holds, i.e. what the next launch would restore.
 */
internal class FakePlayerStateStore(var state: PlayerState = PlayerState()) : PlayerStateStore {

    override fun load(): PlayerState = state

    override fun save(state: PlayerState) {
        this.state = state
    }
}

/**
 * The items the tests go shopping with: one cheap food item offered in two variants, one plain food
 * item, and two items that are bought once — one of them in several variants.
 */
internal object TestItems {

    /** Food in two variants: bought by the handful, and gives the variants something to pick from. */
    val APPLE = Item(
        id = "apple",
        title = "Яблоко",
        price = 15,
        category = ItemCategory.FOOD,
        variants = mapOf("red" to "items/apple/red", "green" to "items/apple/green")
    )

    /** Food with a single variant. */
    val FISH = Item(
        id = "fish",
        title = "Рыбка",
        price = 25,
        category = ItemCategory.FOOD,
        variants = mapOf("default" to "items/fish/default")
    )

    /** Clothes in two variants: owned per variant, so the black one is not the white one. */
    val HAT = Item(
        id = "hat",
        title = "Шляпа",
        price = 100,
        category = ItemCategory.CLOTHES,
        variants = mapOf("black" to "items/hat/black", "white" to "items/hat/white")
    )

    /** A toy with a single variant, bought once. */
    val BALL = Item(
        id = "ball",
        title = "Мячик",
        price = 60,
        category = ItemCategory.TOYS,
        variants = mapOf("red" to "items/ball/red")
    )

    /** Every test item, in the order a catalog would list them. */
    val ALL = listOf(APPLE, FISH, HAT, BALL)
}

/**
 * [ItemCatalog] over a fixed list of items, standing in for the registry that reads them from the
 * asset data files.
 *
 * @param items what is on sale.
 */
internal class FakeItemCatalog(private val items: List<Item> = TestItems.ALL) : ItemCatalog {

    override fun getItemsByCategory(category: ItemCategory): List<Item> =
        items.filter { it.category == category }

    override fun findItemById(itemId: String): Item? = items.find { it.id == itemId }
}

/**
 * [GameClock] the test moves by hand, so days can pass without waiting for midnight.
 *
 * @property day the day the game currently sees, as days since the epoch.
 */
internal class FakeGameClock(var day: Long = DEFAULT_DAY) : GameClock {

    companion object {
        /** An arbitrary day the tests start on. */
        const val DEFAULT_DAY = 19_000L
    }

    override fun today(): Long = day
}

/**
 * Builds a [GameViewModel] over test doubles, so a test only names the parts it cares about.
 *
 * @param store where the player's state is restored from and saved to.
 * @param catalog what is on sale.
 * @param clock which day the game is played on.
 * @return A view model backed by the given doubles.
 */
internal fun testGameViewModel(
    store: PlayerStateStore = FakePlayerStateStore(),
    catalog: ItemCatalog = FakeItemCatalog(),
    clock: GameClock = FakeGameClock()
): GameViewModel = GameViewModel(store = store, catalog = catalog, clock = clock)
