package com.legacy.fingame

import com.legacy.fingame.game.GameViewModel
import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.PlayerStateStore
import com.legacy.fingame.game.economy.GameClock
import com.legacy.fingame.game.items.Item
import com.legacy.fingame.game.items.ItemCatalog
import com.legacy.fingame.game.items.ItemCategory
import com.legacy.fingame.game.quests.Quest
import com.legacy.fingame.game.quests.QuestCatalog
import com.legacy.fingame.game.quests.QuestKind
import com.legacy.fingame.game.quests.QuestNode
import com.legacy.fingame.game.quests.QuestOption
import com.legacy.fingame.game.scene.GameLayer
import com.legacy.fingame.game.stats.StatKind
import kotlin.random.Random

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
 * item, one the pet feels three ways about, two items that are bought once — one of them in several
 * variants — and a decoration that stands in front of the pet instead of on it.
 */
internal object TestItems {

    /** Food in two variants: bought by the handful, and gives the variants something to pick from. */
    val APPLE = Item(
        id = "apple",
        name = "Яблоко",
        price = 15,
        category = ItemCategory.FOOD,
        variantIds = listOf("red", "green"),
        declaredEffects = mapOf(StatKind.HUNGER to 20, StatKind.HEALTH to 5)
    )

    /** Food with a single variant. */
    val FISH = Item(
        id = "fish",
        name = "Рыбка",
        price = 25,
        category = ItemCategory.FOOD,
        variantIds = listOf("default"),
        declaredEffects = mapOf(StatKind.HUNGER to 35)
    )

    /**
     * Clothes in two variants: owned per variant, so the black one is not the white one.
     *
     * Its data still declares an effect, the way a player's own data file may well still declare
     * one: an item the pet wears is worn to no effect all the same (see [Item.effects]).
     */
    val HAT = Item(
        id = "hat",
        name = "Шляпа",
        price = 100,
        category = ItemCategory.CLOTHES,
        variantIds = listOf("black", "white"),
        declaredEffects = mapOf(StatKind.PLEASURE to 10)
    )

    /** A toy with a single variant, bought once and played with over and over. */
    val BALL = Item(
        id = "ball",
        name = "Мячик",
        price = 60,
        category = ItemCategory.TOYS,
        variantIds = listOf("red"),
        declaredEffects = mapOf(StatKind.PLEASURE to 20, StatKind.HUNGER to -5)
    )

    /**
     * A decoration standing in front of the pet: its data names a layer of its own, and — like
     * [HAT] — an effect the pet does not feel, since a decoration is a look and nothing more.
     */
    val LAMP = Item(
        id = "lamp",
        name = "Лампа",
        price = 150,
        category = ItemCategory.DECOR,
        variantIds = listOf("default"),
        declaredEffects = mapOf(StatKind.PLEASURE to 10),
        layer = GameLayer.ENVIRONMENT_FRONT
    )

    /** Food the pet feels three ways about, for the shelf that has to make room for three chips. */
    val CAKE = Item(
        id = "cake",
        name = "Пирожное",
        price = 40,
        category = ItemCategory.FOOD,
        variantIds = listOf("default"),
        declaredEffects = mapOf(
            StatKind.HUNGER to 30,
            StatKind.PLEASURE to 15,
            StatKind.HEALTH to -5
        )
    )

    /** Every test item, in the order a catalog would list them. */
    val ALL = listOf(APPLE, FISH, CAKE, HAT, BALL, LAMP)
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
 * @param questCatalog which quests exist; none by default, so tests about something else never
 * meet a quest.
 * @param random dice for random quests; by default dice that fail the test the moment they are
 * rolled, so a test that does not script them proves they were never needed.
 * @return A view model backed by the given doubles.
 */
internal fun testGameViewModel(
    store: PlayerStateStore = FakePlayerStateStore(),
    catalog: ItemCatalog = FakeItemCatalog(),
    clock: GameClock = FakeGameClock(),
    questCatalog: QuestCatalog = QuestCatalog.EMPTY,
    random: Random = ScriptedRandom()
): GameViewModel = GameViewModel(
    store = store,
    catalog = catalog,
    clock = clock,
    questCatalog = questCatalog,
    random = random
)

/**
 * Квесты, на которых проверяются правила: квест игрока с прогрессом, минимумом и задержкой,
 * квест игрока без всего этого, квест, который стоит дороже, чем может быть у игрока, и два
 * случайных — без прогресса и с прогрессом и паузой.
 */
internal object TestQuests {

    /** Квест игрока с прогрессом: нужен запас 100 монет, первый шаг тратит 40 и ждёт минуту. */
    val PICNIC = Quest(
        id = "picnic",
        title = "Пикник",
        description = "Подготовь пикник для друзей.",
        kind = QuestKind.PLAYER,
        firstNodeId = "food",
        hasProgress = true,
        minBalance = 100,
        nodes = mapOf(
            "food" to QuestNode(
                id = "food",
                text = "Что взять из еды?",
                delayMinutes = 1,
                options = listOf(
                    QuestOption(
                        label = "Фрукты",
                        resultText = "Вкусно и полезно.",
                        nextNodeId = "games",
                        statEffects = mapOf(StatKind.HUNGER to 20, StatKind.HEALTH to 5),
                        moneyDelta = -40,
                        progressDelta = 60
                    ),
                    QuestOption(
                        label = "Ничего",
                        resultText = "Все голодные.",
                        nextNodeId = "games",
                        statEffects = mapOf(StatKind.HUNGER to -10)
                    )
                )
            ),
            "games" to QuestNode(
                id = "games",
                text = "Во что играем?",
                options = listOf(
                    QuestOption(
                        label = "Мяч",
                        resultText = "Играли до заката!",
                        nextNodeId = Quest.END_NODE,
                        statEffects = mapOf(StatKind.PLEASURE to 20),
                        progressDelta = 60
                    )
                )
            )
        )
    )

