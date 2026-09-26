package com.legacy.fingame.game.economy

/**
 * Одна строка журнала: что произошло с деньгами игрока и когда.
 *
 * @property reason причина в том виде, в каком её читает игрок: «Бонус дня», «Яблоко x4»,
 * «Вклад открыт».
 * @property delta изменение ТЕКУЩЕГО счёта, со знаком. Деньги, ушедшие на вклад, — минус,
 * вернувшиеся оттуда — плюс; движения, которых текущий счёт не касается, в журнале не отражаются,
 * потому что игрок смотрит журнал ради своих трат.
 * @property gameDay игровой день, в который это случилось, как его называет [GameClock.today].
 * @property timestampMillis момент по часам игры, в миллисекундах.
 * @property itemId для покупки — id купленного товара, для всего остального — null. Записи,
 * сделанные до истории покупок, его не знают, поэтому и старая покупка читается с null.
 * @property variantId для покупки — вариант товара, в котором его купили, иначе null.
 * @property quantity для покупки — сколько штук куплено одной позицией корзины, иначе ноль.
 * @property spendKind для покупки — в какую категорию плана она легла, иначе null.
 */
data class MoneyEntry(
    val reason: String,
    val delta: Int,
    val gameDay: Long,
    val timestampMillis: Long,
    val itemId: String? = null,
    val variantId: String? = null,
    val quantity: Int = 0,
    val spendKind: SpendKind? = null
) {
    /** Покупка ли это товара — то есть запись, по которой видно, что именно купили. */
    val isPurchase: Boolean get() = itemId != null
}

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
     * День самой старой хранимой записи — начало отсчёта дней, которые видит игрок, — или null,
     * когда журнал пуст и отсчитывать пока не от чего.
     */
    val oldestGameDay: Long? get() = entries.lastOrNull()?.gameDay

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

        /**
         * @param entries записи в любом порядке.
         * @return Журнал, в котором новейшее стоит первым: по игровому дню, внутри дня — по
         * моменту. Записи одного момента остаются в том порядке, в каком пришли (сортировка
         * устойчива).
         */
        fun of(entries: List<MoneyEntry>): MoneyLog = MoneyLog(
            entries.sortedWith(
                compareByDescending<MoneyEntry> { it.gameDay }.thenByDescending { it.timestampMillis }
            ).take(MAX_ENTRIES)
        )

        /** Журнал игрока, с которым ещё ничего не происходило. */
        val EMPTY = MoneyLog()

        /** Причина: выдан бонус дня. */
        const val REASON_DAILY_BONUS = "Бонус дня"

        /** Причина: игра начислила деньги вне бонуса дня — за квест, например. */
        const val REASON_REWARD = "Награда"

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

        /**
         * @param title название квеста.
         * @return Причина начисления или списания по квесту так, как она пишется в журнале:
         * «Квест: Пикник».
         */
        fun questReason(title: String): String = "Квест: $title"
    }
}
