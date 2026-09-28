package com.legacy.fingame

import com.legacy.fingame.game.quests.Quest
import com.legacy.fingame.game.quests.QuestAvailability
import com.legacy.fingame.game.quests.QuestCatalog
import com.legacy.fingame.game.quests.QuestCheck
import com.legacy.fingame.game.quests.QuestEngine
import com.legacy.fingame.game.quests.QuestOutcome
import com.legacy.fingame.game.quests.QuestProgress
import com.legacy.fingame.game.quests.QuestStatus
import com.legacy.fingame.game.quests.QuestUnavailableReason
import com.legacy.fingame.game.stats.StatKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Правила квестов сами по себе, без модели и без часов: только данные на входе и на выходе. */
class QuestEngineTest {

    private val now = FakeGameClock.DEFAULT_MILLIS
    private val minute = TimeUnit.MINUTES.toMillis(1)
    private val sixHours = TimeUnit.HOURS.toMillis(6)

    /** @return Состояние, в котором [quest] только что взят при большом запасе монет. */
    private fun started(quest: Quest): List<QuestProgress> =
        QuestEngine.start(quest, emptyList(), balance = 1_000, nowMillis = now)!!

    // --- Взять квест ---

    @Test
    fun `a quest cannot be taken without its minimum on the balance`() {
        assertFalse(QuestEngine.canStart(TestQuests.PICNIC, emptyList(), balance = 99, nowMillis = now))
        assertNull(QuestEngine.start(TestQuests.PICNIC, emptyList(), balance = 99, nowMillis = now))
    }

    @Test
    fun `exactly the minimum is enough, and the quest starts at its first node`() {
        val quests = QuestEngine.start(TestQuests.PICNIC, emptyList(), balance = 100, nowMillis = now)

        assertEquals(listOf(QuestProgress("picnic", "food", now)), quests)
    }

    @Test
    fun `a running quest cannot be taken a second time`() {
        val quests = started(TestQuests.PICNIC)

        assertFalse(QuestEngine.canStart(TestQuests.PICNIC, quests, balance = 1_000, nowMillis = now + minute))
        assertNull(QuestEngine.start(TestQuests.PICNIC, quests, 1_000, now + minute))
    }

    @Test
    fun `a finished quest starts over from scratch once its cooldown is over`() {
        // Кулдаун снят копией квеста — тест проверяет только сброс прогресса и узла, не время.
        val quest = TestQuests.PICNIC.copy(cooldownMinutes = 0)
        val finished = QuestProgress(
            questId = "picnic",
            nodeId = Quest.END_NODE,
            availableAtMillis = now,
            progress = 80,
            status = QuestStatus.FINISHED
        )

        val quests = QuestEngine.start(quest, listOf(finished), 1_000, now + minute)

        assertEquals(listOf(QuestProgress("picnic", "food", now + minute)), quests)
    }

    // --- Кулдаун и одноразовые квесты ---

    @Test
    fun `a repeatable quest is on cooldown right after it finishes`() {
        val finished = QuestProgress("picnic", Quest.END_NODE, availableAtMillis = now, status = QuestStatus.FINISHED)

        val availability = QuestEngine.availabilityOf(TestQuests.PICNIC, listOf(finished), 1_000, now)

        assertFalse(availability.canStart)
        assertEquals(QuestUnavailableReason.COOLDOWN, availability.reason)
        assertEquals(
            now + TimeUnit.MINUTES.toMillis(TestQuests.PICNIC.cooldownMinutes.toLong()),
            availability.availableAtMillis
        )
        assertNull(QuestEngine.start(TestQuests.PICNIC, listOf(finished), 1_000, now))
    }

    @Test
    fun `a repeatable quest can be taken again once its cooldown passes`() {
        val finished = QuestProgress("picnic", Quest.END_NODE, availableAtMillis = now, status = QuestStatus.FINISHED)
        val cooldownEnd = now + TimeUnit.MINUTES.toMillis(TestQuests.PICNIC.cooldownMinutes.toLong())

        assertNull(QuestEngine.start(TestQuests.PICNIC, listOf(finished), 1_000, cooldownEnd - 1))
        assertTrue(QuestEngine.canStart(TestQuests.PICNIC, listOf(finished), 1_000, cooldownEnd))
        assertEquals(
            listOf(QuestProgress("picnic", "food", cooldownEnd)),
            QuestEngine.start(TestQuests.PICNIC, listOf(finished), 1_000, cooldownEnd)
        )
    }

