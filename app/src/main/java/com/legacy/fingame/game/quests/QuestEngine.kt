package com.legacy.fingame.game.quests

import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Правила квестов: взять, выбрать вариант, перейти дальше, выпасть случайно.
 *
 * Здесь нет ни часов, ни денег игрока, ни сохранения: каждая функция получает текущее состояние
 * квестов, баланс и момент времени и возвращает новое состояние — или null, когда действие сейчас
 * невозможно. Применить исход к питомцу и журналу денег — дело
 * [com.legacy.fingame.game.GameViewModel].
 *
 * Флаг `ignoreDelays` у функций снимает ожидание: кулдаун пройденного квеста
 * ([Quest.cooldownMinutes]) и паузу между шагами ([QuestNode.delayMinutes]) — всё доступно сразу.
 * Это для отладочной сборки, чтобы проверять квесты без ожидания; откуда он берётся, решает
 * вызывающий, само ядро о сборке ничего не знает. Шесть часов между случайными квестами флаг не
 * трогает: это не ожидание действия игрока, а редкость случайных событий.
 */
object QuestEngine {

    /** Сколько игровых часов должно пройти с последнего случайного квеста до следующего. */
    val RANDOM_QUEST_COOLDOWN_MILLIS: Long = TimeUnit.HOURS.toMillis(6)

    /** Случайный квест выпадает с вероятностью один к стольким на каждую проверку. */
    const val RANDOM_QUEST_CHANCE = 4

    /** Длина игровой минуты — единицы [QuestNode.delayMinutes]. */
    private val MINUTE_MILLIS: Long = TimeUnit.MINUTES.toMillis(1)

    /**
     * Что получилось из выбора варианта.
     *
     * @property quests новое состояние квестов.
     * @property outcome исход выбора — его надо применить к питомцу и деньгам, если только он не
     * [awaitingCheck].
     * @property awaitingCheck выбор сдан на проверку взрослому ([Quest.requiresAdultCheck]): исход
     * пока НЕ применяется — это сделает [approve].
     */
    data class Choice(
        val quests: List<QuestProgress>,
        val outcome: QuestOutcome,
        val awaitingCheck: Boolean = false
    )

    /**
     * Случайный квест, который только что выпал.
     *
     * @property quests новое состояние квестов, в котором он уже начат.
     * @property quest какой квест выпал.
     */
    data class Spawn(val quests: List<QuestProgress>, val quest: Quest)

    /**
     * @return Состояние квеста [questId], или null, когда он не начинался.
     */
    fun progressOf(quests: List<QuestProgress>, questId: String): QuestProgress? =
        quests.find { it.questId == questId }

    /**
     * @return Можно ли начать [quest] сейчас; см. [availabilityOf] для причины, когда нельзя.
     */
    fun canStart(
        quest: Quest,
        quests: List<QuestProgress>,
        balance: Int,
        nowMillis: Long,
        ignoreDelays: Boolean = false
    ): Boolean = availabilityOf(quest, quests, balance, nowMillis, ignoreDelays).canStart

    /**
     * @param progress запись идущего квеста.
     * @param ignoreDelays снять паузу между шагами (см. описание [QuestEngine]).
     * @return Момент, с которого доступен следующий шаг: [QuestProgress.availableAtMillis], а без
     * ожидания — не позже [nowMillis], даже если пауза была назначена раньше, до включения флага.
     */
    fun stepAvailableAtMillis(progress: QuestProgress, nowMillis: Long, ignoreDelays: Boolean): Long =
        if (ignoreDelays) minOf(progress.availableAtMillis, nowMillis) else progress.availableAtMillis

