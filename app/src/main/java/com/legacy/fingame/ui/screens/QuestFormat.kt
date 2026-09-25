package com.legacy.fingame.ui.screens

import com.legacy.fingame.game.quests.QuestEntry
import com.legacy.fingame.game.quests.QuestOutcome
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

/**
 * Короткий статус под названием карточки.
 *
 * @param entry карточка.
 * @param balance текущий счёт игрока — для «Нужно N монет».
 * @param nowMillis момент по игровым часам — для отсчёта.
 * @return «Можно взять», «Нужно 100 монет», «Шаг 2 из 3», «Следующий шаг через 0:42»,
 * «Следующий шаг готов», «Можно завершить» или «Завершён · 80%».
 */
fun questStatusText(entry: QuestEntry, balance: Int, nowMillis: Long): String {
    val quest = entry.quest
    val progress = entry.progress
        ?: return if (balance < quest.minBalance) needCoinsText(quest.minBalance) else "Можно взять"
    val choice = progress.lastChoice
    return when {
        progress.isFinished -> finishedText(quest.hasProgress, progress.progress)
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
