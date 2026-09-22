package com.legacy.fingame.game

import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.game.animals.Growth
import com.legacy.fingame.game.economy.BudgetDraft
import com.legacy.fingame.game.economy.BudgetResult
import com.legacy.fingame.game.economy.BudgetState
import com.legacy.fingame.game.economy.Deposit
import com.legacy.fingame.game.economy.Economy
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.game.stats.PetStats

/**
 * Everything about the player's game that has to outlive the app process, i.e. what the player
 * finds in place when coming back after the app was closed.
 *
 * Screen-level state is deliberately not part of it: which screen is open and what is picked in
 * the shop belong to a single visit and start over on the next launch. Money and the things bought
 * with it, on the other hand, are the player's own and are kept.
 *
 * @property selection the pet the player picked, or null while no pet has been picked yet — that
 * is, before the very first launch is over.
 * @property petName name the player gave the pet on the second step of the selection, or an empty
 * string while there is no pet yet — or when the pet comes from a launch that saved no name at all.
 * @property subLocationIndex index of the sub-location the pet was left in, within
 * [com.legacy.fingame.ui.DemoContent.subLocationTitles]. A saved index only means something for
 * the sub-locations that exist now, so [GameViewModel] coerces it into the current range when it
 * restores the state.
 * @property balance coins the player has to spend, never negative. A player who has never played
 * starts with [Economy.STARTING_BALANCE].
 * @property lastDailyBonusDay day the daily bonus was last claimed on, as days since the epoch, or
 * [Economy.NEVER_CLAIMED] when it was never claimed. Only whole days are kept, since the bonus is
 * given once per calendar day.
 * @property owned how many of each item the player bought, keyed by item and variant: the variant
 * is part of the key because the player owns the black hat, not "a hat". Food is bought over and
 * over, so its counter grows; everything else is owned once.
 * @property worn which of the owned items are put on the pet right now, so a pet dressed up before
 * the app was closed is still dressed up when the player comes back.
 * @property stats the pet's stat bars as they were when the game was last saved. They keep falling
 * while the app is closed, which is why [statsUpdatedAtMillis] is saved next to them.
 * @property statsUpdatedAtMillis moment [stats] were last brought up to date, in milliseconds, or
 * [NEVER_UPDATED] when the pet's stats were never touched. The decay of the time between it and the
 * next launch is applied by [GameViewModel] when it restores the state.
 * @property petBornAtMillis moment the pet was taken in, in milliseconds, or [Growth.NOT_BORN] when
 * there is no pet yet. The pet's age stage is worked out from it (see [Growth.ageAt]) instead of
 * being saved, so the pet grows while the app is closed and the stage can never drift.
 * @property gameNowMillis moment the game's own clock had reached when this state was saved, in
 * milliseconds, or [CLOCK_NEVER_SAVED] when no run has saved one yet. The next launch picks its
 * clock up here instead of starting it over (see
 * [com.legacy.fingame.game.economy.FastForwardClock.fastForwardTo]), so neither the time a demo
 * build skipped nor a device clock moved back since can take the pet's age, its bars or its daily
 * bonus back to where they were before.
 * @property deposit вклад, открытый в банке, или null, когда вклада нет. Вклад бывает только
 * один одновременно, и его тело недоступно, пока он не погашен.
 * @property budget подтверждённый бюджет текущего периода, или null, когда период ещё не
 * начинался — до самого первого планирования или пока идёт планирование следующего.
 * @property previousBudgetResult итог прошлого периода, который показывается при планировании,
 * или null, когда ни один период ещё не закрывался.
 * @property budgetDraft раскладка, которую игрок набрал, но не подтвердил, или null, когда он к
 * ней не притрагивался. Хранится, чтобы экран планирования можно было закрыть и вернуться к нему.
 * @property planningOpen открыто ли планирование: становится true при получении бонуса дня и
 * false при подтверждении бюджета.
 * @property moneyLog журнал изменений текущего счёта, новейшее первым.
 */
data class PlayerState(
    val selection: AnimalSelection? = null,
    val petName: String = "",
    val subLocationIndex: Int = 0,
    val balance: Int = Economy.STARTING_BALANCE,
    val deposit: Deposit? = null,
    val budget: BudgetState? = null,
    val previousBudgetResult: BudgetResult? = null,
    val budgetDraft: BudgetDraft? = null,
    val planningOpen: Boolean = false,
    val moneyLog: MoneyLog = MoneyLog.EMPTY,
    val lastDailyBonusDay: Long = Economy.NEVER_CLAIMED,
    val owned: Map<ItemSelection, Int> = emptyMap(),
    val worn: Set<ItemSelection> = emptySet(),
    val stats: PetStats = PetStats.FULL,
    val statsUpdatedAtMillis: Long = NEVER_UPDATED,
    val petBornAtMillis: Long = Growth.NOT_BORN,
    val gameNowMillis: Long = CLOCK_NEVER_SAVED
) {
    companion object {
        /**
         * Value of [statsUpdatedAtMillis] standing for "the pet's stats were never brought up to
         * date", i.e. the very first launch, so that nothing is taken off a pet that has not been
         * lived with yet.
         */
        const val NEVER_UPDATED = Long.MIN_VALUE

        /**
         * Value of [gameNowMillis] standing for "no run has saved the game's clock yet", i.e. the
         * very first launch or one following a run from before the clock was kept. There is then
         * no moment to pick the clock up at, so it simply starts at the device's own.
         */
        const val CLOCK_NEVER_SAVED = Long.MIN_VALUE
    }
}
