package com.legacy.fingame

import com.legacy.fingame.game.quests.Quest
import com.legacy.fingame.game.quests.QuestEntry
import com.legacy.fingame.game.quests.QuestOption
import com.legacy.fingame.game.quests.QuestOutcome
import com.legacy.fingame.game.quests.QuestProgress
import com.legacy.fingame.game.quests.QuestStatus
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.screens.advanceButtonText
import com.legacy.fingame.ui.screens.coinsText
import com.legacy.fingame.ui.screens.countdownText
import com.legacy.fingame.ui.screens.expandedQuestStatusText
import com.legacy.fingame.ui.screens.hasWaitingStep
import com.legacy.fingame.ui.screens.minBalanceNoteText
import com.legacy.fingame.ui.screens.needCoinsText
import com.legacy.fingame.ui.screens.nextExpandedQuest
import com.legacy.fingame.ui.screens.optionLockText
import com.legacy.fingame.ui.screens.progressChangeText
import com.legacy.fingame.ui.screens.questImageOf
import com.legacy.fingame.ui.screens.questStatusText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Что написано на карточке квеста: статус, отсчёт, суммы и надписи кнопок. */
class QuestFormatTest {

    private val now = FakeGameClock.DEFAULT_MILLIS
    private val nbsp = " "

    private val toGames = QuestOutcome("Фрукты", "Вкусно.", "games")
    private val toEnd = QuestOutcome("Мяч", "Играли!", Quest.END_NODE)

    @Test
    fun `coins are counted the Russian way`() {
        assertEquals("1${nbsp}монета", coinsText(1))
        assertEquals("2${nbsp}монеты", coinsText(2))
        assertEquals("4${nbsp}монеты", coinsText(4))
        assertEquals("5${nbsp}монет", coinsText(5))
        assertEquals("11${nbsp}монет", coinsText(11))
        assertEquals("14${nbsp}монет", coinsText(14))
        assertEquals("21${nbsp}монета", coinsText(21))
        assertEquals("22${nbsp}монеты", coinsText(22))
        assertEquals("100${nbsp}монет", coinsText(100))
        assertEquals("111${nbsp}монет", coinsText(111))
        assertEquals("Нужно 100${nbsp}монет", needCoinsText(100))
        assertEquals(
            "Нужно 100${nbsp}монет в запасе${nbsp}— они не тратятся",
            minBalanceNoteText(100)
        )
    }

    @Test
    fun `the countdown rounds up to whole seconds`() {
        assertEquals("0:42", countdownText(42_000L))
        assertEquals("0:42", countdownText(41_001L))
        assertEquals("1:00", countdownText(60_000L))
        assertEquals("1:59", countdownText(119_000L))
        assertEquals("0:01", countdownText(1L))
        assertEquals("0:00", countdownText(0L))
        assertEquals("0:00", countdownText(-5L))
        assertEquals("1:02:03", countdownText(3_723_000L))
    }

    @Test
    fun `a quest nobody took says whether it can be taken`() {
        val entry = QuestEntry(TestQuests.PICNIC, null)

        assertEquals("Нужно 100${nbsp}монет", questStatusText(entry, balance = 99, nowMillis = now))
        assertEquals("Можно взять", questStatusText(entry, balance = 100, nowMillis = now))
    }

    @Test
    fun `a quest at a choice names the step`() {
        val first = QuestEntry(TestQuests.PICNIC, QuestProgress("picnic", "food", now))
        val second = QuestEntry(TestQuests.PICNIC, QuestProgress("picnic", "games", now))

        assertEquals("Шаг 1 из 2", questStatusText(first, 0, now))
        assertEquals("Шаг 2 из 2", questStatusText(second, 0, now))
    }

    @Test
    fun `a waiting quest counts down`() {
        val entry = QuestEntry(
            TestQuests.PICNIC,
            QuestProgress("picnic", "food", now + 42_000L, lastChoice = toGames)
        )

        assertEquals("Следующий шаг через 0:42", questStatusText(entry, 0, now))
    }

    @Test
    fun `once the wait is over the card says what can be done`() {
        val next = QuestEntry(TestQuests.PICNIC, QuestProgress("picnic", "food", now, lastChoice = toGames))
        val last = QuestEntry(TestQuests.PICNIC, QuestProgress("picnic", "games", now, lastChoice = toEnd))

        assertEquals("Следующий шаг готов", questStatusText(next, 0, now))
        assertEquals("Можно завершить", questStatusText(last, 0, now))
    }

    @Test
    fun `a finished quest shows its final progress when it has one`() {
        // Кулдаун снят копией квеста — тест смотрит только на «Завершён», не на отсчёт.
        val noCooldown = TestQuests.PICNIC.copy(cooldownMinutes = 0)
        val picnic = QuestEntry(
            noCooldown,
            QuestProgress("picnic", Quest.END_NODE, now, progress = 80, status = QuestStatus.FINISHED)
        )
        val piggy = QuestEntry(
            TestQuests.PIGGY_BANK.copy(cooldownMinutes = 0),
            QuestProgress("piggy_bank", Quest.END_NODE, now, status = QuestStatus.FINISHED)
        )

        assertEquals("Завершён${nbsp}· 80%", questStatusText(picnic, 0, now))
        assertEquals("Завершён", questStatusText(piggy, 0, now))
    }

