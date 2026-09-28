package com.legacy.fingame.utils

import com.legacy.fingame.game.items.CustomItemsCodec
import com.legacy.fingame.game.quests.CustomQuestsCodec
import android.content.Context
import android.util.Log
import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.PlayerStateStore
import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.game.economy.BudgetDraft
import com.legacy.fingame.game.economy.BudgetResult
import com.legacy.fingame.game.economy.BudgetState
import com.legacy.fingame.game.economy.Deposit
import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.game.stats.StatKind

/**
 * [PlayerStateStore] backed by SharedPreferences: this is what makes the player's game survive
 * the app being closed and the process being killed.
 *
 * Every value is stored under a key of its own rather than as one blob, so a state that grew a
 * new field still reads back on a device that saved it before the field existed. The exceptions
 * are [PlayerState.moneyLog], [PlayerState.quests], [PlayerState.goals],
 * [PlayerState.budgetHistory] and [PlayerState.questLog]: all are lists of variable length, and a
 * key per record would turn the preferences into a file of thousands of lines, so each is written
 * as a single string, by [MoneyLogCodec], [QuestStateCodec], [GoalsCodec], [BudgetHistoryCodec]
 * and [QuestLogCodec] respectively. The goals are a string rather than a string set because their order
 * is the player's own, and a set would lose it.
 *
 * @param context current local application context. Used to get access to SharedPreferences.
 */
class PlayerPreferences(context: Context) : PlayerStateStore {