    @Test
    fun `a quest that is not repeatable stays locked no matter how long it waits`() {
        val quest = TestQuests.PICNIC.copy(repeatable = false)
        val finished = QuestProgress("picnic", Quest.END_NODE, availableAtMillis = now, status = QuestStatus.FINISHED)

        val availability = QuestEngine.availabilityOf(quest, listOf(finished), 1_000, now + TimeUnit.DAYS.toMillis(30))

        assertFalse(availability.canStart)
        assertEquals(QuestUnavailableReason.ONE_TIME_DONE, availability.reason)
        assertNull(availability.availableAtMillis)
    }

    @Test
    fun `enabling a finished quest again lifts both the one-time lock and the cooldown`() {
        val quest = TestQuests.PICNIC.copy(repeatable = false)
        val finished = QuestProgress("picnic", Quest.END_NODE, availableAtMillis = now, status = QuestStatus.FINISHED)

        val enabled = QuestEngine.enable(listOf(finished), "picnic")!!

        assertTrue(enabled.single().enabledAgain)
        assertTrue(QuestEngine.canStart(quest, enabled, 1_000, now))
        val started = QuestEngine.start(quest, enabled, 1_000, now)!!
        // Начатый заново квест — свежая запись без старого флага включения.
        assertFalse(started.single().enabledAgain)
    }

    @Test
    fun `enabling a quest that never finished does nothing`() {
        assertNull(QuestEngine.enable(emptyList(), "picnic"))
        assertNull(QuestEngine.enable(started(TestQuests.PICNIC), "picnic"))
    }

    @Test
    fun `a random quest still on its own cooldown does not come up, even once six hours pass`() {
        val onCooldown = QuestProgress(
            "lost_wallet", Quest.END_NODE, availableAtMillis = now, status = QuestStatus.FINISHED
        )
        val cooldownEnd = now + TimeUnit.MINUTES.toMillis(TestQuests.WALLET.cooldownMinutes.toLong())
        val catalog = QuestCatalog.of(listOf(TestQuests.WALLET))

        // sinceMillis достаточно в прошлом, чтобы общие шесть часов между случайными квестами уже
        // прошли — единственная причина отказа тут должна быть кулдаун самого кошелька.
        val spawn = QuestEngine.maybeSpawnRandom(
            catalog, listOf(onCooldown), 0, sinceMillis = now - sixHours,
            nowMillis = cooldownEnd - 1, random = ScriptedRandom()
        )

        assertNull(spawn)
    }

    @Test
    fun `a random quest done for good does not come up, but another one still can`() {
        val doneForGood = QuestProgress(
            "lost_wallet", Quest.END_NODE, availableAtMillis = now, status = QuestStatus.FINISHED
        )
        val catalog = QuestCatalog.of(listOf(TestQuests.WALLET.copy(repeatable = false), TestQuests.GUESTS))

        val spawn = QuestEngine.maybeSpawnRandom(
            catalog, listOf(doneForGood), 0, now, now + sixHours, ScriptedRandom(0, 0)
        )!!

        assertEquals(TestQuests.GUESTS, spawn.quest)
    }

    @Test
    fun `without the wait a quest on cooldown can be taken right away`() {
        val finished = QuestProgress("picnic", Quest.END_NODE, availableAtMillis = now, status = QuestStatus.FINISHED)

        val availability =
            QuestEngine.availabilityOf(TestQuests.PICNIC, listOf(finished), 1_000, now, ignoreDelays = true)

        assertEquals(QuestAvailability(canStart = true), availability)
        assertEquals(
            listOf(QuestProgress("picnic", "food", now)),
            QuestEngine.start(TestQuests.PICNIC, listOf(finished), 1_000, now, ignoreDelays = true)
        )
    }

    @Test
    fun `without the wait a one-time quest still waits for an adult, and the minimum still counts`() {
        val onceQuest = TestQuests.PICNIC.copy(repeatable = false)
        val finished = QuestProgress("picnic", Quest.END_NODE, availableAtMillis = now, status = QuestStatus.FINISHED)

        assertEquals(
            QuestUnavailableReason.ONE_TIME_DONE,
            QuestEngine.availabilityOf(onceQuest, listOf(finished), 1_000, now, ignoreDelays = true).reason
        )
        assertEquals(
            QuestUnavailableReason.NOT_ENOUGH_MONEY,
            QuestEngine.availabilityOf(TestQuests.PICNIC, listOf(finished), 99, now, ignoreDelays = true).reason
        )
    }