    /**
     * Единственное место, где решается, можно ли начать квест: идёт ли он уже, хватает ли денег,
     * не заблокирован ли он навсегда как одноразовый и не остывает ли ещё после кулдауна.
     * [QuestProgress.enabledAgain] снимает обе последние проверки разом — это то, что делает
     * [enable].
     *
     * @param ignoreDelays не ждать кулдауна (см. описание [QuestEngine]); одноразовый квест всё
     * равно остаётся закрытым до [enable].
     * @return Ответ и причина отказа, если начать нельзя.
     */
    fun availabilityOf(
        quest: Quest,
        quests: List<QuestProgress>,
        balance: Int,
        nowMillis: Long,
        ignoreDelays: Boolean = false
    ): QuestAvailability {
        val progress = progressOf(quests, quest.id)
        if (progress?.isActive == true) {
            return QuestAvailability(canStart = false, reason = QuestUnavailableReason.ACTIVE)
        }
        if (progress != null && progress.isFinished && !progress.enabledAgain) {
            if (!quest.repeatable) {
                return QuestAvailability(canStart = false, reason = QuestUnavailableReason.ONE_TIME_DONE)
            }
            val cooldownEndsAtMillis = progress.availableAtMillis + quest.cooldownMinutes * MINUTE_MILLIS
            if (!ignoreDelays && nowMillis < cooldownEndsAtMillis) {
                return QuestAvailability(
                    canStart = false,
                    reason = QuestUnavailableReason.COOLDOWN,
                    availableAtMillis = cooldownEndsAtMillis
                )
            }
        }
        if (balance < quest.minBalance) {
            return QuestAvailability(canStart = false, reason = QuestUnavailableReason.NOT_ENOUGH_MONEY)
        }
        return QuestAvailability(canStart = true)
    }

    /**
     * Взрослый включает пройденный квест снова: снимает и одноразовую блокировку, и кулдаун —
     * ровно на один следующий раз, пока квест не начнётся и [QuestProgress.enabledAgain] не
     * потеряется вместе со старой записью (см. [start]).
     *
     * @return Новое состояние, или null, когда квест не начинался или ещё идёт — включать нечего.
     */
    fun enable(quests: List<QuestProgress>, questId: String): List<QuestProgress>? {
        val progress = progressOf(quests, questId) ?: return null
        if (!progress.isFinished) return null
        return quests.replaced(progress.copy(enabledAgain = true))
    }

    /**
     * @param option вариант на шаге квеста.
     * @param balance текущий счёт игрока.
     * @return Хватает ли монет на этот вариант: доход и бесплатный вариант доступны всегда, трата —
     * только когда она не больше счёта.
     */
    fun canAfford(option: QuestOption, balance: Int): Boolean =
        option.moneyDelta >= 0 || -option.moneyDelta <= balance

    /**
     * Начинает квест с первого узла; минимальная сумма не тратится.
     *
     * @return Новое состояние квестов, или null, когда начать нельзя (см. [canStart]).
     */
    fun start(
        quest: Quest,
        quests: List<QuestProgress>,
        balance: Int,
        nowMillis: Long,
        ignoreDelays: Boolean = false
    ): List<QuestProgress>? {
        if (!canStart(quest, quests, balance, nowMillis, ignoreDelays)) return null
        val started = QuestProgress(
            questId = quest.id,
            nodeId = quest.firstNodeId,
            availableAtMillis = nowMillis
        )
        return quests.filterNot { it.questId == quest.id } + started
    }

    /**
     * Выбор варианта на текущем узле. Вариант, на который не хватает монет ([canAfford]), не
     * выбирается; трата всё равно урезается до баланса — на случай, если счёт поменялся между
     * проверкой и списанием, ниже нуля он не уходит. Прогресс держится в 0..100 и двигается только
     * у квестов с прогрессом. Следующий шаг откроется через задержку узла; после последнего
     * варианта ждать нечего; с [ignoreDelays] не ждать и между шагами.
     *
     * У квеста с [Quest.requiresAdultCheck] выбор — это «Готово» ребёнка: этап уходит на проверку
     * ([QuestCheck.WAITING]), прогресс не двигается, а исход возвращается с
     * [Choice.awaitingCheck] — применять его рано, это сделает [approve].
     *
     * @param optionIndex номер варианта на узле.
     * @param balance текущий счёт игрока.
     * @return Новое состояние и исход, или null, когда выбирать сейчас нечего: квест не идёт, выбор
     * уже сделан и показан результат, узел ещё не открылся, варианта с таким номером нет или на
     * него не хватает монет.
     */
    fun choose(
        quest: Quest,
        quests: List<QuestProgress>,
        optionIndex: Int,
        balance: Int,
        nowMillis: Long,
        ignoreDelays: Boolean = false
    ): Choice? {
        val current = progressOf(quests, quest.id) ?: return null
        if (!current.isActive || current.lastChoice != null) return null
        if (nowMillis < stepAvailableAtMillis(current, nowMillis, ignoreDelays)) return null
        val node = quest.node(current.nodeId) ?: return null
        val option = node.options.getOrNull(optionIndex) ?: return null
        if (!canAfford(option, balance)) return null

        val moneyDelta = if (option.moneyDelta < 0) {
            option.moneyDelta.coerceAtLeast(-balance.coerceAtLeast(0))
        } else {
            option.moneyDelta
        }
        val progress = if (quest.hasProgress) {
            (current.progress + option.progressDelta).coerceIn(Quest.MIN_PROGRESS, Quest.MAX_PROGRESS)
        } else {
            current.progress
        }
        val outcome = QuestOutcome(
            optionLabel = option.label,
            resultText = option.resultText,
            nextNodeId = option.nextNodeId,
            statEffects = option.statEffects,
            moneyDelta = moneyDelta,
            progressDelta = progress - current.progress
        )
        if (quest.requiresAdultCheck) {
            val sent = current.copy(
                availableAtMillis = nowMillis,
                lastChoice = outcome,
                check = QuestCheck.WAITING
            )
            return Choice(quests.replaced(sent), outcome, awaitingCheck = true)
        }
        val waitMillis = if (outcome.isFinal || ignoreDelays) 0L else node.delayMinutes * MINUTE_MILLIS
        val updated = current.copy(
            progress = progress,
            availableAtMillis = nowMillis + waitMillis,
            lastChoice = outcome,
            check = QuestCheck.NONE
        )
        return Choice(quests.replaced(updated), outcome)
    }

