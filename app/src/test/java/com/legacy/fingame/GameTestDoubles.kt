package com.legacy.fingame

import com.legacy.fingame.game.GameViewModel
import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.PlayerStateStore
import com.legacy.fingame.game.economy.GameClock
import com.legacy.fingame.game.items.Item
import com.legacy.fingame.game.items.ItemCatalog
import com.legacy.fingame.game.items.ItemCategory
import com.legacy.fingame.game.scene.GameLayer
import com.legacy.fingame.game.stats.StatKind

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
 * item, two items that are bought once — one of them in several variants — and a decoration that
 * stands behind the pet instead of on it.
 */
internal object TestItems {

    /** Food in two variants: bought by the handful, and gives the variants something to pick from. */
    val APPLE = Item(
        id = "apple",
        name = "Яблоко",
        price = 15,
        category = ItemCategory.FOOD,
        variantIds = listOf("red", "green"),
        effects = mapOf(StatKind.HUNGER to 20, StatKind.HEALTH to 5)
    )

    /** Food with a single variant. */
    val FISH = Item(
        id = "fish",
        name = "Рыбка",
        price = 25,
        category = ItemCategory.FOOD,
        variantIds = listOf("default"),
        effects = mapOf(StatKind.HUNGER to 35)
    )

    /** Clothes in two variants: owned per variant, so the black one is not the white one. */
    val HAT = Item(
        id = "hat",
        name = "Шляпа",
        price = 100,
        category = ItemCategory.CLOTHES,
        variantIds = listOf("black", "white"),
        effects = mapOf(StatKind.PLEASURE to 10)
    )

    /** A toy with a single variant, bought once and played with over and over. */
    val BALL = Item(
        id = "ball",
        name = "Мячик",
        price = 60,
        category = ItemCategory.TOYS,
        variantIds = listOf("red"),
        effects = mapOf(StatKind.PLEASURE to 20, StatKind.HUNGER to -5)
    )

    /** A decoration standing in front of the pet: its data names a layer of its own. */
    val LAMP = Item(
        id = "lamp",
        name = "Лампа",
        price = 150,
        category = ItemCategory.DECOR,
        variantIds = listOf("default"),
        effects = mapOf(StatKind.PLEASURE to 10),
        layer = GameLayer.ENVIRONMENT_FRONT
    )

    /** Every test item, in the order a catalog would list them. */
    val ALL = listOf(APPLE, FISH, HAT, BALL, LAMP)
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
 * [GameClock] the test moves by hand, so days can pass without waiting for midnight and the pet can
 * be left alone for hours within a single test.
 *
 * The day and the moment are moved apart on purpose: a test about the daily bonus has no business
 * working out timestamps, and a test about the pet getting hungry has none working out calendars.
 *
 * @property day the day the game currently sees, as days since the epoch.
 * @property millis the moment the game currently sees, in milliseconds.
 */
internal class FakeGameClock(
    var day: Long = DEFAULT_DAY,
    var millis: Long = DEFAULT_MILLIS
) : GameClock {

    companion object {
        /** An arbitrary day the tests start on. */
        const val DEFAULT_DAY = 19_000L

        /** An arbitrary moment the tests start at. */
        const val DEFAULT_MILLIS = 1_700_000_000_000L
    }

    override fun today(): Long = day

    override fun nowMillis(): Long = millis
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
