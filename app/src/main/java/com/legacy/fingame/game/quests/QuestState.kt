package com.legacy.fingame.game.quests

import com.legacy.fingame.game.stats.StatKind

/** Идёт ли квест или уже пройден. */
enum class QuestStatus {
    /** Квест идёт: игрок на каком-то его шаге. */
    ACTIVE,

    /** Квест пройден до конца. */
    FINISHED
}

/** Где этап квеста с [Quest.requiresAdultCheck] в проверке взрослым. */
enum class QuestCheck {
    /** Проверки нет: этап не сдавался или уже засчитан. */
    NONE,

    /** Ребёнок сдал этап — выбор сделан, награда ждёт «Засчитать» взрослого. */
    WAITING,

    /** Взрослый не засчитал: этап снова в работе, ребёнок выбирает заново. */
    REJECTED
}

/**
 * Что вышло из выбора игрока: показывается после нажатия и до перехода к следующему шагу.
 *
 * @property optionLabel надпись выбранного варианта.
 * @property resultText описание результата.
 * @property nextNodeId куда ведёт кнопка «Дальше», или [Quest.END_NODE] — тогда «Завершить».
 * @property statEffects изменения полосок питомца, как они были применены.
 * @property moneyDelta реально начисленные (плюс) или списанные (минус) монеты: трата больше
 * баланса урезается до баланса.
 * @property progressDelta реальное изменение прогресса в процентах, с учётом рамок 0..100.
 */
data class QuestOutcome(
    val optionLabel: String,
    val resultText: String,
    val nextNodeId: String,
    val statEffects: Map<StatKind, Int> = emptyMap(),
    val moneyDelta: Int = 0,
    val progressDelta: Int = 0
) {
    /** Ведёт ли этот выбор к концу квеста. */
    val isFinal: Boolean get() = nextNodeId == Quest.END_NODE
}

/**
 * Где игрок в одном квесте.
 *
 * @property questId id квеста.
 * @property nodeId узел, на котором игрок: пока [lastChoice] не null — узел, где сделан выбор;
 * у завершённого квеста — [Quest.END_NODE].
 * @property availableAtMillis момент по игровым часам, с которого доступен следующий шаг: пока
 * игрок выбирает — момент, когда узел открылся; после выбора — момент окончания задержки.
 * @property progress прогресс 0..100 % (у квестов без прогресса всегда 0).
 * @property status идёт квест или пройден.
 * @property lastChoice исход последнего выбора, пока игрок не нажал «Дальше», иначе null.
 * @property enabledAgain включил ли взрослый этот пройденный квест снова — тогда
 * [QuestEngine.availabilityOf] не смотрит ни на [Quest.repeatable], ни на кулдаун. Ставится
 * [QuestEngine.enable], снимается автоматически, когда квест начинается заново ([QuestEngine.start]
 * строит новую запись без этого флага).
 * @property check проверка этапа взрослым (только у квестов с [Quest.requiresAdultCheck]): пока
 * [QuestCheck.WAITING], [lastChoice] — сданный выбор, награда за который ещё не выдана.
 */
data class QuestProgress(
    val questId: String,
    val nodeId: String,
    val availableAtMillis: Long,
    val progress: Int = Quest.MIN_PROGRESS,
    val status: QuestStatus = QuestStatus.ACTIVE,
    val lastChoice: QuestOutcome? = null,
    val enabledAgain: Boolean = false,
    val check: QuestCheck = QuestCheck.NONE
) {
    /** Идёт ли квест. */
    val isActive: Boolean get() = status == QuestStatus.ACTIVE

    /** Ждёт ли этап проверки взрослым. */
    val isAwaitingCheck: Boolean get() = isActive && check == QuestCheck.WAITING

    /** Пройден ли квест. */
    val isFinished: Boolean get() = status == QuestStatus.FINISHED
}