    /**
     * Взрослый «Засчитать»: сданный этап засчитывается, как если бы выбор был сделан сейчас —
     * прогресс двигается, трата урезается до нынешнего баланса, следующий шаг откроется через
     * задержку узла. Дальше ребёнок видит результат и идёт «Дальше», как обычно.
     *
     * @param balance текущий счёт игрока.
     * @param ignoreDelays не ждать паузы узла и после засчитанного этапа (см. описание [QuestEngine]).
     * Саму проверку флаг не снимает: без «Засчитать» награды нет.
     * @return Новое состояние и исход, который теперь надо применить к питомцу и деньгам; null,
     * когда этап квеста проверки не ждёт.
     */
    fun approve(
        quest: Quest,
        quests: List<QuestProgress>,
        balance: Int,
        nowMillis: Long,
        ignoreDelays: Boolean = false
    ): Choice? {
        val current = progressOf(quests, quest.id) ?: return null
        if (!current.isAwaitingCheck) return null
        val sent = current.lastChoice ?: return null
        val moneyDelta = if (sent.moneyDelta < 0) {
            sent.moneyDelta.coerceAtLeast(-balance.coerceAtLeast(0))
        } else {
            sent.moneyDelta
        }
        val progress = if (quest.hasProgress) {
            (current.progress + sent.progressDelta).coerceIn(Quest.MIN_PROGRESS, Quest.MAX_PROGRESS)
        } else {
            current.progress
        }
        val outcome = sent.copy(moneyDelta = moneyDelta, progressDelta = progress - current.progress)
        val delayMinutes = quest.node(current.nodeId)?.delayMinutes ?: 0
        val waitMillis = if (outcome.isFinal || ignoreDelays) 0L else delayMinutes * MINUTE_MILLIS
        val updated = current.copy(
            progress = progress,
            availableAtMillis = nowMillis + waitMillis,
            lastChoice = outcome,
            check = QuestCheck.NONE
        )
        return Choice(quests.replaced(updated), outcome)
    }

    /**
     * Взрослый «Не засчитано»: сданный выбор снимается, этап снова в работе — ребёнок выбирает на
     * том же шаге заново. Награды не было, так что и отнимать нечего.
     *
     * @return Новое состояние, или null, когда этап квеста [questId] проверки не ждёт.
     */
    fun reject(quests: List<QuestProgress>, questId: String, nowMillis: Long): List<QuestProgress>? {
        val current = progressOf(quests, questId) ?: return null
        if (!current.isAwaitingCheck) return null
        return quests.replaced(
            current.copy(availableAtMillis = nowMillis, lastChoice = null, check = QuestCheck.REJECTED)
        )
    }

    /** @return Этапы, которые ждут проверки взрослым, в порядке списка квестов. */
    fun awaitingCheck(quests: List<QuestProgress>): List<QuestProgress> = quests.filter { it.isAwaitingCheck }

