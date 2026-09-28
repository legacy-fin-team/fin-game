package com.legacy.fingame

import com.legacy.fingame.game.adult.DayReport
import com.legacy.fingame.game.adult.dayReportsOf
import com.legacy.fingame.game.adult.purchasesOf
import com.legacy.fingame.game.adult.questHistoryOf
import com.legacy.fingame.game.economy.BudgetResult
import com.legacy.fingame.game.economy.BudgetState
import com.legacy.fingame.game.economy.MoneyEntry
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.game.economy.SpendKind
import com.legacy.fingame.game.quests.QuestChoice
import com.legacy.fingame.game.quests.QuestLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Отчёты взрослого режима: дни, покупки и история квестов из сохранённых журналов. */
class AdultReportsTest {

    private val day = 19_000L
    private val millis = 1_700_000_000_000L

    private fun plan(startDay: Long) = BudgetResult(
        plannedMust = 40,
        actualMust = 30,
        plannedWant = 20,
        actualWant = 25,
        plannedSavings = 90,
        actualSavings = 85,
        plannedDeposit = 0,
        startDay = startDay
    )

    private fun purchase(
        itemId: String,
        cost: Int,
        kind: SpendKind,
        gameDay: Long,
        at: Long,
        quantity: Int = 1
    ) = MoneyEntry(
        reason = MoneyLog.purchaseReason(itemId, quantity),
        delta = -cost,
        gameDay = gameDay,
        timestampMillis = at,
        itemId = itemId,
        variantId = "default",
        quantity = quantity,
        spendKind = kind
    )

    private fun bonus(gameDay: Long, at: Long) =
        MoneyEntry(MoneyLog.REASON_DAILY_BONUS, 50, gameDay, at)

    @Test
    fun `days go newest first and hold the plan and the money of that day`() {
        val apple = purchase("apple", 30, SpendKind.MUST, day + 1, millis + 3, quantity = 2)
        val hat = purchase("hat", 100, SpendKind.WANT, day + 1, millis + 4)
        val log = MoneyLog.of(
            listOf(
                bonus(day, millis),
                bonus(day + 1, millis + 2),
                apple,
                hat,
                MoneyEntry(MoneyLog.REASON_DEPOSIT_OPENED, -200, day + 1, millis + 5)
            )
        )

        val reports = dayReportsOf(history = listOf(plan(day + 2), plan(day)), log = log)

        assertEquals(listOf(day + 2, day + 1, day), reports.map { it.gameDay })
        assertEquals(DayReport(day + 2, plan(day + 2), 0, 0, 0, emptyList()), reports[0])
        val busy = reports[1]
        assertNull(busy.plan)
        assertEquals(30, busy.spentMust)
        assertEquals(100, busy.spentWant)
        assertEquals(50, busy.earned)
        assertEquals(listOf(hat, apple), busy.purchases)
        assertEquals(plan(day), reports[2].plan)
        assertEquals(50, reports[2].earned)
    }

    @Test
    fun `no plan and no money means no days`() {
        assertEquals(emptyList<DayReport>(), dayReportsOf(emptyList(), MoneyLog.EMPTY))
    }

    @Test
    fun `purchases are the item records only, newest first`() {
        val older = purchase("apple", 30, SpendKind.MUST, day, millis + 1)
        val newer = purchase("hat", 100, SpendKind.WANT, day + 1, millis + 2)
        val log = MoneyLog.of(
            listOf(
                older,
                bonus(day, millis),
                newer,
                // Покупка из старой версии: товара в записи нет, в историю покупок она не попадает.
                MoneyEntry("Яблоко x4", -60, day, millis + 3)
            )
        )

        assertEquals(listOf(newer, older), purchasesOf(log))
    }

    @Test
    fun `quest history groups choices by quest, latest quest first, choices in order made`() {
        fun choice(questId: String, nodeId: String, label: String, at: Long) = QuestChoice(
            questId = questId,
            nodeId = nodeId,
            optionLabel = label,
            moneyDelta = 0,
            progressDelta = 0,
            gameDay = day,
            timestampMillis = at
        )
        val picnicFood = choice("picnic", "food", "Фрукты", millis + 1)
        val wallet = choice("lost_wallet", "found", "Вернуть", millis + 2)
        val picnicGames = choice("picnic", "games", "Мяч", millis + 3)
        val vanished = choice("gone", "start", "Да", millis)
        val log = QuestLog.of(listOf(picnicFood, wallet, picnicGames, vanished))

        val history = questHistoryOf(log, TestQuests.CATALOG)

        assertEquals(listOf(TestQuests.PICNIC, TestQuests.WALLET, null), history.map { it.quest })
        assertEquals(listOf(picnicFood, picnicGames), history[0].choices)
        assertEquals(listOf(wallet), history[1].choices)
        assertEquals("gone", history[2].questId)
    }

    @Test
    fun `the open period shows as a day in progress`() {
        val running = BudgetState(
            plannedMust = 40,
            plannedWant = 20,
            plannedSavings = 90,
            plannedDeposit = 0,
            spentMust = 15,
            spentWant = 30,
            startDay = day + 1
        )
        val log = MoneyLog.of(listOf(bonus(day + 1, millis + 1), bonus(day, millis)))

        val reports = dayReportsOf(listOf(plan(day)), log, current = running, balance = 105)

        assertEquals(2, reports.size)
        val today = reports[0]
        assertEquals(day + 1, today.gameDay)
        assertTrue(today.inProgress)
        assertEquals(
            BudgetResult(
                plannedMust = 40,
                actualMust = 15,
                plannedWant = 20,
                actualWant = 30,
                plannedSavings = 90,
                actualSavings = 105,
                plannedDeposit = 0,
                startDay = day + 1
            ),
            today.plan
        )
        assertFalse(reports[1].inProgress)
        assertEquals(plan(day), reports[1].plan)
    }

    @Test
    fun `a closed plan of the same day wins over the running one`() {
        val running = BudgetState(40, 20, 90, 0, 0, 0, startDay = day)

        val reports = dayReportsOf(listOf(plan(day)), MoneyLog.EMPTY, current = running, balance = 1)

        assertEquals(1, reports.size)
        assertEquals(plan(day), reports[0].plan)
        assertFalse(reports[0].inProgress)
    }
}
