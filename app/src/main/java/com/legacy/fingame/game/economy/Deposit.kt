package com.legacy.fingame.game.economy

import kotlin.math.roundToInt

/**
 * Вклад игрока: деньги, отложенные в банк на срок, за который банк платит проценты.
 *
 * Вклад бывает только один одновременно, и, пока он открыт, его тело нельзя ни потратить, ни
 * переложить: игрок или дожидается срока и получает проценты, или закрывает вклад досрочно и
 * получает обратно ровно столько, сколько положил.
 *
 * @property amount тело вклада, больше нуля.
 * @property termDays срок, на который вклад открыт, в игровых днях; всегда в [TERM_DAYS].
 * @property ratePercent ставка в целых процентах, соответствующая [termDays] (см. [rateOf]).
 * Хранится вместе со вкладом, а не берётся из таблицы при погашении, чтобы правка таблицы
 * не меняла условия уже открытого вклада.
 * @property openedDay день открытия, как его называет [GameClock.today].
 */
data class Deposit(
    val amount: Int,
    val termDays: Int,
    val ratePercent: Int,
    val openedDay: Long
) {

    /** День, в который вклад можно погасить с процентами. */
    val maturityDay: Long get() = openedDay + termDays

    /** Проценты, которые банк заплатит при погашении в срок. */
    val interest: Int get() = interestOf(amount, ratePercent)

    /** Сколько вернётся на текущий счёт при погашении в срок: тело плюс проценты. */
    val payout: Int get() = amount + interest

    /**
     * @param day день, про который спрашивают, как его называет [GameClock.today].
     * @return True, когда вклад уже дожил до своего срока и ждёт погашения.
     */
    fun isMatureOn(day: Long): Boolean = day >= maturityDay

    companion object {

        /** Самый короткий срок, на который можно открыть вклад, в игровых днях. */
        const val MIN_TERM_DAYS = 2

        /** Самый длинный срок, на который можно открыть вклад, в игровых днях. */
        const val MAX_TERM_DAYS = 7

        /** Все сроки, на которые вклад открывается. */
        val TERM_DAYS: IntRange = MIN_TERM_DAYS..MAX_TERM_DAYS

        /**
         * Ставка за срок, в целых процентах. Чем дольше лежат деньги, тем больше платит банк, а
         * самый длинный срок платит заметно больше остальных, чтобы его было ради чего выбирать.
         */
        private val RATES: Map<Int, Int> = mapOf(
            2 to 4,
            3 to 6,
            4 to 8,
            5 to 10,
            6 to 12,
            7 to 15
        )

        /**
         * @param termDays срок вклада в днях; срок вне [TERM_DAYS] сначала зажимается в них, так
         * что ответ есть всегда.
         * @return Ставка за этот срок, в целых процентах.
         */
        fun rateOf(termDays: Int): Int = RATES.getValue(termDays.coerceIn(TERM_DAYS))

        /**
         * @param amount тело вклада.
         * @param ratePercent ставка в целых процентах.
         * @return Проценты, округлённые до целой монеты: вклад, который не зарабатывает и монеты,
         * не зарабатывает ничего.
         */
        fun interestOf(amount: Int, ratePercent: Int): Int =
            (amount * ratePercent / 100.0).roundToInt()

        /**
         * Открывает вклад на допустимый срок по действующей ставке.
         *
         * @param amount тело вклада.
         * @param termDays желаемый срок; зажимается в [TERM_DAYS].
         * @param day день открытия.
         * @return Открытый вклад.
         */
        fun openedOn(amount: Int, termDays: Int, day: Long): Deposit {
            val term = termDays.coerceIn(TERM_DAYS)
            return Deposit(
                amount = amount,
                termDays = term,
                ratePercent = rateOf(term),
                openedDay = day
            )
        }
    }
}
