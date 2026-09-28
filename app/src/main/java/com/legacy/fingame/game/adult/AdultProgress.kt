package com.legacy.fingame.game.adult

import com.legacy.fingame.game.GameUiState
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.game.economy.SpendKind
import com.legacy.fingame.game.quests.Quest
import com.legacy.fingame.game.quests.QuestCatalog
import com.legacy.fingame.game.quests.QuestCheckEvent
import com.legacy.fingame.game.quests.QuestTopic

/** Где ребёнок в теме — без оценок, только «уже» и «ещё нет». */
enum class TopicStatus {
    /** Всё, что игра проверяет по этой теме, ребёнок уже делал. */
    MASTERED,

    /** Что-то по теме уже было, что-то ещё впереди. */
    IN_PROGRESS,

    /** По теме пока ничего не было. */
    AHEAD
}

/**
 * Один признак темы — то, что видно по игре: «Составлял план периода», «Прошёл квест на эту тему».
 *
 * @property text что это, нейтрально, в прошедшем времени.
 * @property done было ли это уже.
 */
data class TopicFact(val text: String, val done: Boolean)

/**
 * Тема в разделе «Прогресс».
 *
 * @property topic тема.
 * @property facts признаки темы, выведенные из игры.
 * @property status итог по признакам.
 */
data class TopicProgress(val topic: QuestTopic, val facts: List<TopicFact>, val status: TopicStatus)

/**
 * Отчёт раздела «Прогресс»: темы и общий прогресс.
 *
 * @property topics темы в порядке [QuestTopic].
 */
data class ProgressReport(val topics: List<TopicProgress>) {

    /** Сколько тем освоено. */
    val masteredCount: Int get() = topics.count { it.status == TopicStatus.MASTERED }

    /** Сколько тем начато, но ещё не освоено. */
    val inProgressCount: Int get() = topics.count { it.status == TopicStatus.IN_PROGRESS }

    /** Доля освоенных тем, 0..1. */
    val masteredFraction: Float get() = if (topics.isEmpty()) 0f else masteredCount.toFloat() / topics.size
}

/**
 * Раздел взрослого «Прогресс»: какие темы финансовой грамотности ребёнок уже прошёл. Всё выводится
 * из фактов игры — что ребёнок делал, — и ничего не оценивает: тема либо освоена, либо в процессе,
 * либо ещё впереди.
 */
object AdultProgress {

    /**
     * @param state состояние игры ребёнка.
     * @param catalog квесты — чтобы знать тему каждого квеста.
     * @return Отчёт по всем темам.
     */
    fun compute(state: GameUiState, catalog: QuestCatalog): ProgressReport =
        ProgressReport(QuestTopic.entries.map { topic -> progressOf(topic, state, catalog) })

    private fun progressOf(topic: QuestTopic, state: GameUiState, catalog: QuestCatalog): TopicProgress {
        val facts = gameFactsOf(topic, state).toMutableList()
        val quests = catalog.quests.filter { it.topic == topic }
        if (quests.isNotEmpty()) {
            facts += TopicFact("Прошёл квест на эту тему", quests.any { finishedOnce(it, state) })
        }
        val status = when {
            facts.isNotEmpty() && facts.all { it.done } -> TopicStatus.MASTERED
            facts.any { it.done } -> TopicStatus.IN_PROGRESS
            else -> TopicStatus.AHEAD
        }
        return TopicProgress(topic, facts, status)
    }

    /** Признаки темы, которые видны по игре помимо квестов. */
    private fun gameFactsOf(topic: QuestTopic, state: GameUiState): List<TopicFact> = when (topic) {
        QuestTopic.PLANNING -> listOf(
            TopicFact("Составлял план периода", state.budget != null || state.budgetHistory.isNotEmpty()),
            TopicFact("Прожил период с планом до конца", state.budgetHistory.isNotEmpty())
        )
        QuestTopic.NEEDS_WANTS -> listOf(
            TopicFact(
                "Покупал обязательное",
                state.moneyLog.entries.any { it.isPurchase && it.spendKind == SpendKind.MUST }
            )
        )
        QuestTopic.SAVING -> listOf(
            TopicFact(
                "Откладывал сбережения",
                state.budgetHistory.any { it.actualSavings > 0 } || (state.budget?.plannedSavings ?: 0) > 0
            )
        )
        QuestTopic.DEPOSIT -> listOf(
            TopicFact(
                "Открывал вклад",
                state.deposit != null ||
                    state.moneyLog.entries.any { it.reason == MoneyLog.REASON_DEPOSIT_OPENED } ||
                    state.budgetHistory.any { it.plannedDeposit > 0 }
            )
        )
        QuestTopic.GOALS -> listOf(
            TopicFact("Ставил цель", state.goals.isNotEmpty() || state.goalsReached > 0),
            TopicFact("Купил то, на что копил", state.goalsReached > 0)
        )
        QuestTopic.HONESTY -> emptyList()
    }

    /**
     * Проходил ли ребёнок [quest] до конца хоть раз. Пройденный — сейчас или раньше: многоразовый
     * квест, взятый снова, снова «идёт», но в истории квестов остался выбор, которым он кончился.
     */
    private fun finishedOnce(quest: Quest, state: GameUiState): Boolean {
        if (state.questProgressOf(quest.id)?.isFinished == true) return true
        return state.questLog.entries.any { choice ->
            choice.questId == quest.id &&
                choice.check != QuestCheckEvent.SENT && choice.check != QuestCheckEvent.REJECTED &&
                quest.node(choice.nodeId)?.options?.any {
                    it.label == choice.optionLabel && it.nextNodeId == Quest.END_NODE
                } == true
        }
    }
}
