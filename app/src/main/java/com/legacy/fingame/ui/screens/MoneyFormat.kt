package com.legacy.fingame.ui.screens

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Как пишется время записи журнала: часы и минуты, без секунд и без даты. Язык записан явно, а не
 * взят у устройства: в самом времени слов нет, зато цифры в некоторых локалях пишутся не
 * арабскими, и час записи оказался бы написан иначе, чем все остальные числа игры.
 */
private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

/**
 * Подпись про вклад в шапке — та, что стоит рядом с деньгами.
 *
 * Пока вклада нет, подписи нет совсем: раньше оба счёта писались одной строкой через чёрточку
 * (`250 | 0`), и ноль в ней ребёнку не говорил ничего, а чёрточка читалась как случайный значок.
 *
 * @param depositAmount тело вклада, без процентов: проценты ещё не начислены, показывать их как
 * деньги игрока было бы обещанием.
 * @return Строка вида `вклад 510`, либо `null`, когда вклада нет.
 */
fun depositTextOf(depositAmount: Int): String? =
    if (depositAmount > 0) "вклад $depositAmount" else null

/**
 * Оба счёта словами, для тех, кто слушает экран.
 *
 * @param balance текущие деньги — то, что можно потратить прямо сейчас.
 * @param depositAmount тело вклада.
 * @return Описание счётов; про вклад в нём сказано, только когда вклад есть — ровно то же, что
 * видит глазами тот, кто на экран смотрит.
 */
fun balancesDescriptionOf(balance: Int, depositAmount: Int): String =
    if (depositAmount > 0) "Текущие $balance, на вкладе $depositAmount" else "Текущие $balance"

/**
 * @param amount сумма со знаком.
 * @return Сумма со знаком плюс, когда она положительная, со своим минусом, когда отрицательная,
 * и просто нулём, когда ничего не изменилось.
 */
fun signedAmountText(amount: Int): String = if (amount > 0) "+$amount" else amount.toString()

/**
 * Номер игрового дня так, как его показывают игроку.
 *
 * Дни игра считает от эпохи, и показывать игроку девятнадцатитысячный день бессмысленно, поэтому
 * счёт ведётся от самой старой записи, которую журнал ещё помнит.
 *
 * @param gameDay день записи.
 * @param oldestGameDay день самой старой хранимой записи.
 * @return Порядковый номер дня, начиная с единицы.
 */
fun dayNumberOf(gameDay: Long, oldestGameDay: Long): Int =
    (gameDay - oldestGameDay + 1).coerceAtLeast(1L).toInt()

/**
 * @param millis момент записи, в миллисекундах.
 * @param zone часовой пояс, в котором его читают; по умолчанию пояс устройства.
 * @return Время вида `14:05`.
 */
fun timeTextOf(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    TimeFormat.format(Instant.ofEpochMilli(millis).atZone(zone))

/**
 * Столбец времени в журнале: номер игрового дня и час записи.
 *
 * @param gameDay день записи.
 * @param oldestGameDay день самой старой хранимой записи.
 * @param millis момент записи, в миллисекундах.
 * @param zone часовой пояс, в котором его читают; по умолчанию пояс устройства.
 * @return Строка вида `Д8 · 16:09`.
 */
fun dayTimeTextOf(
    gameDay: Long,
    oldestGameDay: Long,
    millis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): String = "Д${dayNumberOf(gameDay, oldestGameDay)} · ${timeTextOf(millis, zone)}"

/**
 * День, от которого игроку считаются номера дней.
 *
 * Счёт ведётся от самой старой записи журнала, и один и тот же день называется одним и тем же
 * номером везде — и в журнале, и на бюджете. Пока журнал пуст, считать не от чего, и счёт
 * начинается с сегодня: сегодня и есть «день 1».
 *
 * @param oldestGameDay день самой старой хранимой записи; `null`, когда журнал пуст.
 * @param todayDay сегодняшний игровой день.
 * @return День, который игрок называет первым.
 */
fun firstDayOf(oldestGameDay: Long?, todayDay: Long): Long = oldestGameDay ?: todayDay

/**
 * Какой сегодня день, словами.
 *
 * Без этой строки номера дней не с чем сравнить: «закроется в день 23» ничего не говорит тому, кто
 * не знает, какой день сейчас.
 *
 * @param todayDay сегодняшний игровой день.
 * @param oldestGameDay день, от которого ведётся счёт (см. [firstDayOf]).
 * @return Строка вида `Сегодня: день 5`.
 */
fun todayTextOf(todayDay: Long, oldestGameDay: Long): String =
    "Сегодня: день ${dayNumberOf(todayDay, oldestGameDay)}"

/**
 * Когда вклад закроется, словами.
 *
 * Сказано двумя мерами сразу: сколько дней осталось ждать и каким днём этот срок называется в
 * журнале. Одного номера дня мало — «закроется в день 23» не отвечает на вопрос «это когда?», —
 * а одного счётчика мало, чтобы найти этот день в журнале потом.
 *
 * В день созревания и позже написано «Закроется сегодня»: вклад созревает, когда день дошёл до
 * срока (`Deposit.isMatureOn`), и «через 0 дн» про него звучало бы сломанно.
 *
 * @param maturityDay день, в который вклад созревает.
 * @param todayDay сегодняшний игровой день.
 * @param oldestGameDay день, от которого ведётся счёт (см. [firstDayOf]).
 * @return Строка вида `Закроется через 2 дн · день 5` либо `Закроется сегодня`.
 */
fun depositMaturityTextOf(maturityDay: Long, todayDay: Long, oldestGameDay: Long): String {
    val daysLeft = (maturityDay - todayDay).coerceAtLeast(0L)
    return if (daysLeft == 0L) {
        "Закроется сегодня"
    } else {
        "Закроется через $daysLeft дн · день ${dayNumberOf(maturityDay, oldestGameDay)}"
    }
}