    companion object {
        private const val TAG = "PlayerPreferences"

        private const val PREFERENCES_NAME = "player"
        private const val KEY_ANIMAL_ID = "selected_animal_id"
        private const val KEY_ANIMAL_VARIANT_ID = "selected_animal_variant_id"
        private const val KEY_PET_NAME = "pet_name"
        private const val KEY_SUB_LOCATION_INDEX = "sub_location_index"
        private const val KEY_BALANCE = "balance"
        private const val KEY_LAST_DAILY_BONUS_DAY = "last_daily_bonus_day"
        private const val KEY_OWNED_ITEMS = "owned_items"
        private const val KEY_WORN_ITEMS = "worn_items"
        private const val KEY_STATS_UPDATED_AT = "stats_updated_at"
        private const val KEY_PET_BORN_AT = "pet_born_at"
        private const val KEY_GAME_NOW = "game_now"
        private const val KEY_CLOCK_SHIFT = "clock_shift"
        private const val KEY_DEPOSIT_AMOUNT = "deposit_amount"
        private const val KEY_DEPOSIT_TERM_DAYS = "deposit_term_days"
        private const val KEY_DEPOSIT_RATE_PERCENT = "deposit_rate_percent"
        private const val KEY_DEPOSIT_OPENED_DAY = "deposit_opened_day"
        private const val KEY_BUDGET_PRESENT = "budget_present"
        private const val KEY_BUDGET_PLANNED_MUST = "budget_planned_must"
        private const val KEY_BUDGET_PLANNED_WANT = "budget_planned_want"
        private const val KEY_BUDGET_PLANNED_SAVINGS_LEFT = "budget_planned_savings_left"
        private const val KEY_BUDGET_PLANNED_DEPOSIT = "budget_planned_deposit"
        private const val KEY_BUDGET_SPENT_MUST = "budget_spent_must"
        private const val KEY_BUDGET_SPENT_WANT = "budget_spent_want"
        private const val KEY_BUDGET_START_DAY = "budget_start_day"
        private const val KEY_PREVIOUS_BUDGET_PRESENT = "previous_budget_present"
        private const val KEY_PREVIOUS_BUDGET_PLANNED_MUST = "previous_budget_planned_must"
        private const val KEY_PREVIOUS_BUDGET_ACTUAL_MUST = "previous_budget_actual_must"
        private const val KEY_PREVIOUS_BUDGET_PLANNED_WANT = "previous_budget_planned_want"
        private const val KEY_PREVIOUS_BUDGET_ACTUAL_WANT = "previous_budget_actual_want"
        private const val KEY_PREVIOUS_BUDGET_PLANNED_SAVINGS_LEFT =
            "previous_budget_planned_savings_left"
        private const val KEY_PREVIOUS_BUDGET_ACTUAL_SAVINGS = "previous_budget_actual_savings"
        private const val KEY_PREVIOUS_BUDGET_PLANNED_DEPOSIT = "previous_budget_planned_deposit"
        private const val KEY_PREVIOUS_BUDGET_START_DAY = "previous_budget_start_day"
        private const val KEY_BUDGET_DRAFT_PRESENT = "budget_draft_present"
        private const val KEY_BUDGET_DRAFT_MUST = "budget_draft_must"
        private const val KEY_BUDGET_DRAFT_WANT = "budget_draft_want"
        private const val KEY_BUDGET_DRAFT_DEPOSIT_AMOUNT = "budget_draft_deposit_amount"
        private const val KEY_BUDGET_DRAFT_DEPOSIT_TERM_DAYS = "budget_draft_deposit_term_days"
        private const val KEY_PLANNING_OPEN = "planning_open"
        private const val KEY_MONEY_LOG = "money_log"
        private const val KEY_GOALS = "goals"

        /** Состояние квестов одной строкой, см. [QuestStateCodec]. */
        internal const val KEY_QUESTS = "quests"

        /** Момент последнего взгляда на экран квестов. */
        internal const val KEY_QUESTS_SEEN_AT = "quests_seen_at"

        /** Момент, когда выпал последний случайный квест. */
        internal const val KEY_LAST_RANDOM_QUEST_AT = "last_random_quest_at"

        /** История бюджета одной строкой, см. [BudgetHistoryCodec]. */
        internal const val KEY_BUDGET_HISTORY = "budget_history"

        /** Журнал выборов в квестах одной строкой, см. [QuestLogCodec]. */
        internal const val KEY_QUEST_LOG = "quest_log"

        /** Свои предметы взрослого одной строкой, см. [CustomItemsCodec]. */
        internal const val KEY_CUSTOM_ITEMS = "custom_items"

        /** Свои квесты взрослого одной строкой, см. [CustomQuestsCodec]. */
        internal const val KEY_CUSTOM_QUESTS = "custom_quests"

        /** Журнал использованных наград одной строкой, см. [RewardUsageLogCodec]. */
        internal const val KEY_REWARD_USAGE_LOG = "reward_usage_log"

        /** Когда взрослый последний раз смотрел журнал наград. */
        internal const val KEY_REWARD_USAGE_SEEN_AT = "reward_usage_seen_at"

        /** Сколько целей ребёнок купил, см. [PlayerState.goalsReached]. */
        internal const val KEY_GOALS_REACHED = "goals_reached"

        /** Key the savings account was stored under, read once more to hand the money back. */
        private const val KEY_RETIRED_SAVINGS = "savings"

        /**
         * Keys earlier versions wrote and this one no longer reads: the savings account, and the
         * confirmed budget and its result in the shape they had before the plan was split into
         * categories. They are dropped on every save so the file stops carrying them around.
         *
         * No key may be in both this list and [LIVE_KEYS]: [save] drops these after writing those,
         * so a key in both would be written and dropped in the very same transaction and would
         * never survive a single launch.
         */
        internal val RETIRED_KEYS: List<String> = listOf(
            KEY_RETIRED_SAVINGS,
            "budget_planned",
            "budget_planned_savings",
            "budget_spent",
            "budget_draft_savings",
            "previous_budget_planned",
            "previous_budget_planned_savings",
            "previous_budget_actual"
        )

        /**
         * Every key this version writes, in one place. [save] writes exactly these, plus one per
         * stat bar (see [KEY_STAT_PREFIX]), and [load] reads them back. The list stands next to
         * [RETIRED_KEYS] so the two can be held against each other.
         */
        internal val LIVE_KEYS: List<String> = listOf(
            KEY_ANIMAL_ID,
            KEY_ANIMAL_VARIANT_ID,
            KEY_PET_NAME,
            KEY_SUB_LOCATION_INDEX,
            KEY_BALANCE,
            KEY_LAST_DAILY_BONUS_DAY,
            KEY_OWNED_ITEMS,
            KEY_WORN_ITEMS,
            KEY_STATS_UPDATED_AT,
            KEY_PET_BORN_AT,
            KEY_GAME_NOW,
            KEY_CLOCK_SHIFT,
            KEY_DEPOSIT_AMOUNT,
            KEY_DEPOSIT_TERM_DAYS,
            KEY_DEPOSIT_RATE_PERCENT,
            KEY_DEPOSIT_OPENED_DAY,
            KEY_BUDGET_PRESENT,
            KEY_BUDGET_PLANNED_MUST,
            KEY_BUDGET_PLANNED_WANT,
            KEY_BUDGET_PLANNED_SAVINGS_LEFT,
            KEY_BUDGET_PLANNED_DEPOSIT,
            KEY_BUDGET_SPENT_MUST,
            KEY_BUDGET_SPENT_WANT,
            KEY_BUDGET_START_DAY,
            KEY_PREVIOUS_BUDGET_PRESENT,
            KEY_PREVIOUS_BUDGET_PLANNED_MUST,
            KEY_PREVIOUS_BUDGET_ACTUAL_MUST,
            KEY_PREVIOUS_BUDGET_PLANNED_WANT,
            KEY_PREVIOUS_BUDGET_ACTUAL_WANT,
            KEY_PREVIOUS_BUDGET_PLANNED_SAVINGS_LEFT,
            KEY_PREVIOUS_BUDGET_ACTUAL_SAVINGS,
            KEY_PREVIOUS_BUDGET_PLANNED_DEPOSIT,
            KEY_PREVIOUS_BUDGET_START_DAY,
            KEY_BUDGET_HISTORY,
            KEY_BUDGET_DRAFT_PRESENT,
            KEY_BUDGET_DRAFT_MUST,
            KEY_BUDGET_DRAFT_WANT,
            KEY_BUDGET_DRAFT_DEPOSIT_AMOUNT,
            KEY_BUDGET_DRAFT_DEPOSIT_TERM_DAYS,
            KEY_PLANNING_OPEN,
            KEY_MONEY_LOG,
            KEY_QUESTS,
            KEY_QUESTS_SEEN_AT,
            KEY_LAST_RANDOM_QUEST_AT,
            KEY_QUEST_LOG,
            KEY_GOALS,
            KEY_CUSTOM_ITEMS,
            KEY_CUSTOM_QUESTS,
            KEY_REWARD_USAGE_LOG,
            KEY_REWARD_USAGE_SEEN_AT,
            KEY_GOALS_REACHED
        )

        /** Prefix of the key one stat bar is stored under, completed by [StatKind.xmlName]. */
        internal const val KEY_STAT_PREFIX = "stat_"

        /**
         * Separators of one owned-item record, stored as `itemId:variantId=count`: item and variant
         * ids come from the data files and hold neither of these characters. A worn item is stored
         * as the `itemId:variantId` part alone, since there is nothing to count about it.
         */
        private const val VARIANT_SEPARATOR = ':'
        private const val COUNT_SEPARATOR = '='
    }

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /**
     * @return The state saved by [save], with everything that was never saved — or was saved
     * empty — left at its default. A pet is only restored when both of its ids are there, since
     * one id without the other resolves to no animal at all.
     */
    override fun load(): PlayerState {
        val defaults = PlayerState()

        val animalId = preferences.getString(KEY_ANIMAL_ID, null)
        val variantId = preferences.getString(KEY_ANIMAL_VARIANT_ID, null)
        val selection = if (animalId.isNullOrBlank() || variantId.isNullOrBlank()) {
            defaults.selection
        } else {
            AnimalSelection(animalId = animalId, variantId = variantId)
        }

        return PlayerState(
            selection = selection,
            petName = preferences.getString(KEY_PET_NAME, null) ?: defaults.petName,
            subLocationIndex = preferences.getInt(KEY_SUB_LOCATION_INDEX, defaults.subLocationIndex),
            // Счёта сбережений больше нет, деньги игрока не должны пропасть: то, что лежало на
            // нём в сохранении прошлой версии, становится текущими деньгами.
            balance = preferences.getInt(KEY_BALANCE, defaults.balance) +
                preferences.getInt(KEY_RETIRED_SAVINGS, 0),
            deposit = readDeposit(),
            budget = readBudget(),
            previousBudgetResult = readPreviousBudgetResult(),
            budgetHistory = BudgetHistoryCodec.decode(
                preferences.getString(KEY_BUDGET_HISTORY, null)
            ),
            budgetDraft = readBudgetDraft(),
            planningOpen = preferences.getBoolean(KEY_PLANNING_OPEN, defaults.planningOpen),
            moneyLog = MoneyLogCodec.decode(preferences.getString(KEY_MONEY_LOG, null)),
            lastDailyBonusDay = preferences.getLong(
                KEY_LAST_DAILY_BONUS_DAY,
                defaults.lastDailyBonusDay
            ),
            owned = readOwned(defaults.owned),
            worn = readWorn(defaults.worn),
            goals = GoalsCodec.decode(preferences.getString(KEY_GOALS, null)),
            stats = readStats(defaults.stats),
            statsUpdatedAtMillis = preferences.getLong(
                KEY_STATS_UPDATED_AT,
                defaults.statsUpdatedAtMillis
            ),
            petBornAtMillis = preferences.getLong(KEY_PET_BORN_AT, defaults.petBornAtMillis),
            gameNowMillis = preferences.getLong(KEY_GAME_NOW, defaults.gameNowMillis),
            clockShiftMillis = preferences.getLong(KEY_CLOCK_SHIFT, defaults.clockShiftMillis),
            quests = QuestStateCodec.decode(preferences.getString(KEY_QUESTS, null)),
            questsSeenAtMillis = preferences.getLong(
                KEY_QUESTS_SEEN_AT,
                defaults.questsSeenAtMillis
            ),
            lastRandomQuestAtMillis = preferences.getLong(
                KEY_LAST_RANDOM_QUEST_AT,
                defaults.lastRandomQuestAtMillis
            ),
            questLog = QuestLogCodec.decode(preferences.getString(KEY_QUEST_LOG, null)),
            customItems = CustomItemsCodec.decode(preferences.getString(KEY_CUSTOM_ITEMS, null)),
            customQuests = CustomQuestsCodec.decode(preferences.getString(KEY_CUSTOM_QUESTS, null)),
            rewardUsageLog = RewardUsageLogCodec.decode(preferences.getString(KEY_REWARD_USAGE_LOG, null)),
            rewardUsageSeenAtMillis = preferences.getLong(
                KEY_REWARD_USAGE_SEEN_AT,
                defaults.rewardUsageSeenAtMillis
            ),
            goalsReached = preferences.getInt(KEY_GOALS_REACHED, defaults.goalsReached)
        )
    }

