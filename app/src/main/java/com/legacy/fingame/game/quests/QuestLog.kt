package com.legacy.fingame.game.quests

/** Что случилось с этапом, который проверяет взрослый ([Quest.requiresAdultCheck]). */
enum class QuestCheckEvent {
    /** Ребёнок сдал этап на проверку — награды пока нет. */
    SENT,

    /** Взрослый засчитал этап — награда выдана. */
    APPROVED,

    /** Взрослый не засчитал этап — он снова в работе. */
    REJECTED
}

/**
 * Один выбор игрока в квесте — строка истории квестов, которую смотрит взрослый.
 *
 * @property questId id квеста.
 * @property nodeId шаг, на котором выбирали.
 * @property optionLabel надпись выбранного варианта.
 * @property moneyDelta сколько монет выбор принёс (плюс) или стоил (минус) — уже урезанное до
 * баланса, то есть ровно то, что ушло через журнал денег.
 * @property progressDelta насколько сдвинулся прогресс квеста; у квеста без прогресса — ноль.
 * @property gameDay игровой день выбора, как его называет
 * [com.legacy.fingame.game.economy.GameClock.today].
 * @property timestampMillis момент выбора по часам игры.
 * @property check у квеста с проверкой взрослым — что это за запись: сдача этапа, «Засчитать» или
 * «Не засчитано»; у обычного выбора — null. Деньги и прогресс у сдачи и отказа нулевые: награда
 * приходит только с «Засчитать».
 */
data class QuestChoice(
    val questId: String,
    val nodeId: String,
    val optionLabel: String,
    val moneyDelta: Int,
    val progressDelta: Int,
    val gameDay: Long,
    val timestampMillis: Long,
    val check: QuestCheckEvent? = null
)

/**
 * Журнал выборов в квестах, новейший первым — по образцу
 * [com.legacy.fingame.game.economy.MoneyLog]. Длина ограничена [MAX_ENTRIES]: журнал хранится в
 * настройках одной строкой.
 *
 * @property entries выборы, новейший первым.
 */
data class QuestLog(val entries: List<QuestChoice> = emptyList()) {

    /**
     * @param choice только что сделанный выбор.
     * @return Журнал с ним впереди; самый старый выбор отбрасывается, если журнал уже дорос до
     * [MAX_ENTRIES].
     */
    fun plus(choice: QuestChoice): QuestLog = QuestLog((listOf(choice) + entries).take(MAX_ENTRIES))

    companion object {

        /** Сколько последних выборов журнал хранит. */
        const val MAX_ENTRIES = 200

        /** Журнал игрока, который ещё ничего не выбирал. */
        val EMPTY = QuestLog()

        /**
         * @param entries выборы в любом порядке.
         * @return Журнал, в котором новейший выбор стоит первым (по моменту), не длиннее
         * [MAX_ENTRIES].
         */
        fun of(entries: List<QuestChoice>): QuestLog = QuestLog(
            entries.sortedByDescending { it.timestampMillis }.take(MAX_ENTRIES)
        )
    }
}
