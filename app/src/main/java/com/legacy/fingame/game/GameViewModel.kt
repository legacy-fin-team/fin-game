package com.legacy.fingame.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.legacy.fingame.DemoMode
import com.legacy.fingame.game.animals.Animal
import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.game.animals.Growth
import com.legacy.fingame.game.economy.Budget
import com.legacy.fingame.game.economy.BudgetDraft
import com.legacy.fingame.game.economy.BudgetResult
import com.legacy.fingame.game.economy.BudgetHistory
import com.legacy.fingame.game.economy.BudgetState
import com.legacy.fingame.game.economy.Deposit
import com.legacy.fingame.game.economy.Economy
import com.legacy.fingame.game.economy.FastForwardClock
import com.legacy.fingame.game.economy.GameClock
import com.legacy.fingame.game.economy.MoneyEntry
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.game.economy.SpendKind
import com.legacy.fingame.game.items.Item
import com.legacy.fingame.game.items.ItemCatalog
import com.legacy.fingame.game.items.ItemCategory
import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.game.items.ItemUse
import com.legacy.fingame.game.quests.Quest
import com.legacy.fingame.game.quests.QuestCatalog
import com.legacy.fingame.game.quests.QuestChoice
import com.legacy.fingame.game.quests.QuestEngine
import com.legacy.fingame.game.quests.QuestKind
import com.legacy.fingame.game.quests.QuestLog
import com.legacy.fingame.game.quests.QuestOutcome
import com.legacy.fingame.game.quests.QuestProgress
import com.legacy.fingame.game.settings.GameSettings
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.ui.DemoContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

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
    /** Экран планирования бюджета: итог прошлого периода и раскладка на следующий. */
    BUDGET,
    /** Экран журнала: все изменения денег игрока, новейшие сверху. */
    LOG,
    /** The options/settings screen. */
    OPTIONS,
    /** Замок перед режимом взрослого: три примера на умножение. */
    ADULT_LOCK,
    /** Режим взрослого: история дней, покупок и квестов, инвентарь, цели и журнал ребёнка. */
    ADULT_MODE
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
 * @property deposit the deposit currently open, or null when there isn't one, restored from
 * [PlayerState.deposit].
 * @property budget the confirmed budget of the running period, or null when no period has started
 * yet, restored from [PlayerState.budget].
 * @property previousBudgetResult the result of the period last closed, shown while planning the
 * next one, restored from [PlayerState.previousBudgetResult].
 * @property budgetDraft the layout the player started on the planning screen but has not confirmed
 * yet, or null when they haven't touched it, restored from [PlayerState.budgetDraft].
 * @property planningOpen whether budget planning is open right now, restored from
 * [PlayerState.planningOpen]; becomes true when the daily bonus is claimed and false once a budget
 * is confirmed. See [canPlanBudget] for whether the planning screen may actually be opened.
 * @property moneyLog the player's money log, newest first, restored from [PlayerState.moneyLog].
 * @property budgetHistory итоги закрытых периодов, новейший первым, из [PlayerState.budgetHistory];
 * пополняется в [GameViewModel.claimDailyBonus].
 * @property questLog выборы игрока в квестах, новейший первым, из [PlayerState.questLog];
 * пополняется в [GameViewModel.chooseQuestOption].
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
 * @property goals цели игрока — товары в варианте, отмеченные звёздочкой в магазине, в том
 * порядке, в каком он их отмечал; восстанавливаются из [PlayerState.goals]. Купленное из них
 * уходит само (см. [GameViewModel.buyCart]).
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
 * @property todayDay the game day it is right now, on the same scale as [Deposit.maturityDay]: days
 * since the epoch, plus whatever a demo skipped. The screens turn it into the day number the player
 * reads (see [com.legacy.fingame.ui.screens.dayNumberOf]) rather than showing it as it is.
 * @property quests где игрок в каждом квесте, восстановлено из [PlayerState.quests]; квесты, которых
 * больше нет в данных, отброшены, а квест, ждавший выбора на пропавшем из данных шаге, завершён.
 * @property questsSeenAtMillis когда игрок последний раз видел экран квестов, из
 * [PlayerState.questsSeenAtMillis].
 * @property lastRandomQuestAtMillis когда выпал последний случайный квест, из
 * [PlayerState.lastRandomQuestAtMillis].
 * @property hasUnseenQuestStep горит ли точка на кнопке квестов: есть шаг, ставший доступным после
 * последнего взгляда на экран (см. [QuestEngine.hasUnseenStep]). Пересчитывается на каждом
 * [GameViewModel.tick], не сохраняется.
 * @property adultMode открыт ли режим взрослого: пока он включён, модель не пускает ни покупки, ни
 * предметы, ни квесты, ни бонус, ни бюджет, ни цели. Не сохраняется — после перезапуска игра снова
 * у ребёнка.
 */