    /**
     * Writes the whole state in a single edit, so a save can never leave half of it behind.
     *
     * @param state the state to remember for the next launch.
     */
    override fun save(state: PlayerState) {
        val editor = preferences.edit()
            .putString(KEY_ANIMAL_ID, state.selection?.animalId)
            .putString(KEY_ANIMAL_VARIANT_ID, state.selection?.variantId)
            .putString(KEY_PET_NAME, state.petName)
            .putInt(KEY_SUB_LOCATION_INDEX, state.subLocationIndex)
            .putInt(KEY_BALANCE, state.balance)
            .putLong(KEY_LAST_DAILY_BONUS_DAY, state.lastDailyBonusDay)
            .putStringSet(KEY_OWNED_ITEMS, state.owned.map(::encodeOwned).toSet())
            .putStringSet(KEY_WORN_ITEMS, state.worn.map(::encodeSelection).toSet())
            .putLong(KEY_STATS_UPDATED_AT, state.statsUpdatedAtMillis)
            .putLong(KEY_PET_BORN_AT, state.petBornAtMillis)
            .putLong(KEY_GAME_NOW, state.gameNowMillis)
            .putLong(KEY_CLOCK_SHIFT, state.clockShiftMillis)
            .putInt(KEY_DEPOSIT_AMOUNT, state.deposit?.amount ?: 0)
            .putInt(KEY_DEPOSIT_TERM_DAYS, state.deposit?.termDays ?: 0)
            .putInt(KEY_DEPOSIT_RATE_PERCENT, state.deposit?.ratePercent ?: 0)
            .putLong(KEY_DEPOSIT_OPENED_DAY, state.deposit?.openedDay ?: 0L)
            .putBoolean(KEY_BUDGET_PRESENT, state.budget != null)
            .putInt(KEY_BUDGET_PLANNED_MUST, state.budget?.plannedMust ?: 0)
            .putInt(KEY_BUDGET_PLANNED_WANT, state.budget?.plannedWant ?: 0)
            .putInt(KEY_BUDGET_PLANNED_SAVINGS_LEFT, state.budget?.plannedSavings ?: 0)
            .putInt(KEY_BUDGET_PLANNED_DEPOSIT, state.budget?.plannedDeposit ?: 0)
            .putInt(KEY_BUDGET_SPENT_MUST, state.budget?.spentMust ?: 0)
            .putInt(KEY_BUDGET_SPENT_WANT, state.budget?.spentWant ?: 0)
            .putLong(KEY_BUDGET_START_DAY, state.budget?.startDay ?: 0L)
            .putBoolean(KEY_PREVIOUS_BUDGET_PRESENT, state.previousBudgetResult != null)
            .putInt(KEY_PREVIOUS_BUDGET_PLANNED_MUST, state.previousBudgetResult?.plannedMust ?: 0)
            .putInt(KEY_PREVIOUS_BUDGET_ACTUAL_MUST, state.previousBudgetResult?.actualMust ?: 0)
            .putInt(KEY_PREVIOUS_BUDGET_PLANNED_WANT, state.previousBudgetResult?.plannedWant ?: 0)
            .putInt(KEY_PREVIOUS_BUDGET_ACTUAL_WANT, state.previousBudgetResult?.actualWant ?: 0)
            .putInt(
                KEY_PREVIOUS_BUDGET_PLANNED_SAVINGS_LEFT,
                state.previousBudgetResult?.plannedSavings ?: 0
            )
            .putInt(
                KEY_PREVIOUS_BUDGET_ACTUAL_SAVINGS,
                state.previousBudgetResult?.actualSavings ?: 0
            )
            .putInt(
                KEY_PREVIOUS_BUDGET_PLANNED_DEPOSIT,
                state.previousBudgetResult?.plannedDeposit ?: 0
            )
            .putLong(KEY_PREVIOUS_BUDGET_START_DAY, state.previousBudgetResult?.startDay ?: 0L)
            .putString(KEY_BUDGET_HISTORY, BudgetHistoryCodec.encode(state.budgetHistory))
            .putBoolean(KEY_BUDGET_DRAFT_PRESENT, state.budgetDraft != null)
            .putInt(KEY_BUDGET_DRAFT_MUST, state.budgetDraft?.mustSpend ?: 0)
            .putInt(KEY_BUDGET_DRAFT_WANT, state.budgetDraft?.wantSpend ?: 0)
            .putInt(KEY_BUDGET_DRAFT_DEPOSIT_AMOUNT, state.budgetDraft?.depositAmount ?: 0)
            .putInt(
                KEY_BUDGET_DRAFT_DEPOSIT_TERM_DAYS,
                state.budgetDraft?.depositTermDays ?: Deposit.MIN_TERM_DAYS
            )
            .putBoolean(KEY_PLANNING_OPEN, state.planningOpen)
            .putString(KEY_MONEY_LOG, MoneyLogCodec.encode(state.moneyLog))
            .putString(KEY_QUESTS, QuestStateCodec.encode(state.quests))
            .putLong(KEY_QUESTS_SEEN_AT, state.questsSeenAtMillis)
            .putLong(KEY_LAST_RANDOM_QUEST_AT, state.lastRandomQuestAtMillis)
            .putString(KEY_GOALS, GoalsCodec.encode(state.goals))
            .putString(KEY_QUEST_LOG, QuestLogCodec.encode(state.questLog))
            .putString(KEY_CUSTOM_ITEMS, CustomItemsCodec.encode(state.customItems))
            .putString(KEY_CUSTOM_QUESTS, CustomQuestsCodec.encode(state.customQuests))
            .putString(KEY_REWARD_USAGE_LOG, RewardUsageLogCodec.encode(state.rewardUsageLog))
            .putLong(KEY_REWARD_USAGE_SEEN_AT, state.rewardUsageSeenAtMillis)
            .putInt(KEY_GOALS_REACHED, state.goalsReached)

        StatKind.entries.forEach { stat ->
            editor.putInt(KEY_STAT_PREFIX + stat.xmlName, state.stats[stat])
        }
        RETIRED_KEYS.forEach(editor::remove)

        editor.apply()
    }

