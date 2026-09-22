package com.legacy.fingame.game.economy

/**
 * Одна строка журнала: что произошло с деньгами игрока и когда.
 *
 * @property reason причина в том виде, в каком её читает игрок: «Бонус дня», «Яблоко x4»,
 * «В сбережения».
 * @property delta изменение ТЕКУЩЕГО счёта, со знаком. Перевод в сбережения — минус, возврат
 * оттуда — плюс; счета, которых текущий баланс не касается, в журнале не отражаются, потому что
 * игрок смотрит журнал ради своих трат.
 * @property gameDay игровой день, в который это случилось, как его называет [GameClock.today].
 * @property timestampMillis момент по часам игры, в миллисекундах.
 */
data class MoneyEntry(
    val reason: String,
    val delta: Int,
    val gameDay: Long,
    val timestampMillis: Long
)

/**
 * Журнал изменений денег: всё, что случилось с текущим счётом игрока, новейшее первым.
 *
 * Журнал хранится в том же порядке, в каком показывается, поэтому экрану нечего сортировать.
 * Длина ограничена [MAX_ENTRIES]: журнал живёт в настройках приложения вместе с остальным
 * состоянием, и бесконечно растущая строка там рано или поздно стала бы проблемой; игроку же
 * нужны последние операции, а не все за всё время.
 *
 * @property entries записи, новейшая первой.
 */
data class MoneyLog(val entries: List<MoneyEntry> = emptyList()) {

    /**
     * @param entry что произошло.
     * @return Журнал с этой записью впереди остальных; самая старая запись отбрасывается, если
     * журнал уже дорос до [MAX_ENTRIES].
     */
    fun plus(entry: MoneyEntry): MoneyLog =
        MoneyLog((listOf(entry) + entries).take(MAX_ENTRIES))

    companion object {

        /** Сколько последних записей журнал хранит. */
        const val MAX_ENTRIES = 500

        /** Журнал игрока, с которым ещё ничего не происходило. */
        val EMPTY = MoneyLog()

        /** Причина: выдан бонус дня. */
        const val REASON_DAILY_BONUS = "Бонус дня"

        /** Причина: игра начислила деньги вне бонуса дня — за квест, например. */
        const val REASON_REWARD = "Награда"

        /** Причина: деньги переложены с текущего счёта в сбережения. */
        const val REASON_TO_SAVINGS = "В сбережения"

        /** Причина: деньги взяты из сбережений на текущий счёт. */
        const val REASON_FROM_SAVINGS = "Из сбережений"

        /** Причина: открыт вклад. */
        const val REASON_DEPOSIT_OPENED = "Вклад открыт"

        /** Причина: вклад дожил до срока, тело вернулось на текущий счёт. */
        const val REASON_DEPOSIT_CLOSED = "Вклад закрыт"

        /** Причина: начислены проценты по вкладу, погашенному в срок. */
        const val REASON_DEPOSIT_INTEREST = "Проценты по вкладу"

        /** Причина: вклад закрыт до срока, без процентов. */
        const val REASON_DEPOSIT_CLOSED_EARLY = "Вклад закрыт досрочно"

        /**
         * @param name название товара.
         * @param quantity сколько его куплено.
         * @return Причина покупки так, как она пишется в журнале: «Яблоко x4».
         */
        fun purchaseReason(name: String, quantity: Int): String = "$name x$quantity"
    }
}