    /** Квест игрока без прогресса и задержек: один шаг, после него сразу конец и +30 монет. */
    val PIGGY_BANK = Quest(
        id = "piggy_bank",
        title = "Копилка",
        description = "Научись откладывать.",
        kind = QuestKind.PLAYER,
        firstNodeId = "start",
        nodes = mapOf(
            "start" to QuestNode(
                id = "start",
                text = "Положить монеты в копилку?",
                options = listOf(
                    QuestOption(
                        label = "В копилку",
                        resultText = "Терпение окупилось.",
                        nextNodeId = Quest.END_NODE,
                        moneyDelta = 30
                    )
                )
            )
        )
    )

    /** Квест игрока без минимума, но с тратой в 40 монет: проверяет, что счёт не уходит в минус. */
    val ICE_CREAM = Quest(
        id = "ice_cream",
        title = "Мороженое",
        description = "Купи мороженое.",
        kind = QuestKind.PLAYER,
        firstNodeId = "shop",
        nodes = mapOf(
            "shop" to QuestNode(
                id = "shop",
                text = "Какое мороженое взять?",
                options = listOf(
                    QuestOption(
                        label = "Большое",
                        resultText = "Вкусно!",
                        nextNodeId = Quest.END_NODE,
                        statEffects = mapOf(StatKind.PLEASURE to 10),
                        moneyDelta = -40
                    )
                )
            )
        )
    )

    /** Случайный квест без прогресса: один узел, два исхода. */
    val WALLET = Quest(
        id = "lost_wallet",
        title = "Потерянный кошелёк",
        description = "На дорожке лежит кошелёк.",
        kind = QuestKind.RANDOM,
        firstNodeId = "found",
        nodes = mapOf(
            "found" to QuestNode(
                id = "found",
                text = "Как поступить?",
                options = listOf(
                    QuestOption(
                        label = "Вернуть",
                        resultText = "Хозяин дал награду.",
                        nextNodeId = Quest.END_NODE,
                        statEffects = mapOf(StatKind.PLEASURE to 15),
                        moneyDelta = 10
                    ),
                    QuestOption(
                        label = "Оставить себе",
                        resultText = "На душе неспокойно.",
                        nextNodeId = Quest.END_NODE,
                        statEffects = mapOf(StatKind.PLEASURE to -15),
                        moneyDelta = 30
                    )
                )
            )
        )
    )

    /** Случайный квест с прогрессом и паузой в две минуты между шагами. */
    val GUESTS = Quest(
        id = "guests",
        title = "Гости",
        description = "Скоро придут гости.",
        kind = QuestKind.RANDOM,
        firstNodeId = "treat",
        hasProgress = true,
        nodes = mapOf(
            "treat" to QuestNode(
                id = "treat",
                text = "Чем угостить?",
                delayMinutes = 2,
                options = listOf(
                    QuestOption(
                        label = "Пирог",
                        resultText = "Пирог удался.",
                        nextNodeId = "tidy",
                        progressDelta = 50
                    )
                )
            ),
            "tidy" to QuestNode(
                id = "tidy",
                text = "Прибраться?",
                options = listOf(
                    QuestOption(
                        label = "Прибраться",
                        resultText = "Уютно!",
                        nextNodeId = Quest.END_NODE,
                        progressDelta = 50
                    )
                )
            )
        )
    )

    /** Все тестовые квесты в порядке каталога. */
    val ALL = listOf(PICNIC, PIGGY_BANK, ICE_CREAM, WALLET, GUESTS)

    /** Каталог из [ALL]. */
    val CATALOG: QuestCatalog = QuestCatalog.of(ALL)
}

/**
 * [Random], который отвечает на `nextInt(until)` заранее заданными числами по порядку, а на всё
 * остальное — ошибкой. Без чисел он падает при первом же броске, так что тест, в котором
 * случайность не должна спрашиваться, это и проверяет.
 *
 * @param picks ответы на `nextInt(until)` по порядку; каждый должен быть в `0 until until`.
 */
internal class ScriptedRandom(vararg picks: Int) : Random() {

    private val remaining = ArrayDeque(picks.toList())

    /** С какими `until` спрашивали бросок, по порядку. */
    val bounds = mutableListOf<Int>()

    override fun nextBits(bitCount: Int): Int =
        error("ScriptedRandom only answers nextInt(until)")

    override fun nextInt(until: Int): Int {
        bounds += until
        val pick = remaining.removeFirstOrNull() ?: error("ScriptedRandom ran out of picks")
        require(pick in 0 until until) { "Pick $pick is not in 0 until $until" }
        return pick
    }
}