    // --- Выбрать вариант ---

    @Test
    fun `a choice keeps its outcome, moves the progress and starts the wait`() {
        val choice = QuestEngine.choose(
            TestQuests.PICNIC, started(TestQuests.PICNIC), optionIndex = 0, balance = 150, nowMillis = now
        )!!

        val outcome = choice.outcome
        assertEquals("Фрукты", outcome.optionLabel)
        assertEquals("Вкусно и полезно.", outcome.resultText)
        assertEquals("games", outcome.nextNodeId)
        assertEquals(mapOf(StatKind.HUNGER to 20, StatKind.HEALTH to 5), outcome.statEffects)
        assertEquals(-40, outcome.moneyDelta)
        assertEquals(60, outcome.progressDelta)

        val progress = choice.quests.single()
        assertEquals("food", progress.nodeId)
        assertEquals(60, progress.progress)
        assertEquals(now + minute, progress.availableAtMillis)
        assertEquals(outcome, progress.lastChoice)
        assertEquals(QuestStatus.ACTIVE, progress.status)
    }

    @Test
    fun `progress never goes past 100`() {
        var quests = QuestEngine.choose(TestQuests.PICNIC, started(TestQuests.PICNIC), 0, 150, now)!!.quests
        quests = QuestEngine.advance(TestQuests.PICNIC, quests, now + minute)!!

        val last = QuestEngine.choose(TestQuests.PICNIC, quests, 0, 150, now + minute)!!

        assertEquals(Quest.MAX_PROGRESS, last.quests.single().progress)
        assertEquals(40, last.outcome.progressDelta)
    }

    @Test
    fun `a quest without progress keeps it at zero`() {
        val noProgress = TestQuests.PICNIC.copy(hasProgress = false)

        val choice = QuestEngine.choose(noProgress, started(noProgress), 0, 150, now)!!

        assertEquals(0, choice.quests.single().progress)
        assertEquals(0, choice.outcome.progressDelta)
    }

    @Test
    fun `an option the player cannot afford is not chosen`() {
        val quests = started(TestQuests.ICE_CREAM)

        assertNull(QuestEngine.choose(TestQuests.ICE_CREAM, quests, 0, 39, now))
        assertNull(QuestEngine.choose(TestQuests.ICE_CREAM, quests, 0, 0, now))
        assertFalse(QuestEngine.canAfford(TestQuests.ICE_CREAM.node("shop")!!.options[0], 39))
        assertTrue(QuestEngine.canAfford(TestQuests.ICE_CREAM.node("shop")!!.options[0], 40))
        val paid = QuestEngine.choose(TestQuests.ICE_CREAM, quests, 0, 40, now)!!
        assertEquals(-40, paid.outcome.moneyDelta)
    }

    @Test
    fun `a free option or income is affordable with an empty balance`() {
        val free = TestQuests.PICNIC.node("food")!!.options[1]
        val income = TestQuests.PIGGY_BANK.node("start")!!.options[0]

        assertTrue(QuestEngine.canAfford(free, 0))
        assertTrue(QuestEngine.canAfford(income, 0))
    }

    @Test
    fun `income is never cut`() {
        val choice = QuestEngine.choose(TestQuests.PIGGY_BANK, started(TestQuests.PIGGY_BANK), 0, 0, now)!!

        assertEquals(30, choice.outcome.moneyDelta)
    }

    @Test
    fun `a second tap while the result is shown chooses nothing`() {
        val quests = QuestEngine.choose(TestQuests.PICNIC, started(TestQuests.PICNIC), 0, 150, now)!!.quests

        assertNull(QuestEngine.choose(TestQuests.PICNIC, quests, 1, 150, now))
    }

    @Test
    fun `an option that is not there chooses nothing`() {
        val quests = started(TestQuests.PICNIC)

        assertNull(QuestEngine.choose(TestQuests.PICNIC, quests, 5, 150, now))
        assertNull(QuestEngine.choose(TestQuests.PICNIC, quests, -1, 150, now))
    }

    @Test
    fun `a quest that was not taken cannot be played`() {
        assertNull(QuestEngine.choose(TestQuests.PICNIC, emptyList(), 0, 150, now))
        assertNull(QuestEngine.advance(TestQuests.PICNIC, emptyList(), now))
    }

