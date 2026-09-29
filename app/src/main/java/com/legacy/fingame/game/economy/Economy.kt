package com.legacy.fingame.game.economy

/**
 * The rules of the game's money: what the player starts with, what the daily bonus pays and when
 * that bonus is available again.
 *
 * Money itself lives in [com.legacy.fingame.game.PlayerState.balance] and is earned and spent
 * through [com.legacy.fingame.game.GameViewModel]; this object only holds the numbers and the one
 * rule that has to give the same answer everywhere it is asked.
 */
object Economy {

    /** Money a player who has never played before starts the game with. */
    const val STARTING_BALANCE = 400

    /** Money the daily bonus pays out, once per calendar day. */
    const val DAILY_BONUS = 125

    /**
     * Value of [com.legacy.fingame.game.PlayerState.lastDailyBonusDay] standing for "the bonus was
     * never claimed", i.e. the very first launch, so that the first day is always a payday.
     */
    const val NEVER_CLAIMED = Long.MIN_VALUE

    /**
     * Tells whether the daily bonus can be claimed today.
     *
     * A day that has already paid out never pays again, and neither does a day that is *behind* the
     * one that paid last: moving the device clock back would otherwise hand out a bonus per turn of
     * the clock. Such a player simply waits until the calendar catches up with the day already paid.
     *
     * @param lastClaimedDay day the bonus was claimed on, as days since the epoch, or
     * [NEVER_CLAIMED] when it never was.
     * @param today current day, as days since the epoch (see [GameClock.today]).
     * @return True when [today] is past [lastClaimedDay], i.e. the bonus is waiting for the player.
     */
    fun isDailyBonusAvailable(lastClaimedDay: Long, today: Long): Boolean = today > lastClaimedDay
}
