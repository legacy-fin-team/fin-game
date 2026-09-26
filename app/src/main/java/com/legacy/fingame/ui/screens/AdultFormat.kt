package com.legacy.fingame.ui.screens

import com.legacy.fingame.game.adult.DayReport
import com.legacy.fingame.game.economy.BudgetResult
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.game.quests.Quest
import com.legacy.fingame.game.quests.QuestChoice
import com.legacy.fingame.game.quests.QuestProgress
import com.legacy.fingame.ui.components.Sprites
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Дата дня так, как её читает взрослый: «26 сентября». */
private val DateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"))

/**
 * Иконка покупки: картинка варианта товара, а пока её нет в ассетах — иконка его раздела магазина;
 * товар, которого больше нет, — иконка магазина. Красная заглушка «ERROR» взрослому ничего не скажет.
 *
 * @param iconPath картинка варианта, или null, когда товара больше нет.
 * @param categoryIcon иконка раздела магазина, или null, когда товара больше нет.
 * @param exists есть ли такой файл в ассетах.
 * @return Путь к картинке, которая точно есть — или иконка магазина.
 */
fun purchaseIconOf(iconPath: String?, categoryIcon: String?, exists: (String) -> Boolean): String =
    iconPath?.takeIf(exists) ?: categoryIcon?.takeIf(exists) ?: Sprites.SHOP

/** Чем кончился день по плану. */
enum class DayVerdict {
    /** План был, и траты в него уложились. */
    DONE,

    /** План был, и траты вышли за него. */
    OVERSPENT,

    /** В этот день плана не подтверждали. */
    NO_PLAN
}

/**
 * День, от которого взрослому считаются номера дней: самый старый из тех, что помнят журнал и
 * история бюджета, — так «день 1» один и тот же во всех вкладках и в журнале ребёнка.
 *
 * @param log журнал денег.
 * @param history история бюджета.
 * @param todayDay сегодняшний игровой день — пока истории нет, счёт идёт от сегодня.
 * @return День, который называется первым.
 */
fun adultFirstDayOf(log: MoneyLog, history: List<BudgetResult>, todayDay: Long): Long {
    val oldestPlan = history.map { it.startDay }.filter { it > 0L }.minOrNull()
    return listOfNotNull(log.oldestGameDay, oldestPlan, todayDay).min()
}

/**
 * @param gameDay игровой день — дни от эпохи (с перемоткой демо).
 * @return Дата этого дня: «26 сентября».
 */
fun dateTextOf(gameDay: Long): String = DateFormat.format(LocalDate.ofEpochDay(gameDay))

/**
 * @param report день.
 * @return Перерасход плана: сколько обязательные и необязательные вместе вышли за свои планы;
 * ноль, когда плана не было или он выполнен.
 */
fun overspendOf(report: DayReport): Int {
    val plan = report.plan ?: return 0
    return (plan.actualMust - plan.plannedMust).coerceAtLeast(0) +
        (plan.actualWant - plan.plannedWant).coerceAtLeast(0)
}

/**
 * @param report день.
 * @return Итог дня: без плана, план выполнен или перерасход.
 */
fun dayVerdictOf(report: DayReport): DayVerdict = when {
    report.plan == null -> DayVerdict.NO_PLAN
    overspendOf(report) > 0 -> DayVerdict.OVERSPENT
    else -> DayVerdict.DONE
}

/**
 * @param report день.
 * @return Итог дня одной строкой: «План выполнен», «Перерасход +30» или «Без плана».
 */
fun dayVerdictText(report: DayReport): String = when (dayVerdictOf(report)) {
    DayVerdict.DONE -> "План выполнен"
    DayVerdict.OVERSPENT -> "Перерасход +${overspendOf(report)}"
    DayVerdict.NO_PLAN -> "Без плана"
}

/**
 * @param name имя товара.
 * @param quantity сколько штук купили.
 * @return «Яблоко × 3».
 */
fun purchaseTitleText(name: String, quantity: Int): String =
    "$name$NoBreakSpace×$NoBreakSpace${quantity.coerceAtLeast(1)}"

/**
 * Где ребёнок в квесте — для карточки квеста у взрослого.
 *
 * @param quest квест, или null, когда его больше нет в данных.
 * @param progress где ребёнок в квесте, или null, когда квест сейчас не идёт.
 * @return «Идёт · шаг 2 из 3», «Завершён · 80%» или «Не идёт».
 */
fun adultQuestStatusText(quest: Quest?, progress: QuestProgress?): String = when {
    progress == null -> "Не идёт"
    progress.isFinished -> finishedText(quest?.hasProgress == true, progress.progress)
    quest == null -> "Идёт"
    else -> buildString {
        append("Идёт")
        append(DotSeparator)
        append(
            stepText(
                quest.stepNumberOf(progress.nodeId).coerceAtLeast(1),
                quest.stepCount.coerceAtLeast(1)
            ).replaceFirstChar { it.lowercase() }
        )
        if (quest.hasProgress) append("$DotSeparator${progress.progress}%")
    }
}

/**
 * @param quest квест, или null, когда его больше нет в данных.
 * @param choice выбор ребёнка.
 * @return «Шаг 1: Фрукты» — или только вариант, когда шага уже не найти.
 */
fun questChoiceTitleText(quest: Quest?, choice: QuestChoice): String {
    val step = quest?.stepNumberOf(choice.nodeId) ?: 0
    return if (step > 0) "Шаг $step: ${choice.optionLabel}" else choice.optionLabel
}

/**
 * @param choice выбор ребёнка.
 * @param firstDay день, от которого считаются номера (см. [adultFirstDayOf]).
 * @return Что выбор принёс, и когда: «−40 монет · +60% · день 2»; нулевые части пропускаются.
 */
fun questChoiceDetailText(choice: QuestChoice, firstDay: Long): String = listOfNotNull(
    choice.moneyDelta.takeIf { it != 0 }?.let { if (it > 0) "+${coinsText(it)}" else coinsText(it) },
    choice.progressDelta.takeIf { it != 0 }?.let { "${signedAmountText(it)}%" },
    "день$NoBreakSpace${dayNumberOf(choice.gameDay, firstDay)}"
).joinToString(DotSeparator)