    /**
     * Reads back what [save] wrote for [PlayerState.owned].
     *
     * A record that doesn't parse is dropped rather than failing the whole load: losing one line of
     * the inventory is a far smaller loss to the player than losing the pet and the money with it.
     *
     * @param defaults value to fall back to when nothing was ever saved.
     * @return The owned items, keyed by item and variant.
     */
    private fun readOwned(defaults: Map<ItemSelection, Int>): Map<ItemSelection, Int> {
        val records = preferences.getStringSet(KEY_OWNED_ITEMS, null) ?: return defaults

        val owned = mutableMapOf<ItemSelection, Int>()
        records.forEach { record ->
            val selection = decodeOwned(record)
            if (selection == null) {
                Log.e(TAG, "Dropped a malformed owned item record: '$record'")
            } else {
                owned[selection.first] = selection.second
            }
        }
        return owned.toMap()
    }

    /**
     * Reads back what [save] wrote for [PlayerState.worn].
     *
     * A record that doesn't parse is dropped the same way [readOwned] drops one: the pet then simply
     * comes back with that one thing taken off, which the player can put back on.
     *
     * @param defaults value to fall back to when nothing was ever saved.
     * @return The items the pet has on.
     */
    private fun readWorn(defaults: Set<ItemSelection>): Set<ItemSelection> {
        val records = preferences.getStringSet(KEY_WORN_ITEMS, null) ?: return defaults

        val worn = mutableSetOf<ItemSelection>()
        records.forEach { record ->
            val selection = decodeSelection(record)
            if (selection == null) {
                Log.e(TAG, "Dropped a malformed worn item record: '$record'")
            } else {
                worn.add(selection)
            }
        }
        return worn.toSet()
    }

