package com.legacy.fingame.game.economy

/**
 * Что это за трата с точки зрения плана периода.
 *
 * @property title как категория называется игроку.
 */
enum class SpendKind(val title: String) {
    /** Обязательные траты: то, без чего питомцу не прожить, — еда и игрушки. */
    MUST("Обязательные"),

    /** Необязательные: то, что покупают для красоты, — декор и одежда. */
    WANT("Необязательные")
}

/**
 * Несохранённая раскладка периода: сколько игрок собирается потратить на обязательное и на
 * необязательное и сколько кладёт на вклад. План сбережений здесь не хранится — это всегда
 * остаток (см. [Budget.savingsOf]), поэтому раскладка, в которой суммы не сходятся, невозможна.
 *
 * @property mustSpend план обязательных трат: еда и игрушки.
 * @property wantSpend план необязательных трат: декор и одежда.
 * @property depositAmount сколько уйдёт на новый вклад; ноль — вклад не открывается.
 * @property depositTermDays срок нового вклада; всегда в [Deposit.TERM_DAYS].
 */
data class BudgetDraft(
    val mustSpend: Int,
    val wantSpend: Int,
    val depositAmount: Int,
    val depositTermDays: Int
)

/**
 * Подтверждённый бюджет периода: что игрок запланировал и что уже потратил, по категориям.
 * После подтверждения план не меняется.
 *
 * @property plannedMust план обязательных трат.
 * @property plannedWant план необязательных трат.
 * @property plannedSavings сколько игрок собирался сохранить: остаток, которого не забрали планы
 * трат и вклад.
 * @property plannedDeposit сколько он положил на вклад; ноль, если вклад не открывался.
 * @property spentMust сколько обязательного куплено с момента подтверждения.
 * @property spentWant сколько необязательного куплено с момента подтверждения.
 * @property startDay день подтверждения, как его называет [GameClock.today].
 */
data class BudgetState(
    val plannedMust: Int,
    val plannedWant: Int,
    val plannedSavings: Int,
    val plannedDeposit: Int,
    val spentMust: Int,
    val spentWant: Int,
    val startDay: Long
) {
    /** Сколько игрок собирался потратить всего: обязательное плюс необязательное. */
    val plannedSpend: Int get() = plannedMust + plannedWant

    /** Сколько потрачено всего. */
    val spent: Int get() = spentMust + spentWant

    /** Сколько обязательного плана ещё не потрачено; минус — перерасход. */
    val mustLeft: Int get() = plannedMust - spentMust

    /** Сколько необязательного плана ещё не потрачено; минус — перерасход. */
    val wantLeft: Int get() = plannedWant - spentWant
}

/**
 * Итог закрытого периода: план и факт по каждой категории и по сбережениям.
 *
 * @property plannedMust сколько игрок собирался потратить на обязательное.
 * @property actualMust сколько потратил на самом деле.
 * @property plannedWant сколько собирался потратить на необязательное.
 * @property actualWant сколько потратил на самом деле.
 * @property plannedSavings сколько собирался сохранить.
 * @property actualSavings сколько денег реально осталось на текущем счёте к концу периода.
 * @property plannedDeposit сколько ушло на вклад при подтверждении.
 */
data class BudgetResult(
    val plannedMust: Int,
    val actualMust: Int,
    val plannedWant: Int,
    val actualWant: Int,
    val plannedSavings: Int,
    val actualSavings: Int,
    val plannedDeposit: Int
) {
    /** Плюс — не дотратил, минус — перерасход. */
    val mustDiff: Int get() = plannedMust - actualMust

    /** Плюс — не дотратил, минус — перерасход. */
    val wantDiff: Int get() = plannedWant - actualWant

    /** Плюс — сохранил больше, чем собирался. */
    val savingsDiff: Int get() = actualSavings - plannedSavings
}

/** Правила раскладки денег на период. */
object Budget {