    // --- Дальше ---

    @Test
    fun `the next step waits for the delay of the node`() {
        val quests = QuestEngine.choose(TestQuests.PICNIC, started(TestQuests.PICNIC), 0, 150, now)!!.quests

        assertNull(QuestEngine.advance(TestQuests.PICNIC, quests, now + minute - 1))

        val next = QuestEngine.advance(TestQuests.PICNIC, quests, now + minute)!!.single()
        assertEquals(QuestProgress("picnic", "games", now + minute, progress = 60), next)
    }

    @Test
    fun `without the wait the next step opens right after the choice`() {
        val choice = QuestEngine.choose(
            TestQuests.PICNIC, started(TestQuests.PICNIC), 0, 150, now, ignoreDelays = true
        )!!
        assertEquals(now, choice.quests.single().availableAtMillis)

        val next = QuestEngine.advance(TestQuests.PICNIC, choice.quests, now, ignoreDelays = true)!!.single()

        assertEquals(QuestProgress("picnic", "games", now, progress = 60), next)
    }

    @Test
    fun `without the wait a step that was already waiting opens right away`() {
        // Выбор сделан ещё с ожиданием, а флаг включился потом: ждать всё равно не нужно.
        val quests = QuestEngine.choose(TestQuests.PICNIC, started(TestQuests.PICNIC), 0, 150, now)!!.quests

        assertNull(QuestEngine.advance(TestQuests.PICNIC, quests, now))
        assertEquals(
            "games",
            QuestEngine.advance(TestQuests.PICNIC, quests, now, ignoreDelays = true)!!.single().nodeId
        )
    }

    @Test
    fun `there is nothing to go on to before a choice is made`() {
        assertNull(QuestEngine.advance(TestQuests.PICNIC, started(TestQuests.PICNIC), now + sixHours))
    }

    @Test
    fun `the last choice finishes without a wait`() {
        val quests = QuestEngine.choose(TestQuests.PIGGY_BANK, started(TestQuests.PIGGY_BANK), 0, 0, now)!!.quests
        assertEquals(now, quests.single().availableAtMillis)

        val finished = QuestEngine.advance(TestQuests.PIGGY_BANK, quests, now)!!.single()

        assertEquals(QuestStatus.FINISHED, finished.status)
        assertEquals(Quest.END_NODE, finished.nodeId)
        assertNull(finished.lastChoice)
    }

    @Test
    fun `a final choice in a delayed step needs no wait`() {
        val delayedGames = TestQuests.PICNIC.nodes.getValue("games").copy(delayMinutes = 5)
        val quest = TestQuests.PICNIC.copy(nodes = TestQuests.PICNIC.nodes + ("games" to delayedGames))

        var quests = QuestEngine.choose(quest, started(quest), 0, 150, now)!!.quests
        quests = QuestEngine.advance(quest, quests, now + minute)!!

        val choice = QuestEngine.choose(quest, quests, 0, 150, now + minute)!!
        assertEquals(now + minute, choice.quests.single().availableAtMillis)

        val finished = QuestEngine.advance(quest, choice.quests, now + minute)!!.single()
        assertEquals(QuestStatus.FINISHED, finished.status)
    }

    @Test
    fun `a step the data no longer has finishes the quest instead of getting stuck`() {
        val quests = QuestEngine.choose(TestQuests.PICNIC, started(TestQuests.PICNIC), 0, 150, now)!!.quests
        val trimmed = TestQuests.PICNIC.copy(nodes = TestQuests.PICNIC.nodes - "games")

        val finished = QuestEngine.advance(trimmed, quests, now + minute)!!.single()

        assertEquals(QuestStatus.FINISHED, finished.status)
        assertEquals(60, finished.progress)
    }

    @Test
    fun `a quest whose current step vanished from the data finishes on advance`() {
        val quests = started(TestQuests.PICNIC)
        val trimmed = TestQuests.PICNIC.copy(nodes = TestQuests.PICNIC.nodes - "food")

        val finished = QuestEngine.advance(trimmed, quests, now)!!.single()

        assertEquals(QuestStatus.FINISHED, finished.status)
        assertEquals(Quest.END_NODE, finished.nodeId)
        assertNull(finished.lastChoice)

        // Пройденный квест, даже так, не должен держать случайные квесты заблокированными.
        val spawn = QuestEngine.maybeSpawnRandom(
            TestQuests.CATALOG, listOf(finished), 0, now, now + sixHours, ScriptedRandom(0, 1)
        )!!

        assertEquals(TestQuests.GUESTS, spawn.quest)
    }