    /**
     * «Дальше» или «Завершить» после показанного результата. Узел, которого в данных больше нет,
     * завершает квест, а не оставляет игрока в тупике — это касается и следующего узла (после
     * выбора), и текущего: если квест ждёт выбора на узле, которого больше нет в данных, он тоже
     * завершается, а не виснет активным навсегда.
     *
     * @return Новое состояние, или null, когда идти дальше пока нельзя: выбор ещё не сделан (а
     * текущий узел при этом есть в данных) или задержка после выбора ещё не прошла.
     */
    fun advance(
        quest: Quest,
        quests: List<QuestProgress>,
        nowMillis: Long,
        ignoreDelays: Boolean = false
    ): List<QuestProgress>? {
        val current = progressOf(quests, quest.id) ?: return null
        if (!current.isActive) return null

        if (current.isAwaitingCheck) return null
        val choice = current.lastChoice
        if (choice == null) {
            if (quest.node(current.nodeId) != null) return null
            val updated = current.copy(
                nodeId = Quest.END_NODE,
                status = QuestStatus.FINISHED,
                availableAtMillis = nowMillis,
                lastChoice = null
            )
            return quests.replaced(updated)
        }
        if (nowMillis < stepAvailableAtMillis(current, nowMillis, ignoreDelays)) return null

        val next = quest.node(choice.nextNodeId)
        val updated = if (choice.isFinal || next == null) {
            current.copy(
                nodeId = Quest.END_NODE,
                status = QuestStatus.FINISHED,
                availableAtMillis = nowMillis,
                lastChoice = null
            )
        } else {
            current.copy(nodeId = next.id, availableAtMillis = nowMillis, lastChoice = null)
        }
        return quests.replaced(updated)
    }

    /**
     * Проверка на случайный квест, раз в тик. Выпадает, только если сейчас не идёт ни один
     * случайный квест, с [sinceMillis] прошло не меньше [RANDOM_QUEST_COOLDOWN_MILLIS] и бросок
     * [random] попал в один шанс из [RANDOM_QUEST_CHANCE]. Кандидаты — случайные квесты каталога,
     * которые можно начать ([canStart]); бросок не делается, пока до него не дошло. [ignoreDelays]
     * снимает кулдаун самих случайных квестов, но не шесть часов между ними.
     *
     * @param sinceMillis момент последнего случайного квеста или, если их не было, момент, когда
     * игрок завёл питомца.
     * @return Выпавший квест и новое состояние, или null.
     */
    fun maybeSpawnRandom(
        catalog: QuestCatalog,
        quests: List<QuestProgress>,
        balance: Int,
        sinceMillis: Long,
        nowMillis: Long,
        random: Random,
        ignoreDelays: Boolean = false
    ): Spawn? {
        val randomRunning = quests.any { progress ->
            progress.isActive && catalog.findQuestById(progress.questId)?.kind == QuestKind.RANDOM
        }
        if (randomRunning) return null
        if (nowMillis < sinceMillis || nowMillis - sinceMillis < RANDOM_QUEST_COOLDOWN_MILLIS) {
            return null
        }

        val candidates = catalog.quests.filter {
            it.kind == QuestKind.RANDOM && canStart(it, quests, balance, nowMillis, ignoreDelays)
        }
        if (candidates.isEmpty()) return null
        if (random.nextInt(RANDOM_QUEST_CHANCE) != 0) return null

        val quest = candidates[random.nextInt(candidates.size)]
        val started = start(quest, quests, balance, nowMillis, ignoreDelays) ?: return null
        return Spawn(started, quest)
    }

    /**
     * @param seenAtMillis когда игрок последний раз смотрел экран квестов.
     * @return Есть ли идущий квест, шаг которого стал доступен после этого и уже доступен сейчас —
     * тогда на кнопке квестов горит точка.
     */
    fun hasUnseenStep(quests: List<QuestProgress>, seenAtMillis: Long, nowMillis: Long): Boolean =
        quests.any {
            it.isActive && it.availableAtMillis <= nowMillis && it.availableAtMillis > seenAtMillis
        }

    /** @return Этот список, в котором запись того же квеста заменена на [updated]. */
    private fun List<QuestProgress>.replaced(updated: QuestProgress): List<QuestProgress> =
        map { if (it.questId == updated.questId) updated else it }
}
