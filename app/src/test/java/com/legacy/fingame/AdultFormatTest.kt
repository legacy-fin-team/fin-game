package com.legacy.fingame

import com.legacy.fingame.game.adult.DayReport
import com.legacy.fingame.game.economy.BudgetResult
import com.legacy.fingame.game.economy.MoneyEntry
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.game.quests.QuestChoice
import com.legacy.fingame.game.quests.QuestLog
import com.legacy.fingame.game.quests.QuestProgress
import com.legacy.fingame.game.quests.QuestStatus
import com.legacy.fingame.ui.screens.DayVerdict
import com.legacy.fingame.ui.screens.adultFirstDayOf
import com.legacy.fingame.ui.screens.adultQuestStatusText
import com.legacy.fingame.ui.screens.dateTextOf
import com.legacy.fingame.ui.screens.dayVerdictOf
import com.legacy.fingame.ui.screens.dayVerdictText
import com.legacy.fingame.ui.screens.purchaseIconOf
import com.legacy.fingame.ui.screens.purchaseTitleText
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.screens.questChoiceDetailText
import com.legacy.fingame.ui.screens.questChoiceTitleText
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Тексты хаба взрослого: итог дня, дата, покупки и выборы в квестах. */
class AdultFormatTest {

    private val nbsp = " "

    private fun plan(must: Pair<Int, Int>, want: Pair<Int, Int>) = BudgetResult(
        plannedMust = must.first,
        actualMust = must.second,
        plannedWant = want.first,
        actualWant = want.second,
        plannedSavings = 50,
        actualSavings = 40,
        plannedDeposit = 0,
        startDay = 19_000L
    )

    private fun day(plan: BudgetResult?) = DayReport(19_000L, plan, 0, 0, 0, emptyList())

    @Test
    fun `a day within its plan is done`() {
        val report = day(plan(100 to 100, 50 to 20))
        assertEquals(DayVerdict.DONE, dayVerdictOf(report))
        assertEquals("План выполнен", dayVerdictText(report))
    }

    @Test
    fun `overspend adds up both kinds and ignores savings`() {
        val report = day(plan(100 to 130, 50 to 60))
        assertEquals(DayVerdict.OVERSPENT, dayVerdictOf(report))
        assertEquals("Перерасход +40", dayVerdictText(report))
    }

    @Test
    fun `a running plan is in progress until it is overspent`() {
        assertEquals("План идёт", dayVerdictText(day(plan(100 to 40, 50 to 20)).copy(inProgress = true)))
        assertEquals("Перерасход +10", dayVerdictText(day(plan(100 to 110, 50 to 20)).copy(inProgress = true)))
    }

    @Test
    fun `quest choices count toward the first day too`() {
        val quests = QuestLog(listOf(QuestChoice("q", "n", "Да", 0, 0, 18_990L, 1L)))
        assertEquals(18_990L, adultFirstDayOf(MoneyLog.EMPTY, emptyList(), 19_005L, quests))
    }

    @Test
    fun `a day without a plan says so`() {
        assertEquals("Без плана", dayVerdictText(day(null)))
    }

    @Test
    fun `the date is the calendar day of the game day`() {
        assertEquals("26 сентября", dateTextOf(LocalDate.of(2026, 9, 26).toEpochDay()))
    }

    @Test
    fun `days are counted from the oldest thing remembered`() {
        val log = MoneyLog(listOf(MoneyEntry("Бонус дня", 50, 19_003L, 1L)))
        assertEquals(19_001L, adultFirstDayOf(log, listOf(plan(0 to 0, 0 to 0).copy(startDay = 19_001L)), 19_005L))
        assertEquals(19_003L, adultFirstDayOf(log, emptyList(), 19_005L))
        assertEquals(19_005L, adultFirstDayOf(MoneyLog.EMPTY, emptyList(), 19_005L))
    }

    @Test
    fun `a purchase icon falls back to its shop section and then to the shop`() {
        assertEquals("a.webp", purchaseIconOf("a.webp", "c.webp") { true })
        assertEquals("c.webp", purchaseIconOf("a.webp", "c.webp") { it == "c.webp" })
        assertEquals(Sprites.SHOP, purchaseIconOf(null, null) { true })
    }

    @Test
    fun `a purchase reads as name times quantity`() {
        assertEquals("Яблоко$nbsp×${nbsp}3", purchaseTitleText("Яблоко", 3))
    }

    @Test
    fun `a quest choice names its step and what it brought`() {
        val choice = QuestChoice(TestQuests.PICNIC.id, "food", "Фрукты", -40, 60, 19_001L, 1L)

        assertEquals("Шаг 1: Фрукты", questChoiceTitleText(TestQuests.PICNIC, choice))
        assertEquals("Фрукты", questChoiceTitleText(null, choice))
        assertEquals(
            "-40${nbsp}монет$nbsp· +60%$nbsp· день${nbsp}2",
            questChoiceDetailText(choice, firstDay = 19_000L)
        )
        assertEquals(
            "день${nbsp}1",
            questChoiceDetailText(choice.copy(moneyDelta = 0, progressDelta = 0), 19_001L)
        )
    }

    @Test
    fun `quest status for the adult`() {
        assertEquals("Не идёт", adultQuestStatusText(TestQuests.PICNIC, null))
        val finished = QuestProgress(
            questId = TestQuests.PICNIC.id,
            nodeId = "food",
            availableAtMillis = 0L,
            progress = 80,
            status = QuestStatus.FINISHED
        )
        assertEquals("Завершён$nbsp· 80%", adultQuestStatusText(TestQuests.PICNIC, finished))
        val active = finished.copy(status = QuestStatus.ACTIVE, progress = 20)
        assertEquals(
            "Идёт$nbsp· шаг 1 из ${TestQuests.PICNIC.stepCount}$nbsp· 20%",
            adultQuestStatusText(TestQuests.PICNIC, active)
        )
    }
}