    /**
     * Reads back what [save] wrote for [PlayerState.stats].
     *
     * Every bar is stored under a key of its own, so a stat the game only just gained is missing
     * rather than breaking the read, and one it no longer has is left where it is: the value is keyed
     * by the stat's name, and a name nothing answers to is never asked for again.
     *
     * @param defaults value to fall back to for a bar that was never saved.
     * @return The pet's stat bars.
     */
    private fun readStats(defaults: PetStats): PetStats = PetStats(
        StatKind.entries.associateWith { stat ->
            preferences.getInt(KEY_STAT_PREFIX + stat.xmlName, defaults[stat])
                .coerceIn(PetStats.MIN_VALUE, PetStats.MAX_VALUE)
        }
    )

    /**
     * Reads back what [save] wrote for [PlayerState.deposit].
     *
     * A deposit only counts as existing when it has a body: a save with no deposit writes zero
     * into all four keys, and a zero body never turns into anything.
     *
     * @return The player's deposit, or null when there is none.
     */
    private fun readDeposit(): Deposit? {
        val amount = preferences.getInt(KEY_DEPOSIT_AMOUNT, 0)
        if (amount <= 0) return null

        val termDays = preferences.getInt(KEY_DEPOSIT_TERM_DAYS, Deposit.MIN_TERM_DAYS)
            .coerceIn(Deposit.TERM_DAYS)
        return Deposit(
            amount = amount,
            termDays = termDays,
            // Ставки, которой нет в сохранении, отвечает ставка этого срока, а не ноль: вклад,
            // сохранённый версией игры, которая ставку ещё не писала, достаётся игроку с теми
            // условиями, на которые он подписывался, а не беспроцентным.
            ratePercent = preferences.getInt(KEY_DEPOSIT_RATE_PERCENT, Deposit.rateOf(termDays)),
            openedDay = preferences.getLong(KEY_DEPOSIT_OPENED_DAY, 0L)
        )
    }

