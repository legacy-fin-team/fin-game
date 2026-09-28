package com.legacy.fingame.ui.screens

import com.legacy.fingame.game.quests.Quest
import com.legacy.fingame.game.quests.QuestEngine
import com.legacy.fingame.game.quests.QuestEntry
import com.legacy.fingame.game.quests.QuestOption
import com.legacy.fingame.game.quests.QuestOutcome
import com.legacy.fingame.game.quests.QuestProgress
import com.legacy.fingame.game.quests.QuestUnavailableReason
import com.legacy.fingame.ui.components.Sprites
import java.util.Locale
import kotlin.math.abs

/** Отсчёт идёт целыми секундами. */
private const val SecondMillis = 1_000L
private const val MinuteSeconds = 60L
private const val HourSeconds = 3_600L

/**
 * @param amount число монет.
 * @return Число и слово «монета» в нужной форме, через неразрывный пробел: «1 монета»,
 * «3 монеты», «100 монет».
 */
fun coinsText(amount: Int): String {
    val lastTwo = abs(amount) % 100
    val last = lastTwo % 10
    val word = when {
        lastTwo in 11..14 -> "монет"
        last == 1 -> "монета"
        last in 2..4 -> "монеты"
        else -> "монет"
    }
    return "$amount$NoBreakSpace$word"
}

/** @return Статус квеста, на который не хватает денег: «Нужно 100 монет». */
fun needCoinsText(amount: Int): String = "Нужно ${coinsText(amount)}"

/**
 * @param option вариант на шаге квеста.
 * @param balance текущий счёт игрока.
 * @return «Нужно 30 монет», когда на вариант не хватает монет (кнопка тогда неактивна), или null,
 * когда его можно выбрать.
 */
fun optionLockText(option: QuestOption, balance: Int): String? =
    if (QuestEngine.canAfford(option, balance)) null else needCoinsText(-option.moneyDelta)

/** @return Пояснение к минимуму: монеты должны быть в запасе, но не тратятся. */
fun minBalanceNoteText(amount: Int): String =
    "Нужно ${coinsText(amount)} в запасе${DashSeparator}они не тратятся"

/** @return «Шаг 2 из 3». */
fun stepText(step: Int, total: Int): String = "Шаг $step из $total"

/**
 * @param remainingMillis сколько осталось ждать.
 * @return «M:SS», а от часа — «H:MM:SS». Секунды округляются вверх, так что пока ждать есть
 * чего, «0:00» не показывается.
 */
