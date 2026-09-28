package com.legacy.fingame.game.quests

/**
 * Почему [QuestAvailability.canStart] сейчас ложь.
 */
enum class QuestUnavailableReason {

    /** Квест уже идёт: сперва его нужно закончить. */
    ACTIVE,

    /** На счёте меньше, чем [Quest.minBalance]. */
    NOT_ENOUGH_MONEY,

    /** Квест с `repeatable = false` уже пройден, и взрослый ещё не включил его снова. */
    ONE_TIME_DONE,

    /** Квест пройден и ждёт [Quest.cooldownMinutes] с момента прошлого прохождения. */
    COOLDOWN
}

/**
 * Можно ли начать квест прямо сейчас — и если нет, то почему; единственное место, где это решается
 * (см. [QuestEngine.availabilityOf]). Экран квестов и будущий экран взрослого режима только
 * спрашивают, ничего не решают сами.
 *
 * @property canStart можно ли начать квест сейчас.
 * @property reason причина отказа, или null, когда [canStart] истинно.
 * @property availableAtMillis момент, когда пройдёт кулдаун — только когда [reason] это
 * [QuestUnavailableReason.COOLDOWN]; иначе null.
 */
data class QuestAvailability(
    val canStart: Boolean,
    val reason: QuestUnavailableReason? = null,
    val availableAtMillis: Long? = null
)