    /**
     * @return Раскладка, с которой открывается пустой экран: ничего не запланировано, вклада нет.
     */
    fun startingDraft(): BudgetDraft = BudgetDraft(
        mustSpend = 0,
        wantSpend = 0,
        depositAmount = 0,
        depositTermDays = Deposit.MIN_TERM_DAYS
    )

    /**
     * Приводит раскладку в допустимый вид. Вклад зажимается первым: это реальные деньги,
     * которые уедут со счёта, а планы трат — всего лишь планы, и считать их можно только от
     * того, что на счёте останется.
     *
     * @param draft что набрал игрок.
     * @param total сколько денег вообще можно разложить. Тело уже открытого вклада сюда не
     * входит — им распоряжаться нельзя.
     * @param depositAllowed можно ли открыть новый вклад, то есть свободен ли банк: вклад бывает
     * только один одновременно.
     * @return Раскладка, в которой все суммы неотрицательны, вместе не превышают [total], а срок
     * вклада — один из предлагаемых.
     */
    fun normalize(draft: BudgetDraft, total: Int, depositAllowed: Boolean): BudgetDraft {
        val money = total.coerceAtLeast(0)
        val deposit = if (depositAllowed) draft.depositAmount.coerceIn(0, money) else 0
        val left = money - deposit
        val must = draft.mustSpend.coerceIn(0, left)
        val want = draft.wantSpend.coerceIn(0, left - must)
        return BudgetDraft(
            mustSpend = must,
            wantSpend = want,
            depositAmount = deposit,
            depositTermDays = draft.depositTermDays.coerceIn(Deposit.TERM_DAYS)
        )
    }

    /**
     * @param draft раскладка, приведённая [normalize].
     * @param total сколько денег раскладывается.
     * @return Сколько игрок собирается сохранить: всё, чего не забрали планы трат и вклад.
     */
    fun savingsOf(draft: BudgetDraft, total: Int): Int =
        total - draft.mustSpend - draft.wantSpend - draft.depositAmount

    /**
     * На сколько покупка выйдет за план по одному виду трат.
     *
     * План — это план, а не запрет: перерасход показывается игроку, но покупку не отменяет.
     *
     * @param budget подтверждённый бюджет периода.
     * @param kind вид трат, по которому считается перерасход.
     * @param cartSpend сколько корзина добавит к уже потраченному по этому виду.
     * @return На сколько потраченное вместе с корзиной превысит план; ноль, если план не превышен.
     */
    fun overspendOf(budget: BudgetState, kind: SpendKind, cartSpend: Int): Int {
        val planned = when (kind) {
            SpendKind.MUST -> budget.plannedMust
            SpendKind.WANT -> budget.plannedWant
        }
        val spent = when (kind) {
            SpendKind.MUST -> budget.spentMust
            SpendKind.WANT -> budget.spentWant
        }
        return (spent + cartSpend - planned).coerceAtLeast(0)
    }

    /**
     * Перерасход, который даст покупка, по всем видам трат сразу: то, о чём окно подтверждения
     * предупреждает игрока перед оплатой.
     *
     * О виде трат, которого в корзине нет, окно молчит, даже если план по нему уже превышен: игрок
     * спрашивает про эту покупку, а не про прошлые.
     *
     * @param budget подтверждённый бюджет периода; null — период не запланирован, и сравнивать
     * покупку не с чем.
     * @param cartSpend сколько корзина добавит по каждому виду трат; см.
     * [com.legacy.fingame.game.items.Cart.spendByKindOf].
     * @return Виды трат, по которым покупка выйдет за план, и насколько, в порядке [SpendKind];
     * пусто, если плана нет или он выдержан.
     */
    fun overspendsOf(
        budget: BudgetState?,
        cartSpend: Map<SpendKind, Int>
    ): List<Pair<SpendKind, Int>> {
        if (budget == null) return emptyList()
        return SpendKind.entries.mapNotNull { kind ->
            val spend = cartSpend[kind] ?: 0
            if (spend <= 0) return@mapNotNull null
            val over = overspendOf(budget, kind, spend)
            if (over > 0) kind to over else null
        }
    }
}
