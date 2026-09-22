package com.legacy.fingame.game.economy

/**
 * Подтверждённый бюджет текущего периода: сколько игрок решил оставить себе на траты, куда убрал
 * остальное и сколько уже потратил.
 *
 * Подтверждённый бюджет не меняется до конца периода: переложить деньги между счетами игрок
 * может, а переписать план — нет.
 *
 * @property planned сколько денег осталось на текущем счёте сразу после подтверждения, то есть
 * сколько игрок собирался потратить за период.
 * @property plannedSavings сколько он отложил в сбережения.
 * @property plannedDeposit сколько он положил на вклад; ноль, если вклад не открывался.
 * @property spent сумма покупок с момента подтверждения. Переводы между счетами и операции по
 * вкладу сюда не попадают: тратой считается только покупка.
 * @property startDay день подтверждения, как его называет [GameClock.today].
 */
data class BudgetState(
    val planned: Int,
    val plannedSavings: Int,
    val plannedDeposit: Int,
    val spent: Int,
    val startDay: Long
)

/**
 * Итог закрытого периода — то, что показывается игроку, когда он садится планировать следующий.
 *
 * @property planned сколько он собирался потратить.
 * @property plannedSavings сколько откладывал в сбережения.
 * @property plannedDeposit сколько клал на вклад.
 * @property actual сколько потратил на самом деле.
 */
data class BudgetResult(
    val planned: Int,
    val plannedSavings: Int,
    val plannedDeposit: Int,
    val actual: Int
) {

    /**
     * Насколько игрок разошёлся с планом: плюс — не потратил всё, что мог, минус — вышел за
     * бюджет, то есть достал деньги со сбережений или со вклада и потратил их.
     */
    val diff: Int get() = planned - actual
}

/**
 * Несохранённая раскладка: то, что игрок набрал на экране планирования, но ещё не подтвердил.
 *
 * Текущие деньги здесь не хранятся — они всегда остаток (см. [Budget.currentOf]), поэтому
 * раскладка, в которой суммы не сходятся, невозможна в принципе.
 *
 * @property savings сколько уйдёт в сбережения.
 * @property depositAmount сколько уйдёт на новый вклад; ноль — вклад не открывается.
 * @property depositTermDays срок нового вклада; всегда в [Deposit.TERM_DAYS].
 */
data class BudgetDraft(
    val savings: Int,
    val depositAmount: Int,
    val depositTermDays: Int
)

/** Правила раскладки денег на период. */
object Budget {

    /**
     * @param savings сбережения игрока на сейчас.
     * @return Раскладка, с которой открывается пустой экран планирования: сбережения остаются на
     * месте, вклад не открывается, срок — самый короткий из предлагаемых.
     */
    fun startingDraft(savings: Int): BudgetDraft = BudgetDraft(
        savings = savings,
        depositAmount = 0,
        depositTermDays = Deposit.MIN_TERM_DAYS
    )

    /**
     * Приводит раскладку в допустимый вид — единственное место, где это делается, так что ни
     * экран, ни хранилище не могут подсунуть игре раскладку, которая не сходится.
     *
     * @param draft что набрал игрок.
     * @param total сколько денег вообще можно разложить: текущие плюс сбережения. Тело уже
     * открытого вклада сюда не входит — им распоряжаться нельзя.
     * @param depositAllowed можно ли открыть новый вклад, то есть свободен ли банк: вклад бывает
     * только один одновременно.
     * @return Раскладка, в которой сбережения и вклад неотрицательны, вместе не превышают [total],
     * а срок вклада — один из предлагаемых.
     */
    fun normalize(draft: BudgetDraft, total: Int, depositAllowed: Boolean): BudgetDraft {
        val savings = draft.savings.coerceIn(0, total.coerceAtLeast(0))
        val deposit = if (depositAllowed) {
            draft.depositAmount.coerceIn(0, (total - savings).coerceAtLeast(0))
        } else {
            0
        }
        return BudgetDraft(
            savings = savings,
            depositAmount = deposit,
            depositTermDays = draft.depositTermDays.coerceIn(Deposit.TERM_DAYS)
        )
    }

    /**
     * @param draft раскладка, приведённая [normalize].
     * @param total сколько денег раскладывается.
     * @return Сколько останется на текущем счёте, то есть что не забрали сбережения и вклад.
     */
    fun currentOf(draft: BudgetDraft, total: Int): Int =
        total - draft.savings - draft.depositAmount
}