data class GameUiState(
    val screen: Screen = Screen.MAIN,
    val selection: AnimalSelection? = null,
    val petName: String = "",
    val balance: Int = Economy.STARTING_BALANCE,
    val deposit: Deposit? = null,
    val budget: BudgetState? = null,
    val previousBudgetResult: BudgetResult? = null,
    val budgetDraft: BudgetDraft? = null,
    val planningOpen: Boolean = false,
    val moneyLog: MoneyLog = MoneyLog.EMPTY,
    val budgetHistory: List<BudgetResult> = emptyList(),
    val questLog: QuestLog = QuestLog.EMPTY,
    val dailyBonusAvailable: Boolean = false,
    val lastDailyBonusDay: Long = Economy.NEVER_CLAIMED,
    val selectedCategory: ItemCategory = ItemCategory.entries.first(),
    val quantities: Map<String, Int> = emptyMap(),
    val pickedVariants: Map<String, String> = emptyMap(),
    val cartPrice: Int = 0,
    val owned: Map<ItemSelection, Int> = emptyMap(),
    val worn: Set<ItemSelection> = emptySet(),
    val goals: List<ItemSelection> = emptyList(),
    val stats: PetStats = PetStats.FULL,
    val statsUpdatedAtMillis: Long = PlayerState.NEVER_UPDATED,
    val petAge: Int = Animal.FIRST_AGE,
    val petBornAtMillis: Long = Growth.NOT_BORN,
    val subLocationIndex: Int = 0,
    val todayDay: Long = 0L,
    val quests: List<QuestProgress> = emptyList(),
    val questsSeenAtMillis: Long = PlayerState.QUESTS_NEVER_SEEN,
    val lastRandomQuestAtMillis: Long = PlayerState.NO_RANDOM_QUEST,
    val hasUnseenQuestStep: Boolean = false,
    val adultMode: Boolean = false,
    val settings: GameSettings = GameSettings()
) {
    /**
     * @param questId id квеста.
     * @return Где игрок в этом квесте, или null, когда квест не начинался.
     */
    fun questProgressOf(questId: String): QuestProgress? = quests.find { it.questId == questId }

    /**
     * Whether the player picked anything at all, i.e. whether there is a purchase to ask about.
     * Says nothing about the money: a cart the player cannot afford is still a cart.
     */
    val hasCart: Boolean get() = cartPrice > 0

    /**
     * How many coins the cart costs over what the player has, i.e. by how much the purchase
     * overshoots the balance. Zero whenever the cart can be paid for, so this is the one number the
     * "not enough money" window has to show.
     */
    val cartShortfall: Int get() = (cartPrice - balance).coerceAtLeast(0)

    /** Whether the balance covers the cart. A cart costing exactly the balance is covered. */
    val canAffordCart: Boolean get() = cartShortfall == 0

    /** Whether the cart holds something the player can actually pay for. */
    val canBuyCart: Boolean get() = hasCart && canAffordCart

    /** Сколько денег игрок может разложить: всё, что на текущем счёте. Тело вклада сюда не входит. */
    val totalToPlan: Int get() = balance

    /** Тело открытого вклада, без процентов; ноль, когда вклада нет. */
    val depositAmount: Int get() = deposit?.amount ?: 0

    /** Раскладка, которую показывает экран планирования: начатая игроком или начальная. */
    val planningDraft: BudgetDraft get() = budgetDraft ?: Budget.startingDraft()

    /**
     * Планирование доступно после получения бонуса дня, а также всегда, когда подтверждённого
     * бюджета нет: без него экрану бюджета всё равно нечего показать, а игроку — нечего ждать.
     * Сюда попадают и первый запуск, и старые сохранения, в которых планирование не открывалось.
     *
     * Это же условие пропускает правки раскладки ([GameViewModel.updateBudgetDraft]) и её
     * подтверждение ([GameViewModel.confirmBudget]), так что показанная раскладка всегда работает.
     */
    val canPlanBudget: Boolean get() = planningOpen || budget == null

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

    /**
     * @param item товар на полке магазина.
     * @return Отмечен ли звёздочкой именно тот вариант [item], который сейчас выбран: белая шляпа
     * может быть целью, а чёрная — нет.
     */
    fun isGoal(item: Item): Boolean = ItemSelection(item.id, pickedVariantOf(item)) in goals
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
 * @param questCatalog какие квесты есть в игре; правила над ними — в [QuestEngine].
 * @param random кости для случайных квестов; в тестах — заранее заданные.
 * @param allowRestart можно ли пройти пройденный квест ещё раз ([restartQuest]); только в
 * демо-сборке, иначе монеты «Копилки» можно было бы собирать без конца.
 * @param settings the settings the app starts with, as they were saved: they are in the state from
 * its very first value, so nothing that follows the state — the music, the click sound, the theme —
 * ever sees the defaults for a frame.
 */
class GameViewModel(
    private val store: PlayerStateStore,
    private val catalog: ItemCatalog,
    clock: GameClock = GameClock.DEVICE,
    private val questCatalog: QuestCatalog = QuestCatalog.EMPTY,
    private val random: Random = Random.Default,
    private val allowRestart: Boolean = DemoMode.ENABLED,
    settings: GameSettings = GameSettings()
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
         * @param questCatalog какие квесты есть в игре.
         * @param allowRestart можно ли проходить квесты ещё раз; по умолчанию — только в демо.
         * @param settings the saved settings the app starts with.
         * @return A factory creating a [GameViewModel] backed by [store] and [catalog].
         */
        fun factory(
            store: PlayerStateStore,
            catalog: ItemCatalog,
            clock: GameClock = GameClock.DEVICE,
            questCatalog: QuestCatalog = QuestCatalog.EMPTY,
            allowRestart: Boolean = DemoMode.ENABLED,
            settings: GameSettings = GameSettings()
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                GameViewModel(
                    store,
                    catalog,
                    clock,
                    questCatalog,
                    allowRestart = allowRestart,
                    settings = settings
                )
            }
        }
    }

    private val _state = MutableStateFlow(restoredState(settings))

    /** Current [GameUiState], observed by the UI. */
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    init {
        // Вклад мог дожить до срока, пока приложение было закрыто: игрок должен найти деньги на
        // счету, а не вклад, который «уже должен был закрыться».
        settleMaturedDeposit()
    }

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
        settleMaturedDeposit()
        val seesQuests = screen == Screen.QUESTS || _state.value.screen == Screen.QUESTS
        _state.value = stateForNavigatingTo(screen)
        // Открыть экран квестов или уйти с него — это «игрок всё увидел»: момент запоминается
        // сразу, чтобы точка на кнопке не загорелась снова после перезапуска.
        if (seesQuests) persist()
    }

    /**
     * Returns the player to the main screen, equivalent to `openScreen(`[Screen.MAIN]`)`.
     * If the player was on [Screen.SHOP], the unpaid shop cart is dropped.
     */
    fun closeScreen() {
        when (_state.value.screen) {
            Screen.ADULT_MODE -> exitAdultMode()
            Screen.ADULT_LOCK -> openScreen(Screen.OPTIONS)
            else -> openScreen(Screen.MAIN)
        }
    }

    /** Замок решён: открывается режим взрослого, и игра ребёнка становится только для просмотра. */
    fun enterAdultMode() {
        _state.value = _state.value.copy(adultMode = true)
        openScreen(Screen.ADULT_MODE)
    }

    /** Взрослый закрыл свой режим: обратно в настройки, игра снова у ребёнка. */
    fun exitAdultMode() {
        _state.value = _state.value.copy(adultMode = false)
        openScreen(Screen.OPTIONS)
    }

    /** Режим взрослого включён: действия, меняющие игру ребёнка, ничего не делают. */
    private val readOnly: Boolean get() = _state.value.adultMode

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
        val openingQuests = screen == Screen.QUESTS
        // Уходя с экрана квестов, игрок видел всё, что на нём было, — и шаг, открывшийся без тика.
        val leavingQuests = previous.screen == Screen.QUESTS
        return previous.copy(
            questsSeenAtMillis = if (openingQuests || leavingQuests) {
                clock.nowMillis()
            } else {
                previous.questsSeenAtMillis
            },
            hasUnseenQuestStep = !openingQuests && !leavingQuests && previous.hasUnseenQuestStep,
            screen = screen,
            adultMode = previous.adultMode && screen == Screen.ADULT_MODE,
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
     *
     * Здесь же проверяется случайный квест ([QuestEngine.maybeSpawnRandom]) и пересчитывается
     * точка на кнопке квестов.
     */
    fun tick() {
        settleMaturedDeposit()

        val current = _state.value
        val now = clock.nowMillis()
        val ticks = PetStats.ticksBetween(current.statsUpdatedAtMillis, now)
        val age = Growth.ageAt(current.petBornAtMillis, now)
        val lived = if (ticks == 0L && age == current.petAge) {
            current
        } else {
            current.copy(
                stats = current.stats.decayedBy(ticks),
                statsUpdatedAtMillis = current.statsUpdatedAtMillis + ticks * PetStats.TICK_MILLIS,
                petAge = age,
                todayDay = clock.today()
            )
        }
        // Квесты живут по тем же часам: может выпасть случайный, может кончиться задержка шага.
        val next = lived.withQuestsCaughtUp(now)
        if (next == current) return

        _state.value = next
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
     * pet falls through. The skipped time outlives the app being closed as well (see
     * [PlayerState.gameNowMillis]): a pet shown grown up in a demo is still grown up when the app is
     * opened again, and goes on living from the moment the demo left it at.
     *
     * @param millis how much time to skip; zero or less skips nothing.
     */
    fun fastForward(millis: Long) {
        if (millis <= 0) return
        clock.fastForward(millis)
        settleMaturedDeposit()

        val current = _state.value
        _state.value = current.copy(
            dailyBonusAvailable = Economy.isDailyBonusAvailable(
                lastClaimedDay = current.lastDailyBonusDay,
                today = clock.today()
            ),
            todayDay = clock.today()
        )
        tick()
        // The skipped time is remembered even when it moved neither a bar nor a stage, since the
        // next launch starts from the moment it was skipped to and not from the one before it.
        persist()
    }

    /**
     * Игрок берёт квест кнопкой «Взять». Только квест игрока, который ещё не начинался, и только
     * когда на счёте его минимум; минимум не тратится.
     *
     * @param questId id квеста.
     * @return True, когда квест начат.
     */
    fun startQuest(questId: String): Boolean {
        if (readOnly) return false
        val quest = questCatalog.findQuestById(questId) ?: return false
        if (quest.kind != QuestKind.PLAYER) return false
        if (_state.value.questProgressOf(questId) != null) return false
        return beginQuest(quest)
    }

    /**
     * «Ещё раз» для пройденного квеста игрока: он начинается заново с первого узла и нулевого
     * прогресса. Минимум на счёте нужен и здесь. Только в демо-сборке (см. `allowRestart`).
     *
     * @param questId id квеста.
     * @return True, когда квест начат заново.
     */
    fun restartQuest(questId: String): Boolean {
        if (readOnly) return false
        if (!allowRestart) return false
        val quest = questCatalog.findQuestById(questId) ?: return false
        if (quest.kind != QuestKind.PLAYER) return false
        if (_state.value.questProgressOf(questId)?.isFinished != true) return false
        return beginQuest(quest)
    }

    /**
     * Выбор варианта на текущем шаге квеста: полоски питомца и деньги меняются сразу, деньги —
     * через журнал с причиной «Квест: <название>», в бюджет периода это не идёт.
     *
     * @param questId id квеста.
     * @param optionIndex номер варианта на шаге.
     * @return True, когда выбор сделан; false, когда выбирать сейчас нечего (см. [QuestEngine.choose]).
     */
    fun chooseQuestOption(questId: String, optionIndex: Int): Boolean {
        if (readOnly) return false
        val quest = questCatalog.findQuestById(questId) ?: return false
        val current = _state.value
        val now = clock.nowMillis()
        val nodeId = current.questProgressOf(questId)?.nodeId ?: return false
        val choice = QuestEngine.choose(quest, current.quests, optionIndex, current.balance, now)
            ?: return false
        val logged = QuestChoice(
            questId = questId,
            nodeId = nodeId,
            optionLabel = choice.outcome.optionLabel,
            moneyDelta = choice.outcome.moneyDelta,
            progressDelta = choice.outcome.progressDelta,
            gameDay = clock.today(),
            timestampMillis = now
        )

        _state.value = current.copy(quests = choice.quests, questLog = current.questLog.plus(logged))
            .applyQuestEffects(quest, choice.outcome)
            .withQuestsLookedAt(now)
        persist()
        return true
    }

    /**
     * «Дальше» или «Завершить» после результата выбора — когда задержка шага прошла.
     *
     * @param questId id квеста.
     * @return True, когда игрок перешёл на следующий шаг или квест завершён.
     */
    fun advanceQuest(questId: String): Boolean {
        if (readOnly) return false
        val quest = questCatalog.findQuestById(questId) ?: return false
        val current = _state.value
        val now = clock.nowMillis()
        val quests = QuestEngine.advance(quest, current.quests, now) ?: return false

        _state.value = current.copy(quests = quests).withQuestsLookedAt(now)
        persist()
        return true
    }

    /**
     * @return Момент по часам игры — с перемоткой демо-сборки. По нему экран квестов считает
     * «Следующий шаг через …», по нему же модель решает, можно ли идти дальше.
     */
    fun nowMillis(): Long = clock.nowMillis()

    /**
     * @return True, когда квест начат; см. [QuestEngine.start].
     */
    private fun beginQuest(quest: Quest): Boolean {
        val current = _state.value
        val now = clock.nowMillis()
        val quests = QuestEngine.start(quest, current.quests, current.balance, now) ?: return false

        _state.value = current.copy(quests = quests).withQuestsLookedAt(now)
        persist()
        return true
    }

    /**
     * Применяет исход выбора: полоски питомца двигаются в своих рамках, деньги идут через журнал.
     * Сумма исхода уже урезана до баланса ([QuestEngine.choose]), так что счёт не уходит в минус.
     */
    private fun GameUiState.applyQuestEffects(quest: Quest, outcome: QuestOutcome): GameUiState {
        val changed = copy(stats = stats.changedBy(outcome.statEffects))
        if (outcome.moneyDelta == 0) return changed
        return changed
            .copy(balance = (changed.balance + outcome.moneyDelta).coerceAtLeast(0))
            .logged(MoneyLog.questReason(quest.title), outcome.moneyDelta)
    }

    /** Игрок действует на экране квестов — значит, всё на нём видел. */
    private fun GameUiState.withQuestsLookedAt(now: Long): GameUiState =
        copy(questsSeenAtMillis = now, hasUnseenQuestStep = false)

    /**
     * Квесты догоняют часы: может выпасть случайный квест, а точка на кнопке загорается, когда
     * есть непросмотренный шаг. Пока открыт экран квестов, новый шаг сразу считается увиденным.
     */
    private fun GameUiState.withQuestsCaughtUp(now: Long): GameUiState {
        val spawned = withRandomQuestSpawned(now)
        val unseen = QuestEngine.hasUnseenStep(spawned.quests, spawned.questsSeenAtMillis, now)
        return when {
            !unseen -> spawned.copy(hasUnseenQuestStep = false)
            spawned.screen == Screen.QUESTS -> spawned.withQuestsLookedAt(now)
            else -> spawned.copy(hasUnseenQuestStep = true)
        }
    }

    /**
     * Бросает кости на случайный квест. Шесть часов считаются от последнего случайного квеста, а
     * пока их не было — от момента, когда игрок завёл питомца; без питомца квесты не выпадают.
     */
    private fun GameUiState.withRandomQuestSpawned(now: Long): GameUiState {
        if (selection == null || petBornAtMillis == Growth.NOT_BORN) return this
        val since = lastRandomQuestAtMillis.takeUnless { it == PlayerState.NO_RANDOM_QUEST }
            ?: petBornAtMillis
        val spawn = QuestEngine.maybeSpawnRandom(questCatalog, quests, balance, since, now, random)
            ?: return this
        return copy(quests = spawn.quests, lastRandomQuestAtMillis = now)
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
        if (readOnly) return false
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
     * one off, since the pet has but one head.
     *
     * Neither putting an item on nor taking it off moves a single bar: what the pet wears is a look
     * and nothing more (see [Item.effects]), so the stats are the same before and after.
     *
     * @param selection the item and the variant of it to put on or take off.
     * @return True when the item was put on or taken off, false when the player doesn't own it, it is
     * not registered any more, or it is not something the pet can wear.
     */
    fun toggleWorn(selection: ItemSelection): Boolean {
        if (readOnly) return false
        val current = _state.value
        val item = catalog.findItemById(selection.itemId) ?: return false
        if (!item.isWearable) return false
        if ((current.owned[selection] ?: 0) <= 0) return false

        val worn = if (selection in current.worn) {
            current.worn - selection
        } else {
            current.worn.filterNot { it.itemId == selection.itemId }.toSet() + selection
        }

        _state.value = current.copy(worn = worn)
        persist()
        return true
    }

    /**
     * Отмечает товар целью или снимает отметку — это делает звёздочка на карточке магазина. Цель
     * сохраняется сразу и переживает перезапуск; новая цель встаёт в конец, так что карточки на
     * главном экране не меняются местами.
     *
     * Целью может быть только то, на что можно копить: товар, который есть в магазине, в варианте,
     * который у него есть, и не уже купленная вещь (еду покупают снова и снова, поэтому она целью
     * быть может). Снять отметку можно всегда.
     *
     * @param selection товар и вариант, выбранный на карточке.
     * @return True, когда цель добавлена или снята, false, когда такой товар целью стать не может.
     */
    fun toggleGoal(selection: ItemSelection): Boolean {
        if (readOnly) return false
        val current = _state.value
        val goals = if (selection in current.goals) {
            current.goals - selection
        } else {
            if (!canBeGoal(selection, current.owned)) return false
            current.goals + selection
        }

        _state.value = current.copy(goals = goals)
        persist()
        return true
    }

    /**
     * Открывает магазин по нажатию на карточку цели: на полке товара и с вариантом цели, так что
     * игрок сразу видит свою звёздочку и кнопку покупки. Цель на товар, которого в магазине больше
     * нет, открывает магазин как есть.
     *
     * @param selection цель, по карточке которой нажали.
     */
    fun openGoal(selection: ItemSelection) {
        openScreen(Screen.SHOP)
        val item = catalog.findItemById(selection.itemId) ?: return
        selectCategory(item.category)
        pickVariant(item.id, selection.variantId)
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
     * Купленное перестаёт быть целью — на него накопили; другой вариант того же товара, если он
     * тоже был целью, остаётся.
     *
     * @return True when the purchase went through, false when there was nothing to buy or not
     * enough money for it.
     */
    fun buyCart(): Boolean {
        if (readOnly) return false
        val current = _state.value
        if (!current.canBuyCart) return false

        val owned = current.owned.toMutableMap()
        val bought = mutableSetOf<ItemSelection>()
        var logged = current
        var spentMust = 0
        var spentWant = 0
        current.quantities.forEach { (itemId, quantity) ->
            if (quantity <= 0) return@forEach
            val item = catalog.findItemById(itemId) ?: return@forEach
            val key = ItemSelection(itemId, current.pickedVariantOf(item))
            owned[key] = (owned[key] ?: 0) + quantity
            bought += key
            val cost = item.price * quantity
            when (item.category.spendKind) {
                SpendKind.MUST -> spentMust += cost
                SpendKind.WANT -> spentWant += cost
            }
            logged = logged.logged(
                MoneyEntry(
                    reason = MoneyLog.purchaseReason(item.name, quantity),
                    delta = -cost,
                    gameDay = clock.today(),
                    timestampMillis = clock.nowMillis(),
                    itemId = item.id,
                    variantId = key.variantId,
                    quantity = quantity,
                    spendKind = item.category.spendKind
                )
            )
        }

        _state.value = stateForNavigatingTo(Screen.MAIN).copy(
            balance = current.balance - (spentMust + spentWant),
            owned = owned.toMap(),
            goals = current.goals.filterNot { goal -> goal in bought },
            quantities = emptyMap(),
            pickedVariants = emptyMap(),
            cartPrice = 0,
            moneyLog = logged.moneyLog,
            budget = current.budget?.let {
                it.copy(spentMust = it.spentMust + spentMust, spentWant = it.spentWant + spentWant)
            }
        )
        persist()
        return true
    }

    /**
     * Закрывает вклад до срока: тело возвращается на текущий счёт, проценты не начисляются.
     *
     * Вклад, доживший до срока, сперва гасится [settleMaturedDeposit] — тому, кто дождался
     * процентов, они не сгорают только потому, что игрок не заходил на экраны, которые обычно
     * это замечают.
     *
     * @return True, когда вклад закрыт досрочно, false, когда вклада не было или он уже погашен
     * (по сроку — тогда деньги на счету, но не через "досрочно").
     */
    fun closeDepositEarly(): Boolean {
        if (readOnly) return false
        settleMaturedDeposit()

        val current = _state.value
        val deposit = current.deposit ?: return false

        _state.value = current.copy(
            balance = current.balance + deposit.amount,
            deposit = null
        ).logged(MoneyLog.REASON_DEPOSIT_CLOSED_EARLY, deposit.amount)
        persist()
        return true
    }

    /**
     * Гасит вклад, доживший до срока: тело и проценты возвращаются на текущий счёт, вклада больше
     * нет. Вклад, которому ещё рано, и отсутствующий вклад не делают ничего.
     *
     * Вызывается там же, где пересчитывается доступность бонуса дня: при создании модели, при
     * каждом переходе между экранами и после перемотки времени, — то есть в каждой точке, где
     * игра узнаёт, что день сменился.
     */
    private fun settleMaturedDeposit() {
        val current = _state.value
        val deposit = current.deposit ?: return
        if (!deposit.isMatureOn(clock.today())) return

        // Проценты попадают в журнал, только когда они есть: вклад, который не заработал и
        // монеты, не заработал ничего, и строка «Проценты по вкладу +0» рассказывала бы игроку
        // ровно об этом — но так, будто что-то начислили.
        val settled = current.copy(
            balance = current.balance + deposit.payout,
            deposit = null
        ).logged(MoneyLog.REASON_DEPOSIT_CLOSED, deposit.amount)

        _state.value = if (deposit.interest > 0) {
            settled.logged(MoneyLog.REASON_DEPOSIT_INTEREST, deposit.interest)
        } else {
            settled
        }
        persist()
    }

    /**
     * Записывает изменение текущего счёта в [GameUiState.moneyLog], со знаком и причиной, как
     * их читает игрок.
     *
     * @param reason причина изменения так, как её читает игрок.
     * @param delta изменение текущего счёта, со знаком.
     * @return Это состояние с дописанной строкой журнала.
     */
    private fun GameUiState.logged(reason: String, delta: Int): GameUiState = logged(
        MoneyEntry(
            reason = reason,
            delta = delta,
            gameDay = clock.today(),
            timestampMillis = clock.nowMillis()
        )
    )

    /**
     * @param entry готовая строка журнала — например, покупка с товаром и количеством.
     * @return Это состояние с дописанной строкой журнала.
     */
    private fun GameUiState.logged(entry: MoneyEntry): GameUiState =
        copy(moneyLog = moneyLog.plus(entry))

    /**
     * Adds coins the player earned to the balance and remembers them right away, so money is never
     * lost to the app being closed.
     *
     * @param amount coins to add; zero or less is ignored, as spending goes through [buyCart].
     * @param reason причина начисления так, как её читает игрок в журнале; по умолчанию —
     * [MoneyLog.REASON_REWARD].
     */
    fun earn(amount: Int, reason: String = MoneyLog.REASON_REWARD) {
        if (amount <= 0) return
        _state.value = _state.value.let { it.copy(balance = it.balance + amount) }
            .logged(reason, amount)
        persist()
    }

    /**
     * Выдаёт игроку бонус дня — один раз за игровой день (см. [Economy.isDailyBonusAvailable]) — и
     * этим же начинает новый игровой период: прошлый бюджет закрывается в
     * [GameUiState.previousBudgetResult], открывается планирование и игрок оказывается на экране
     * бюджета, откуда бы он ни нажал кнопку.
     *
     * Вклад, доживший до срока, сперва гасится [settleMaturedDeposit], как и в [confirmBudget]:
     * его тело и проценты должны лечь на счёт до того, как с этого счёта снимут «сколько
     * сохранено», — иначе игрок, дождавшийся срока, выглядел бы в отчёте так, будто всё потратил.
     *
     * @return True, когда бонус выдан, false, когда этот день уже платил.
     */
    fun claimDailyBonus(): Boolean {
        if (readOnly) return false
        settleMaturedDeposit()

        val current = _state.value
        val today = clock.today()
        if (!Economy.isDailyBonusAvailable(current.lastDailyBonusDay, today)) return false

        val closed = current.budget?.let {
            BudgetResult(
                plannedMust = it.plannedMust,
                actualMust = it.spentMust,
                plannedWant = it.plannedWant,
                actualWant = it.spentWant,
                plannedSavings = it.plannedSavings,
                // Сколько реально осталось на счёте: считается до начисления бонуса, иначе
                // деньги нового периода оказались бы сохранёнными в прошлом.
                actualSavings = current.balance,
                plannedDeposit = it.plannedDeposit,
                startDay = it.startDay
            )
        }
        _state.value = current.copy(
            balance = current.balance + Economy.DAILY_BONUS,
            lastDailyBonusDay = today,
            dailyBonusAvailable = false,
            budget = null,
            previousBudgetResult = closed ?: current.previousBudgetResult,
            budgetHistory = closed?.let { BudgetHistory.plus(current.budgetHistory, it) }
                ?: current.budgetHistory,
            budgetDraft = null,
            planningOpen = true
        ).logged(MoneyLog.REASON_DAILY_BONUS, Economy.DAILY_BONUS)
        persist()
        openScreen(Screen.BUDGET)
        return true
    }

    /**
     * Запоминает раскладку, которую игрок набирает на экране планирования, чтобы он мог закрыть
     * экран и вернуться к ней — в том числе после перезапуска приложения.
     *
     * Раскладка приводится в допустимый вид ([Budget.normalize]) до того, как попадёт в состояние,
     * поэтому раскладка, в которой суммы не сходятся, не сохраняется никогда.
     *
     * Вклад, доживший до срока, сперва гасится [settleMaturedDeposit], как и в
     * [closeDepositEarly]: раскладка считается от денег, которые у игрока действительно есть, и
     * поле нового вклада появляется в тот же миг, когда старый закрылся.
     *
     * @param draft что набрал игрок; игнорируется, когда планировать нечего
     * ([GameUiState.canPlanBudget]) — подтверждённый бюджет не переписывается.
     */
    fun updateBudgetDraft(draft: BudgetDraft) {
        if (readOnly) return
        settleMaturedDeposit()

        val current = _state.value
        if (!current.canPlanBudget) return

        _state.value = current.copy(
            budgetDraft = Budget.normalize(
                draft = draft,
                total = current.totalToPlan,
                depositAllowed = current.deposit == null
            )
        )
        persist()
    }

    /**
     * Подтверждает бюджет: со счёта уезжает вклад — если игрок его выбрал, — и начинается период,
     * в котором план уже не меняется. Планы трат денег не двигают: это планы, а не счета.
     *
     * Вклад, доживший до срока, сперва гасится [settleMaturedDeposit], как и в
     * [closeDepositEarly]: его тело и проценты раскладываются вместе со всем остальным, а не
     * лежат мимо подтверждённого плана.
     *
     * @return True, когда бюджет подтверждён, false, когда планировать нечего
     * ([GameUiState.canPlanBudget]), то есть подтверждать нечего.
     */
    fun confirmBudget(): Boolean {
        if (readOnly) return false
        settleMaturedDeposit()

        val current = _state.value
        if (!current.canPlanBudget) return false

        val total = current.totalToPlan
        val draft = Budget.normalize(
            draft = current.planningDraft,
            total = total,
            depositAllowed = current.deposit == null
        )

        var next = current.copy(balance = total - draft.depositAmount)
        if (draft.depositAmount > 0) {
            next = next.copy(
                deposit = Deposit.openedOn(
                    amount = draft.depositAmount,
                    termDays = draft.depositTermDays,
                    day = clock.today()
                )
            ).logged(MoneyLog.REASON_DEPOSIT_OPENED, -draft.depositAmount)
        }

        _state.value = next.copy(
            budget = BudgetState(
                plannedMust = draft.mustSpend,
                plannedWant = draft.wantSpend,
                plannedSavings = Budget.savingsOf(draft, total),
                plannedDeposit = draft.depositAmount,
                spentMust = 0,
                spentWant = 0,
                startDay = clock.today()
            ),
            planningOpen = false,
            budgetDraft = null
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
     * @param settings the saved settings the app starts with.
     * @return The initial [GameUiState]: the player's game as it was saved, everything else fresh.
     */
    private fun restoredState(settings: GameSettings): GameUiState {
        val saved = store.load()
        // The time a demo skipped outlives a restart as a shift, not as a moment reached: the
        // real time that passed while the app was closed runs on top of the skipped hours instead
        // of eating them, so the bars keep falling and the day keeps counting in between.
        clock.fastForward(saved.clockShiftMillis)
        // And whatever the device's clock says, the game never goes back behind what the player was
        // already shown — the age stage, the bars, the day the bonus was paid on.
        clock.fastForwardTo(saved.gameNowMillis)
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
        // Квест, которого больше нет в данных, сыграть нельзя: его запись просто отбрасывается.
        val known = saved.quests.filter { questCatalog.findQuestById(it.questId) != null }
        // Квест, ждущий выбора на шаге, которого больше нет в данных, завершается тем же правилом,
        // что и «Дальше» (см. [QuestEngine.advance]), — иначе он висел бы активным навсегда.
        val quests = known.fold(known) { restored, progress ->
            val quest = questCatalog.findQuestById(progress.questId) ?: return@fold restored
            val stepVanished = progress.isActive && progress.lastChoice == null &&
                quest.node(progress.nodeId) == null
            if (stepVanished) QuestEngine.advance(quest, restored, now) ?: restored else restored
        }

        return GameUiState(
            selection = saved.selection,
            petName = saved.petName,
            balance = saved.balance,
            deposit = saved.deposit,
            budget = saved.budget,
            previousBudgetResult = saved.previousBudgetResult,
            budgetDraft = saved.budgetDraft,
            planningOpen = saved.planningOpen,
            moneyLog = saved.moneyLog,
            budgetHistory = saved.budgetHistory,
            questLog = saved.questLog,
            lastDailyBonusDay = saved.lastDailyBonusDay,
            dailyBonusAvailable = Economy.isDailyBonusAvailable(
                lastClaimedDay = saved.lastDailyBonusDay,
                today = clock.today()
            ),
            owned = saved.owned,
            worn = wearableOf(saved.worn, saved.owned),
            goals = goalsOf(saved.goals, saved.owned),
            stats = saved.stats.decayedBy(ticks),
            statsUpdatedAtMillis = statsUpdatedAt + ticks * PetStats.TICK_MILLIS,
            petAge = Growth.ageAt(bornAt, now),
            petBornAtMillis = bornAt,
            subLocationIndex = existingSubLocation(saved.subLocationIndex),
            todayDay = clock.today(),
            quests = quests,
            questsSeenAtMillis = saved.questsSeenAtMillis,
            lastRandomQuestAtMillis = saved.lastRandomQuestAtMillis,
            hasUnseenQuestStep = QuestEngine.hasUnseenStep(quests, saved.questsSeenAtMillis, now),
            settings = settings
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
     * Оставляет из сохранённых целей только те, на которые ещё можно копить: товары и их варианты
     * могут пропасть из данных между запусками, а купленная вещь целью уже не является.
     *
     * @param goals цели, как их сохранили.
     * @param owned что у игрока есть.
     * @return Годные цели в сохранённом порядке, без повторов.
     */
    private fun goalsOf(
        goals: List<ItemSelection>,
        owned: Map<ItemSelection, Int>
    ): List<ItemSelection> = goals.distinct().filter { goal -> canBeGoal(goal, owned) }

    /**
     * @param selection товар и вариант.
     * @param owned что у игрока есть.
     * @return Можно ли на [selection] копить: товар есть в каталоге, вариант у него есть, и это не
     * уже купленная вещь. Еда годится всегда — её покупают снова.
     */
    private fun canBeGoal(selection: ItemSelection, owned: Map<ItemSelection, Int>): Boolean {
        val item = catalog.findItemById(selection.itemId) ?: return false
        if (selection.variantId !in item.variantIds) return false
        return item.category.use == ItemUse.CONSUMED || (owned[selection] ?: 0) <= 0
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
                deposit = current.deposit,
                budget = current.budget,
                previousBudgetResult = current.previousBudgetResult,
                budgetDraft = current.budgetDraft,
                planningOpen = current.planningOpen,
                moneyLog = current.moneyLog,
                budgetHistory = current.budgetHistory,
                questLog = current.questLog,
                lastDailyBonusDay = current.lastDailyBonusDay,
                owned = current.owned,
                worn = current.worn,
                goals = current.goals,
                stats = current.stats,
                statsUpdatedAtMillis = current.statsUpdatedAtMillis,
                petBornAtMillis = current.petBornAtMillis,
                gameNowMillis = clock.nowMillis(),
                clockShiftMillis = clock.shiftMillis,
                quests = current.quests,
                questsSeenAtMillis = current.questsSeenAtMillis,
                lastRandomQuestAtMillis = current.lastRandomQuestAtMillis
            )
        )
    }

    /**
     * Updates the game settings (sound, music, theme).
     *
     * @param settings the new [GameSettings] to apply.
     */
    fun updateSettings(settings: GameSettings) {
        _state.value = _state.value.copy(settings = settings)
    }

    /**
     * Starts the game over: the pet, the money, the budget, the items and everything else the
     * player has done is dropped — from the screen and from [store] alike, so a restart does not
     * bring it back — and the player is taken back to picking a pet, as on the very first launch.
     *
     * The settings stay as they are: they are the player's, not the game's. The clock stays as it
     * is as well: the game never goes back behind a moment the player was already shown (see
     * [PlayerState.gameNowMillis]), so a demo that skipped ahead stays skipped ahead.
     */
    fun resetProgress() {
        val today = clock.today()
        _state.value = GameUiState(
            dailyBonusAvailable = Economy.isDailyBonusAvailable(
                lastClaimedDay = Economy.NEVER_CLAIMED,
                today = today
            ),
            todayDay = today,
            settings = _state.value.settings
        )
        persist()
    }
}
