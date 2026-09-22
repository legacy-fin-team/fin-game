package com.legacy.fingame.ui.screens

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Разделитель трёх балансов в шапке: одна иконка монеты и три числа через него. */
private const val BalanceSeparator = " | "

/**
 * Как пишется время записи журнала: часы и минуты, без секунд и без даты. Язык записан явно, а не
 * взят у устройства: в самом времени слов нет, зато цифры в некоторых локалях пишутся не
 * арабскими, и час записи оказался бы написан иначе, чем все остальные числа игры.
 */
private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

/**
 * Три счёта игрока одной строкой.
 *
 * @param balance текущие деньги — то, что можно потратить прямо сейчас.
 * @param savings сбережения.
 * @param depositAmount тело вклада, без процентов: проценты ещё не начислены, показывать их как
 * деньги игрока было бы обещанием.
 * @return Строка вида `100 | 200 | 510`.
 */
fun balancesTextOf(balance: Int, savings: Int, depositAmount: Int): String =
    listOf(balance, savings, depositAmount).joinToString(BalanceSeparator)

/**
 * То же самое словами, для тех, кто слушает экран: три числа через чёрточку вслух не читаются.
 *
 * @param balance текущие деньги.
 * @param savings сбережения.
 * @param depositAmount тело вклада.
 * @return Описание всех трёх счётов.
 */
fun balancesDescriptionOf(balance: Int, savings: Int, depositAmount: Int): String =
    "Текущие $balance, сбережения $savings, на вкладе $depositAmount"

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