fun countdownText(remainingMillis: Long): String {
    val totalSeconds = (remainingMillis.coerceAtLeast(0L) + SecondMillis - 1) / SecondMillis
    val hours = totalSeconds / HourSeconds
    val minutes = totalSeconds % HourSeconds / MinuteSeconds
    val seconds = totalSeconds % MinuteSeconds
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

/** @return «Следующий шаг через 0:42». */
fun nextStepInText(remainingMillis: Long): String =
    "Следующий шаг через ${countdownText(remainingMillis)}"

/** @return «Завершён · 80%» у квеста с прогрессом, «Завершён» — без. */
fun finishedText(hasProgress: Boolean, progress: Int): String =
    if (hasProgress) "Завершён$DotSeparator$progress%" else "Завершён"

/** Ребёнок сдал этап, взрослый ещё не проверил. */
const val QuestAwaitingCheckText = "Ждём, пока взрослый проверит"

/** Взрослый не засчитал этап — ребёнку, без упрёка. */
const val QuestCheckRejectedText = "Взрослый пока не засчитал этот шаг. Попробуй ещё раз!"

/** @return «Доступен через 1:02:03» — квест на кулдауне после прохождения. */
fun availableInText(remainingMillis: Long): String =
    "Доступен через ${countdownText(remainingMillis)}"

/**
 * Статус пройденного квеста: обычное «Завершён», либо, когда квест на кулдауне, отсчёт до
 * следующей возможности его начать. Одноразовый квест, ждущий взрослого, тоже «Завершён» — для
 * него текста пока нет, экран взрослого режима будет в другой ветке.
 *
 * @param quest квест.
 * @param progress его сохранённый прогресс — пройденный.
 * @param balance текущий счёт игрока.
 * @param nowMillis текущий момент.
 */
private fun finishedStatusText(
    quest: Quest,
    progress: QuestProgress,
    balance: Int,
    nowMillis: Long
): String {
    val availability = QuestEngine.availabilityOf(quest, listOf(progress), balance, nowMillis)
    return if (availability.reason == QuestUnavailableReason.COOLDOWN) {
        availableInText(availability.availableAtMillis!! - nowMillis)
    } else {
        finishedText(quest.hasProgress, progress.progress)
    }
}

/**
 * Короткий статус под названием карточки.
 *
 * @param entry карточка.
 * @param balance текущий счёт игрока — для «Нужно N монет».
 * @param nowMillis момент по игровым часам — для отсчёта.
 * @return «Можно взять», «Нужно 100 монет», «Шаг 2 из 3», «Следующий шаг через 0:42»,
 * «Следующий шаг готов», «Можно завершить», «Завершён · 80%» или «Доступен через …».
 */
fun questStatusText(entry: QuestEntry, balance: Int, nowMillis: Long): String {
    val quest = entry.quest
    val progress = entry.progress
        ?: return if (balance < quest.minBalance) needCoinsText(quest.minBalance) else "Можно взять"
    val choice = progress.lastChoice
    return when {
        progress.isFinished -> finishedStatusText(quest, progress, balance, nowMillis)
        progress.isAwaitingCheck -> QuestAwaitingCheckText
        choice == null -> stepText(
            quest.stepNumberOf(progress.nodeId).coerceAtLeast(1),
            quest.stepCount.coerceAtLeast(1)
        )
        nowMillis < progress.availableAtMillis ->
            nextStepInText(progress.availableAtMillis - nowMillis)
        choice.isFinal -> "Можно завершить"
        else -> "Следующий шаг готов"
    }
}

/**
 * Статус под названием раскрытой карточки. Низ раскрытой карточки сам показывает отсчёт и что квест
 * завершён, так что шапка их не повторяет — кроме отсчёта кулдауна, его больше негде показать.
 *
 * @param entry карточка.
 * @param balance текущий счёт игрока — для «Нужно N монет».
 * @param nowMillis момент по игровым часам.
 * @return «Шаг 2 из 3» у взятого квеста, «Доступен через …» у пройденного на кулдауне, null у
 * пройденного без кулдауна, у невзятого — как [questStatusText].
 */
fun expandedQuestStatusText(entry: QuestEntry, balance: Int, nowMillis: Long): String? {
    val progress = entry.progress ?: return questStatusText(entry, balance, nowMillis)
    if (progress.isFinished) {
        val availability = QuestEngine.availabilityOf(entry.quest, listOf(progress), balance, nowMillis)
        return if (availability.reason == QuestUnavailableReason.COOLDOWN) {
            availableInText(availability.availableAtMillis!! - nowMillis)
        } else {
            null
        }
    }
    return stepText(
        entry.quest.stepNumberOf(progress.nodeId).coerceAtLeast(1),
        entry.quest.stepCount.coerceAtLeast(1)
    )
}

/**
 * @param entries карточки экрана.
 * @param nowMillis момент по игровым часам.
 * @return Ждёт ли хоть один идущий квест следующего шага — только тогда экрану нужен отсчёт.
 */
fun hasWaitingStep(entries: List<QuestEntry>, nowMillis: Long): Boolean =
    entries.any { entry ->
        entry.progress?.let { it.isActive && it.availableAtMillis > nowMillis } == true
    }

/** @return Надпись кнопки под результатом: «Завершить» после последнего выбора, иначе «Дальше». */
fun advanceButtonText(outcome: QuestOutcome): String =
    if (outcome.isFinal) "Завершить" else "Дальше"

/** @return «Прогресс +30%». */
fun progressChangeText(delta: Int): String = "Прогресс ${signedAmountText(delta)}%"

/**
 * @param imagePath картинка из данных, или null.
 * @param exists есть ли файл в `assets/textures/` (в приложении — `SpriteLoader.hasSprite`).
 * @return Картинка, а когда её нет — иконка квестов, а не заглушка `error.webp`.
 */
fun questImageOf(imagePath: String?, exists: (String) -> Boolean): String =
    imagePath?.takeIf(exists) ?: Sprites.QUESTS

/**
 * Раскрыта одна карточка за раз.
 *
 * @param expanded id раскрытой сейчас карточки, или null.
 * @param tapped id карточки, по которой тапнули.
 * @return id карточки, которая будет раскрыта, или null, когда тапнули по раскрытой.
 */
fun nextExpandedQuest(expanded: String?, tapped: String): String? =
    if (expanded == tapped) null else tapped