    // --- Случайные квесты ---

    @Test
    fun `no random quest before six hours have passed`() {
        val spawn = QuestEngine.maybeSpawnRandom(
            TestQuests.CATALOG, emptyList(), balance = 0,
            sinceMillis = now, nowMillis = now + sixHours - 1, random = ScriptedRandom()
        )

        assertNull(spawn)
    }

    @Test
    fun `after six hours a lucky roll starts a random quest`() {
        val random = ScriptedRandom(0, 1)

        val spawn = QuestEngine.maybeSpawnRandom(
            TestQuests.CATALOG, emptyList(), 0, now, now + sixHours, random
        )!!

        assertEquals(TestQuests.GUESTS, spawn.quest)
        assertEquals(listOf(QuestProgress("guests", "treat", now + sixHours)), spawn.quests)
        // Сначала шанс один к четырём, потом выбор из двух случайных квестов каталога.
        assertEquals(listOf(QuestEngine.RANDOM_QUEST_CHANCE, 2), random.bounds)
    }

    @Test
    fun `an unlucky roll starts nothing`() {
        val spawn = QuestEngine.maybeSpawnRandom(
            TestQuests.CATALOG, emptyList(), 0, now, now + sixHours, ScriptedRandom(1)
        )

        assertNull(spawn)
    }

    @Test
    fun `no second random quest while one is running`() {
        val running = QuestEngine.start(TestQuests.WALLET, emptyList(), 0, now)!!

        val spawn = QuestEngine.maybeSpawnRandom(
            TestQuests.CATALOG, running, 0, now - sixHours, now + sixHours, ScriptedRandom()
        )

        assertNull(spawn)
    }

    @Test
    fun `a running quest of the player does not hold random ones back`() {
        val running = started(TestQuests.PIGGY_BANK)

        val spawn = QuestEngine.maybeSpawnRandom(
            TestQuests.CATALOG, running, 0, now, now + sixHours, ScriptedRandom(0, 0)
        )!!

        assertEquals(TestQuests.WALLET, spawn.quest)
        assertEquals(listOf("piggy_bank", "lost_wallet"), spawn.quests.map { it.questId })
    }

    @Test
    fun `a random quest the player cannot afford does not come up`() {
        val catalog = QuestCatalog.of(listOf(TestQuests.WALLET.copy(minBalance = 50), TestQuests.GUESTS))

        val spawn = QuestEngine.maybeSpawnRandom(
            catalog, emptyList(), balance = 10, sinceMillis = now, nowMillis = now + sixHours,
            random = ScriptedRandom(0, 0)
        )!!

        assertEquals(TestQuests.GUESTS, spawn.quest)
    }

    @Test
    fun `a finished random quest can come up again`() {
        val finished = QuestProgress(
            "lost_wallet", Quest.END_NODE, now, status = QuestStatus.FINISHED
        )
        val catalog = QuestCatalog.of(listOf(TestQuests.WALLET))

        val spawn = QuestEngine.maybeSpawnRandom(
            catalog, listOf(finished), 0, now, now + sixHours, ScriptedRandom(0, 0)
        )!!

        assertEquals(listOf(QuestProgress("lost_wallet", "found", now + sixHours)), spawn.quests)
    }

    @Test
    fun `a clock behind the last random quest starts nothing`() {
        val spawn = QuestEngine.maybeSpawnRandom(
            TestQuests.CATALOG, emptyList(), 0, sinceMillis = now, nowMillis = now - 1,
            random = ScriptedRandom()
        )

        assertNull(spawn)
    }

    // --- Непросмотренный шаг ---

    @Test
    fun `a step that became available after the last look is unseen`() {
        val quests = listOf(QuestProgress("guests", "treat", availableAtMillis = now))

        assertTrue(QuestEngine.hasUnseenStep(quests, seenAtMillis = now - 1, nowMillis = now))
        assertFalse(QuestEngine.hasUnseenStep(quests, seenAtMillis = now, nowMillis = now))
    }