    /**
     * Reads back what [save] wrote for [PlayerState.budget].
     *
     * A budget confirmed before the plan was split into categories is not carried over: it has no
     * per-category figures, and inventing them would only put a lie into the next report. The
     * period then counts as unplanned and the player lays the money out again; the money itself is
     * untouched.
     *
     * @return The confirmed budget of the current period, or null when there is none.
     */
    private fun readBudget(): BudgetState? {
        if (!preferences.getBoolean(KEY_BUDGET_PRESENT, false)) return null
        if (!preferences.contains(KEY_BUDGET_PLANNED_MUST)) return null

        return BudgetState(
            plannedMust = preferences.getInt(KEY_BUDGET_PLANNED_MUST, 0),
            plannedWant = preferences.getInt(KEY_BUDGET_PLANNED_WANT, 0),
            plannedSavings = preferences.getInt(KEY_BUDGET_PLANNED_SAVINGS_LEFT, 0),
            plannedDeposit = preferences.getInt(KEY_BUDGET_PLANNED_DEPOSIT, 0),
            spentMust = preferences.getInt(KEY_BUDGET_SPENT_MUST, 0),
            spentWant = preferences.getInt(KEY_BUDGET_SPENT_WANT, 0),
            startDay = preferences.getLong(KEY_BUDGET_START_DAY, 0L)
        )
    }

