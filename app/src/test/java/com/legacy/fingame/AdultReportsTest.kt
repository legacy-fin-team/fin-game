package com.legacy.fingame

import com.legacy.fingame.game.adult.DayReport
import com.legacy.fingame.game.adult.dayReportsOf
import com.legacy.fingame.game.adult.purchasesOf
import com.legacy.fingame.game.adult.questHistoryOf
import com.legacy.fingame.game.economy.BudgetResult
import com.legacy.fingame.game.economy.MoneyEntry
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.game.economy.SpendKind
import com.legacy.fingame.game.quests.QuestChoice
import com.legacy.fingame.game.quests.QuestLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
