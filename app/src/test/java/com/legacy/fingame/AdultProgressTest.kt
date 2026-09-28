package com.legacy.fingame

import com.legacy.fingame.game.GameUiState
import com.legacy.fingame.game.adult.AdultProgress
import com.legacy.fingame.game.adult.TopicStatus
import com.legacy.fingame.game.economy.BudgetResult
import com.legacy.fingame.game.economy.BudgetState
import com.legacy.fingame.game.economy.Deposit
import com.legacy.fingame.game.economy.MoneyEntry
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.game.economy.SpendKind
import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.game.quests.Quest
import com.legacy.fingame.game.quests.QuestCatalog
import com.legacy.fingame.game.quests.QuestCheckEvent
import com.legacy.fingame.game.quests.QuestChoice
import com.legacy.fingame.game.quests.QuestLog
import com.legacy.fingame.game.quests.QuestProgress
import com.legacy.fingame.game.quests.QuestStatus
import com.legacy.fingame.game.quests.QuestTopic
import com.legacy.fingame.ui.screens.AdultAppGoalsText
import com.legacy.fingame.ui.screens.progressSummaryText
import com.legacy.fingame.ui.screens.topicStatusText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Раздел взрослого «Прогресс»: темы выводятся из фактов игры и не оценивают ребёнка. */
class AdultProgressTest {

    private val piggy = TestQuests.PICNIC.copy(id = "piggy", topic = QuestTopic.SAVING)
    private val picnic = TestQuests.PICNIC.copy(topic = QuestTopic.PLANNING)
    private val catalog = QuestCatalog.of(listOf(picnic, piggy))

    private fun statusOf(state: GameUiState, topic: QuestTopic, quests: QuestCatalog = catalog): TopicStatus =
        AdultProgress.compute(state, quests).topics.single { it.topic == topic }.status

    private fun result(savings: Int = 0, deposit: Int = 0) = BudgetResult(
        plannedMust = 30,
        actualMust = 30,
        plannedWant = 20,
        actualWant = 10,
        plannedSavings = savings,
        actualSavings = savings,
        plannedDeposit = deposit,
        startDay = 20_000L
    )

    @Test
    fun `a fresh game has every topic ahead`() {
        val report = AdultProgress.compute(GameUiState(), catalog)

        assertEquals(QuestTopic.entries, report.topics.map { it.topic })
        assertTrue(report.topics.all { it.status == TopicStatus.AHEAD })
        assertEquals(0f, report.masteredFraction)
        assertEquals("Все темы ещё впереди", progressSummaryText(report))
    }

    @Test
    fun `planning is in progress with a plan and mastered with a closed period and its quest`() {
        val planned = GameUiState(budget = BudgetState(30, 20, 10, 0, 0, 0, 20_000L))
        assertEquals(TopicStatus.IN_PROGRESS, statusOf(planned, QuestTopic.PLANNING))

        val closed = planned.copy(budgetHistory = listOf(result()))
        assertEquals(TopicStatus.IN_PROGRESS, statusOf(closed, QuestTopic.PLANNING))

        val finished = closed.copy(
            quests = listOf(QuestProgress("picnic", Quest.END_NODE, 0L, status = QuestStatus.FINISHED))
        )
        assertEquals(TopicStatus.MASTERED, statusOf(finished, QuestTopic.PLANNING))
        // Без квеста на тему хватает фактов игры.
        assertEquals(TopicStatus.MASTERED, statusOf(closed, QuestTopic.PLANNING, QuestCatalog.EMPTY))
    }

    @Test
    fun `a quest taken again still counts as passed once by its final choice in the history`() {
        val choice = QuestChoice("piggy", "games", "Мяч", 0, 40, 20_000L, 1L)
        val again = GameUiState(
            quests = listOf(QuestProgress("piggy", "food", 2L)),
            questLog = QuestLog(listOf(choice))
        )
        val report = AdultProgress.compute(again, catalog).topics.single { it.topic == QuestTopic.SAVING }
        assertTrue(report.facts.single { it.text.startsWith("Прошёл квест") }.done)

        // Сданный на проверку финал — ещё не пройден.
        val sent = again.copy(questLog = QuestLog(listOf(choice.copy(check = QuestCheckEvent.SENT))))
        val pending = AdultProgress.compute(sent, catalog).topics.single { it.topic == QuestTopic.SAVING }
        assertFalse(pending.facts.single { it.text.startsWith("Прошёл квест") }.done)
    }

    @Test
    fun `game facts light up their topics`() {
        val state = GameUiState(
            budgetHistory = listOf(result(savings = 15)),
            deposit = Deposit(amount = 50, termDays = 7, ratePercent = 10, openedDay = 20_000L),
            moneyLog = MoneyLog(
                listOf(MoneyEntry("Яблоко x1", -15, 20_000L, 1L, itemId = "apple", quantity = 1, spendKind = SpendKind.MUST))
            ),
            goals = listOf(ItemSelection("ball", "red")),
            goalsReached = 1
        )

        assertEquals(TopicStatus.MASTERED, statusOf(state, QuestTopic.NEEDS_WANTS, QuestCatalog.EMPTY))
        assertEquals(TopicStatus.MASTERED, statusOf(state, QuestTopic.DEPOSIT, QuestCatalog.EMPTY))
        assertEquals(TopicStatus.MASTERED, statusOf(state, QuestTopic.GOALS, QuestCatalog.EMPTY))
        // Сбережения были, а квест на тему ещё не пройден.
        assertEquals(TopicStatus.IN_PROGRESS, statusOf(state, QuestTopic.SAVING))
        // Только поставленная цель — тема в процессе.
        assertEquals(TopicStatus.IN_PROGRESS, statusOf(state.copy(goalsReached = 0), QuestTopic.GOALS))
    }

    @Test
    fun `a topic with nothing to check in the game waits for its quests`() {
        val report = AdultProgress.compute(GameUiState(), QuestCatalog.EMPTY)

        val honesty = report.topics.single { it.topic == QuestTopic.HONESTY }
        assertEquals(TopicStatus.AHEAD, honesty.status)
        assertTrue(honesty.facts.isEmpty())
    }

    @Test
    fun `the words are neutral`() {
        val state = GameUiState(
            budgetHistory = listOf(result()),
            // Без сбережений в плане — освоено одно планирование.
            budget = BudgetState(30, 20, 0, 0, 0, 0, 20_000L)
        )
        val report = AdultProgress.compute(state, QuestCatalog.EMPTY)

        assertEquals(1, report.masteredCount)
        assertEquals(1f / QuestTopic.entries.size, report.masteredFraction)
        assertEquals("Освоено 1 тема из ${QuestTopic.entries.size}", progressSummaryText(report).substringBefore(' '))
        assertEquals("Тема ещё впереди", topicStatusText(TopicStatus.AHEAD))
        assertEquals("В процессе", topicStatusText(TopicStatus.IN_PROGRESS))
        assertEquals("Освоено", topicStatusText(TopicStatus.MASTERED))
        val words = (report.topics.flatMap { t -> t.facts.map { it.text } } + AdultAppGoalsText).joinToString(" ")
        listOf("плохо", "слишком", "много тратит", "ошиб").forEach { assertFalse(words.lowercase().contains(it)) }
    }
}