    /**
     * Reads back what [save] wrote for [PlayerState.previousBudgetResult].
     *
     * A result written before the report was split into categories is dropped for the same reason
     * [readBudget] drops the budget: a card of zeroes says less than no card at all.
     *
     * @return The outcome of the last period, or null when none has closed yet.
     */
    private fun readPreviousBudgetResult(): BudgetResult? {
        if (!preferences.getBoolean(KEY_PREVIOUS_BUDGET_PRESENT, false)) return null
        if (!preferences.contains(KEY_PREVIOUS_BUDGET_PLANNED_MUST)) return null

        return BudgetResult(
            plannedMust = preferences.getInt(KEY_PREVIOUS_BUDGET_PLANNED_MUST, 0),
            actualMust = preferences.getInt(KEY_PREVIOUS_BUDGET_ACTUAL_MUST, 0),
            plannedWant = preferences.getInt(KEY_PREVIOUS_BUDGET_PLANNED_WANT, 0),
            actualWant = preferences.getInt(KEY_PREVIOUS_BUDGET_ACTUAL_WANT, 0),
            plannedSavings = preferences.getInt(KEY_PREVIOUS_BUDGET_PLANNED_SAVINGS_LEFT, 0),
            actualSavings = preferences.getInt(KEY_PREVIOUS_BUDGET_ACTUAL_SAVINGS, 0),
            plannedDeposit = preferences.getInt(KEY_PREVIOUS_BUDGET_PLANNED_DEPOSIT, 0),
            startDay = preferences.getLong(KEY_PREVIOUS_BUDGET_START_DAY, 0L)
        )
    }

    /**
     * Reads back what [save] wrote for [PlayerState.budgetDraft].
     *
     * @return The unfinished layout, or null when the player has not touched it.
     */
    private fun readBudgetDraft(): BudgetDraft? {
        if (!preferences.getBoolean(KEY_BUDGET_DRAFT_PRESENT, false)) return null

        return BudgetDraft(
            mustSpend = preferences.getInt(KEY_BUDGET_DRAFT_MUST, 0),
            wantSpend = preferences.getInt(KEY_BUDGET_DRAFT_WANT, 0),
            depositAmount = preferences.getInt(KEY_BUDGET_DRAFT_DEPOSIT_AMOUNT, 0),
            depositTermDays = preferences.getInt(
                KEY_BUDGET_DRAFT_DEPOSIT_TERM_DAYS,
                Deposit.MIN_TERM_DAYS
            ).coerceIn(Deposit.TERM_DAYS)
        )
    }

    /**
     * @param owned one entry of [PlayerState.owned].
     * @return The entry as a single string, in the `itemId:variantId=count` form.
     */
    private fun encodeOwned(owned: Map.Entry<ItemSelection, Int>): String =
        "${encodeSelection(owned.key)}$COUNT_SEPARATOR${owned.value}"

    /**
     * @param selection an item in one of its variants.
     * @return The item as a single string, in the `itemId:variantId` form.
     */
    private fun encodeSelection(selection: ItemSelection): String =
        "${selection.itemId}$VARIANT_SEPARATOR${selection.variantId}"

    /**
     * @param record a record written by [encodeOwned].
     * @return The item with the number of them owned, or null when the record is not in the
     * `itemId:variantId=count` form or holds no usable count.
     */
    private fun decodeOwned(record: String): Pair<ItemSelection, Int>? {
        val countSeparator = record.lastIndexOf(COUNT_SEPARATOR)
        if (countSeparator <= 0) return null

        val count = record.substring(countSeparator + 1).toIntOrNull()
        if (count == null || count <= 0) return null

        val selection = decodeSelection(record.substring(0, countSeparator)) ?: return null
        return selection to count
    }

    /**
     * @param record a record written by [encodeSelection].
     * @return The item and its variant, or null when the record is not in the `itemId:variantId`
     * form, i.e. one of the two ids is missing.
     */
    private fun decodeSelection(record: String): ItemSelection? {
        val variantSeparator = record.indexOf(VARIANT_SEPARATOR)
        if (variantSeparator <= 0 || variantSeparator == record.lastIndex) return null

        return ItemSelection(
            itemId = record.substring(0, variantSeparator),
            variantId = record.substring(variantSeparator + 1)
        )
    }
}
