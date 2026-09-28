package com.legacy.fingame.game.adult

/**
 * Ребёнок использовал награду из жизни (предмет раздела «Другое», см.
 * [com.legacy.fingame.game.items.ItemUse.REDEEMED]) — строка журнала «Использованные награды»,
 * который смотрит взрослый.
 *
 * @property itemId id предмета.
 * @property itemName название предмета в тот момент — предмет взрослый может потом убрать.
 * @property petName как звали питомца ребёнка: по нему взрослый узнаёт, чья это игра.
 * @property gameDay игровой день, как его называет [com.legacy.fingame.game.economy.GameClock.today].
 * @property timestampMillis момент по часам игры.
 */
data class RewardUsage(
    val itemId: String,
    val itemName: String,
    val petName: String,
    val gameDay: Long,
    val timestampMillis: Long
)

/**
 * Журнал использованных наград, новейшая первой — по образцу
 * [com.legacy.fingame.game.quests.QuestLog]. Длина ограничена [MAX_ENTRIES].
 *
 * @property entries записи, новейшая первой.
 */
data class RewardUsageLog(val entries: List<RewardUsage> = emptyList()) {

    /**
     * @param usage только что использованная награда.
     * @return Журнал с ней впереди; самая старая отбрасывается, если журнал уже дорос до
     * [MAX_ENTRIES].
     */
    fun plus(usage: RewardUsage): RewardUsageLog =
        RewardUsageLog((listOf(usage) + entries).take(MAX_ENTRIES))

    /**
     * @param seenAtMillis когда взрослый последний раз смотрел журнал, или [NEVER_SEEN].
     * @return Сколько наград использовано с тех пор — число на вкладке «Награды».
     */
    fun unseenCount(seenAtMillis: Long): Int = entries.count { it.timestampMillis > seenAtMillis }

    companion object {

        /** Сколько последних записей журнал хранит. */
        const val MAX_ENTRIES = 100

        /** Значение «взрослый журнал наград ещё не смотрел». */
        const val NEVER_SEEN = Long.MIN_VALUE

        /** Журнал, в котором ещё ничего нет. */
        val EMPTY = RewardUsageLog()

        /**
         * @param entries записи в любом порядке.
         * @return Журнал, новейшая первой (по моменту), не длиннее [MAX_ENTRIES].
         */
        fun of(entries: List<RewardUsage>): RewardUsageLog =
            RewardUsageLog(entries.sortedByDescending { it.timestampMillis }.take(MAX_ENTRIES))
    }
}
