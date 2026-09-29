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
 * @property termDays срок, на который вклад открыт, в игровых днях; при открытии — один из [TERM_DAYS].
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

        /**
         * Сроки, на которые открывается вклад, в игровых днях, от короткого к длинному, и ставка
         * за весь срок в целых процентах.
         *
         * Самый короткий срок платит 15%: положил 100 — через 3 дня вернётся 115. Дальше ставка
         * растёт быстрее срока, и за каждый день вклад приносит тем больше, чем он длиннее:
         * 5% в день на 3 дня, 6% на 5 дней, около 7% на неделю. Поэтому неделя всегда выгоднее,
         * чем класть те же монеты несколько раз подряд на короткий срок, — даже вместе с
         * процентами на проценты и с округлением до целой монеты (это проверяет DepositTest).
         */
        private val RATES: Map<Int, Int> = linkedMapOf(
            3 to 15,
            5 to 30,
            7 to 50
        )

        /** Все сроки, на которые вклад открывается, от короткого к длинному. */
        val TERM_DAYS: List<Int> = RATES.keys.toList()

        /** Самый короткий срок, на который можно открыть вклад, в игровых днях. */
        val MIN_TERM_DAYS: Int = TERM_DAYS.first()

        /** Самый длинный срок, на который можно открыть вклад, в игровых днях. */
        val MAX_TERM_DAYS: Int = TERM_DAYS.last()

        /**
         * Приводит любой срок к одному из [TERM_DAYS]: к ближайшему не короче него, а срок
         * длиннее самого длинного — к самому длинному. Так срок, сохранённый старой версией игры
         * (там были сроки от 2 до 7 дней), превращается в срок, который игра предлагает сейчас.
         *
         * @param termDays срок в днях, любой.
         * @return Один из [TERM_DAYS].
         */
        fun termOf(termDays: Int): Int = TERM_DAYS.firstOrNull { it >= termDays } ?: MAX_TERM_DAYS

        /**
         * @param termDays срок вклада в днях; срок не из [TERM_DAYS] сначала приводится к ним
         * через [termOf], так что ответ есть всегда.
         * @return Ставка за этот срок, в целых процентах.
         */
        fun rateOf(termDays: Int): Int = RATES.getValue(termOf(termDays))

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
         * @param termDays желаемый срок; приводится к [TERM_DAYS] через [termOf].
         * @param day день открытия.
         * @return Открытый вклад.
         */
        fun openedOn(amount: Int, termDays: Int, day: Long): Deposit {
            val term = termOf(termDays)
            return Deposit(
                amount = amount,
                termDays = term,
                ratePercent = rateOf(term),
                openedDay = day
            )
        }
    }
}