    @Test
    fun `a step still waiting is not news yet`() {
        val quests = listOf(
            QuestProgress(
                "picnic", "food", availableAtMillis = now + minute,
                lastChoice = QuestOutcome("Фрукты", "Вкусно.", "games")
            )
        )

        assertFalse(QuestEngine.hasUnseenStep(quests, seenAtMillis = now - 1, nowMillis = now))
        assertTrue(QuestEngine.hasUnseenStep(quests, seenAtMillis = now - 1, nowMillis = now + minute))
    }

    @Test
    fun `a finished quest is never news`() {
        val quests = listOf(
            QuestProgress("guests", Quest.END_NODE, now, status = QuestStatus.FINISHED)
        )

        assertFalse(QuestEngine.hasUnseenStep(quests, seenAtMillis = now - 1, nowMillis = now))
    }
    // --- Проверка взрослым ---

    private val checked = TestQuests.PICNIC.copy(requiresAdultCheck = true)

    @Test
    fun `a choice in a checked quest waits for the adult and gives nothing yet`() {
        val choice = QuestEngine.choose(checked, started(checked), 0, balance = 1_000, nowMillis = now)!!

        assertTrue(choice.awaitingCheck)
        val progress = choice.quests.single()
        assertEquals(QuestCheck.WAITING, progress.check)
        assertTrue(progress.isAwaitingCheck)
        assertEquals(0, progress.progress)
        assertEquals("Фрукты", progress.lastChoice?.optionLabel)
        // Пока ждёт проверки — ни дальше, ни второго выбора.
        assertNull(QuestEngine.advance(checked, choice.quests, now + sixHours))
        assertNull(QuestEngine.choose(checked, choice.quests, 1, balance = 1_000, nowMillis = now))
        assertEquals(listOf(progress), QuestEngine.awaitingCheck(choice.quests))
    }

    @Test
    fun `approving gives the reward and opens the next step after the node delay`() {
        val sent = QuestEngine.choose(checked, started(checked), 0, balance = 1_000, nowMillis = now)!!.quests

        val approved = QuestEngine.approve(checked, sent, balance = 1_000, nowMillis = now + sixHours)!!

        assertFalse(approved.awaitingCheck)
        assertEquals(-40, approved.outcome.moneyDelta)
        assertEquals(60, approved.outcome.progressDelta)
        val progress = approved.quests.single()
        assertEquals(QuestCheck.NONE, progress.check)
        assertEquals(60, progress.progress)
        assertEquals(now + sixHours + minute, progress.availableAtMillis)
        assertNull(QuestEngine.advance(checked, approved.quests, now + sixHours))
        assertEquals("games", QuestEngine.advance(checked, approved.quests, now + sixHours + minute)!!.single().nodeId)
        assertNull(QuestEngine.approve(checked, approved.quests, balance = 1_000, nowMillis = now + sixHours))
    }

    @Test
    fun `an approved spending is cut to the balance of the moment`() {
        val sent = QuestEngine.choose(checked, started(checked), 0, balance = 1_000, nowMillis = now)!!.quests

        val approved = QuestEngine.approve(checked, sent, balance = 25, nowMillis = now)!!

        assertEquals(-25, approved.outcome.moneyDelta)
    }

    @Test
    fun `rejecting returns the step to work without a reward`() {
        val sent = QuestEngine.choose(checked, started(checked), 0, balance = 1_000, nowMillis = now)!!.quests

        val rejected = QuestEngine.reject(sent, "picnic", now + minute)!!

        val progress = rejected.single()
        assertEquals(QuestCheck.REJECTED, progress.check)
        assertNull(progress.lastChoice)
        assertEquals("food", progress.nodeId)
        assertEquals(0, progress.progress)
        assertNull(QuestEngine.reject(rejected, "picnic", now + minute))
        // Этап снова в работе: можно выбрать ещё раз, и он опять уйдёт на проверку.
        val again = QuestEngine.choose(checked, rejected, 1, balance = 1_000, nowMillis = now + minute)!!
        assertTrue(again.awaitingCheck)
        assertEquals(QuestCheck.WAITING, again.quests.single().check)
    }

    @Test
    fun `a quest without the check is counted at once`() {
        val choice = QuestEngine.choose(TestQuests.PICNIC, started(TestQuests.PICNIC), 0, 1_000, now)!!

        assertFalse(choice.awaitingCheck)
        assertEquals(QuestCheck.NONE, choice.quests.single().check)
        assertNull(QuestEngine.approve(TestQuests.PICNIC, choice.quests, 1_000, now))
        assertNull(QuestEngine.reject(choice.quests, "picnic", now))
    }
}
