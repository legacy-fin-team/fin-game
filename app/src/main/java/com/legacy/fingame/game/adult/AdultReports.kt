package com.legacy.fingame.game.adult

import com.legacy.fingame.game.economy.BudgetResult
import com.legacy.fingame.game.economy.BudgetState
import com.legacy.fingame.game.economy.MoneyEntry
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.game.economy.SpendKind
import com.legacy.fingame.game.quests.Quest
import com.legacy.fingame.game.quests.QuestCatalog
import com.legacy.fingame.game.quests.QuestChoice
import com.legacy.fingame.game.quests.QuestEngine
import com.legacy.fingame.game.quests.QuestLog
import com.legacy.fingame.game.quests.QuestProgress

/**
 * Один игровой день глазами взрослого: как он был спланирован и что в нём случилось с деньгами.
 *
 * @property gameDay игровой день, как его называет [com.legacy.fingame.game.economy.GameClock.today].
 * @property plan итог периода, начатого в этот день, или null, когда в этот день плана не
 * подтверждали. План и факт по категориям — в нём самом (факт — за весь период).
 * @property spentMust сколько в этот день ушло на обязательные покупки, по журналу денег.
 * @property spentWant сколько в этот день ушло на необязательные покупки, по журналу денег.
 * Покупки из журнала версии до истории покупок категории не знают и сюда не попадают.
 * @property earned сколько в этот день пришло на текущий счёт: бонус, награды, вернувшийся вклад.
 * @property purchases покупки этого дня (записи с товаром), новейшая первой.
 * @property inProgress план этого дня ещё идёт: период не закрыт, [plan] — предварительный итог
 * (факт — сколько уже потрачено, сбережения — что сейчас на счёте).
 */
data class DayReport(
    val gameDay: Long,
    val plan: BudgetResult?,
    val spentMust: Int,
    val spentWant: Int,
    val earned: Int,
    val purchases: List<MoneyEntry>,
    val inProgress: Boolean = false
)

/**
 * Квест в истории взрослого со всеми выборами, которые в нём делал ребёнок.
 *
 * @property quest квест, или null, когда его больше нет в данных — выборы всё равно показываются.
 * @property choices выборы в этом квесте в том порядке, в каком их делали: старший первым.
 */
data class QuestHistoryEntry(
    val quest: Quest?,
    val choices: List<QuestChoice>
) {
    /** Id квеста — есть и тогда, когда самого квеста в данных уже нет. */
    val questId: String get() = quest?.id ?: choices.first().questId
}

/**
 * @param history история бюджета ([com.legacy.fingame.game.PlayerState.budgetHistory]).
 * @param log журнал денег.
 * @return Дни от новых к старым — только те, в которые начинался период или двигались деньги.
 * Если в один день подтверждали несколько планов, берётся последний.
 *
 * @param current идущий, ещё не закрытый период ([com.legacy.fingame.game.GameUiState.budget]):
 * его день показывается с предварительным итогом и [DayReport.inProgress]. Если на этот день в
 * истории уже есть закрытый итог, главнее история.
 * @param balance что сейчас на счёте — «факт» сбережений идущего периода.
 */
fun dayReportsOf(
    history: List<BudgetResult>,
    log: MoneyLog,
    current: BudgetState? = null,
    balance: Int = 0
): List<DayReport> {
    // История — новейший первым, поэтому первый итог дня и есть последний подтверждённый.
    val closed = history.groupBy { it.startDay }.mapValues { (_, results) -> results.first() }
    val running = current?.takeIf { it.startDay !in closed }
    val plans = if (running == null) closed else closed + (running.startDay to running.preliminaryResult(balance))
    val entriesByDay = log.entries.groupBy { it.gameDay }
    val days = (plans.keys + entriesByDay.keys).sortedDescending()
    return days.map { day ->
        val entries = entriesByDay[day].orEmpty()
        DayReport(
            gameDay = day,
            inProgress = running != null && day == running.startDay,
            plan = plans[day],
            spentMust = spentOn(entries, SpendKind.MUST),
            spentWant = spentOn(entries, SpendKind.WANT),
            earned = entries.filter { it.delta > 0 }.sumOf { it.delta },
            purchases = entries.filter { it.isPurchase }
        )
    }
}

/**
 * @param log журнал денег.
 * @return Покупки — записи с товаром ([MoneyEntry.itemId]), новейшая первой.
 */
fun purchasesOf(log: MoneyLog): List<MoneyEntry> = log.entries.filter { it.isPurchase }

/**
 * @param log журнал выборов в квестах.
 * @param catalog квесты игры — по ним у выборов появляются названия и статусы.
 * @return По записи на квест: сначала квест, в котором выбирали последним; выборы внутри — в
 * порядке, в каком их делали.
 */
fun questHistoryOf(log: QuestLog, catalog: QuestCatalog): List<QuestHistoryEntry> =
    // Журнал — новейший первым, так что groupBy ставит квесты по последнему выбору.
    log.entries.groupBy { it.questId }.map { (questId, choices) ->
        QuestHistoryEntry(
            quest = catalog.findQuestById(questId),
            choices = choices.sortedBy { it.timestampMillis }
        )
    }

/** Итог идущего периода, каким он был бы, закройся период сейчас. */
private fun BudgetState.preliminaryResult(balance: Int) = BudgetResult(
    plannedMust = plannedMust,
    actualMust = spentMust,
    plannedWant = plannedWant,
    actualWant = spentWant,
    plannedSavings = plannedSavings,
    actualSavings = balance,
    plannedDeposit = plannedDeposit,
    startDay = startDay
)

private fun spentOn(entries: List<MoneyEntry>, kind: SpendKind): Int =
    entries.filter { it.spendKind == kind && it.delta < 0 }.sumOf { -it.delta }

/**
 * @param quests состояние квестов ребёнка.
 * @param catalog квесты — чтобы найти сам квест; этап квеста, которого больше нет, не показывается.
 * @return Этапы, которые ждут проверки взрослым: квест и где в нём ребёнок.
 */
fun questChecksOf(quests: List<QuestProgress>, catalog: QuestCatalog): List<Pair<Quest, QuestProgress>> =
    QuestEngine.awaitingCheck(quests).mapNotNull { progress ->
        catalog.findQuestById(progress.questId)?.let { it to progress }
    }
