package com.legacy.fingame.game.economy

/**
 * История бюджета: итоги закрытых периодов, новейший первым — по дню начала периода
 * ([BudgetResult.startDay]). Её смотрит взрослый; игроку при планировании хватает последнего итога.
 *
 * Длина ограничена [MAX] по той же причине, что и журнал денег ([MoneyLog.MAX_ENTRIES]): история
 * живёт в настройках приложения одной строкой.
 */
object BudgetHistory {

    /** Сколько последних периодов история хранит — два месяца, если период длится день. */
    const val MAX = 60

    /**
     * @param history история, новейшее первым.
     * @param result итог только что закрытого периода.
     * @return История с [result] на своём месте по дню начала; самый старый итог отбрасывается,
     * если история уже доросла до [MAX]. Итог того же дня встаёт перед прежним (сортировка
     * устойчива).
     */
    fun plus(history: List<BudgetResult>, result: BudgetResult): List<BudgetResult> =
        of(listOf(result) + history)

    /**
     * @param results итоги в любом порядке.
     * @return Они же, новейший период первым, не больше [MAX].
     */
    fun of(results: List<BudgetResult>): List<BudgetResult> =
        results.sortedByDescending { it.startDay }.take(MAX)
}