    @Test
    fun `a finished quest on cooldown counts down to when it can be taken again`() {
        val entry = QuestEntry(
            TestQuests.PIGGY_BANK,
            QuestProgress("piggy_bank", Quest.END_NODE, now, status = QuestStatus.FINISHED)
        )
        val cooldownEnd = now + TestQuests.PIGGY_BANK.cooldownMinutes * 60_000L

        assertEquals("Доступен через 1:00:00", questStatusText(entry, 0, cooldownEnd - 3_600_000L))
        assertEquals("Доступен через 0:42", expandedQuestStatusText(entry, 0, cooldownEnd - 42_000L))
        // Кулдаун прошёл — снова «Завершён», а не бесконечный отсчёт.
        assertEquals("Завершён", questStatusText(entry, 0, cooldownEnd))
    }

    @Test
    fun `without the wait a finished quest on cooldown shows no countdown`() {
        val entry = QuestEntry(
            TestQuests.PIGGY_BANK,
            QuestProgress("piggy_bank", Quest.END_NODE, now, status = QuestStatus.FINISHED)
        )

        assertEquals("Завершён", questStatusText(entry, 0, now, ignoreDelays = true))
        assertNull(expandedQuestStatusText(entry, 0, now, ignoreDelays = true))
    }

    @Test
    fun `without the wait a step that was waiting is ready and the screen does not tick`() {
        val waiting = QuestEntry(
            TestQuests.PICNIC,
            QuestProgress("picnic", "food", now + 42_000L, lastChoice = toGames)
        )

        assertEquals("Следующий шаг готов", questStatusText(waiting, 0, now, ignoreDelays = true))
        assertFalse(hasWaitingStep(listOf(waiting), now, ignoreDelays = true))
    }

    @Test
    fun `a one-time quest that is done for good just says it is finished`() {
        val onceQuest = TestQuests.PIGGY_BANK.copy(repeatable = false)
        val entry = QuestEntry(
            onceQuest,
            QuestProgress("piggy_bank", Quest.END_NODE, now, status = QuestStatus.FINISHED)
        )

        assertEquals("Завершён", questStatusText(entry, 0, now + 1_000_000_000L))
        assertNull(expandedQuestStatusText(entry, 0, now + 1_000_000_000L))
    }

    @Test
    fun `the button under a result says where it leads`() {
        assertEquals("Дальше", advanceButtonText(toGames))
        assertEquals("Завершить", advanceButtonText(toEnd))
    }

    @Test
    fun `the progress change is signed`() {
        assertEquals("Прогресс +30%", progressChangeText(30))
        assertEquals("Прогресс -10%", progressChangeText(-10))
    }

    @Test
    fun `a missing picture falls back to the quests icon`() {
        assertEquals(Sprites.QUESTS, questImageOf(null) { true })
        assertEquals(Sprites.QUESTS, questImageOf("quests/picnic.webp") { false })
        assertEquals("quests/picnic.webp", questImageOf("quests/picnic.webp") { true })
    }

    @Test
    fun `an open card names the step instead of repeating the countdown or the finish`() {
        val waiting = QuestEntry(
            TestQuests.PICNIC,
            QuestProgress("picnic", "food", now + 42_000L, lastChoice = toGames)
        )
        val choosing = QuestEntry(TestQuests.PICNIC, QuestProgress("picnic", "games", now))
        // Кулдаун снят копией квеста — этот тест не про отсчёт до повтора, у него свой тест ниже.
        val finished = QuestEntry(
            TestQuests.PICNIC.copy(cooldownMinutes = 0),
            QuestProgress("picnic", Quest.END_NODE, now, progress = 80, status = QuestStatus.FINISHED)
        )
        val untaken = QuestEntry(TestQuests.PICNIC, null)

        assertEquals("Шаг 1 из 2", expandedQuestStatusText(waiting, 0, now))
        assertEquals("Шаг 2 из 2", expandedQuestStatusText(choosing, 0, now))
        assertNull(expandedQuestStatusText(finished, 0, now))
        assertEquals("Нужно 100${nbsp}монет", expandedQuestStatusText(untaken, 99, now))
    }

    @Test
    fun `the screen waits only while an active quest has a step ahead`() {
        val waiting = QuestEntry(
            TestQuests.PICNIC,
            QuestProgress("picnic", "food", now + 42_000L, lastChoice = toGames)
        )
        val ready = QuestEntry(TestQuests.PICNIC, QuestProgress("picnic", "food", now, lastChoice = toGames))
        val finished = QuestEntry(
            TestQuests.PICNIC,
            QuestProgress("picnic", Quest.END_NODE, now + 42_000L, status = QuestStatus.FINISHED)
        )
        val untaken = QuestEntry(TestQuests.PIGGY_BANK, null)

        assertTrue(hasWaitingStep(listOf(untaken, waiting), now))
        assertFalse(hasWaitingStep(listOf(waiting), now + 42_000L))
        assertFalse(hasWaitingStep(listOf(ready, finished, untaken), now))
        assertFalse(hasWaitingStep(emptyList(), now))
    }

    @Test
    fun `tapping a card opens it, again closes it, another switches`() {
        assertEquals("picnic", nextExpandedQuest(null, "picnic"))
        assertNull(nextExpandedQuest("picnic", "picnic"))
        assertEquals("guests", nextExpandedQuest("picnic", "guests"))
    }

    @Test
    fun `an option that costs more than the balance says how much is needed`() {
        val cake = QuestOption("Купить торт", "Торт.", "tidy", moneyDelta = -30)
        val tea = QuestOption("Позвать на чай", "Чай.", "tidy")
        val gift = QuestOption("Открыть", "Монеты!", Quest.END_NODE, moneyDelta = 20)

        assertEquals("Нужно 30${nbsp}монет", optionLockText(cake, balance = 29))
        assertEquals("Нужно 30${nbsp}монет", optionLockText(cake, balance = 0))
        assertNull(optionLockText(cake, balance = 30))
        assertNull(optionLockText(tea, balance = 0))
        assertNull(optionLockText(gift, balance = 0))
    }
}
