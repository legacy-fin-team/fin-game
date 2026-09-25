package com.legacy.fingame.ui.screens

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Круглые ступени шага: единица, четверть десятка и половина десятка в каждом десятичном
 * разряде — 1, 5, 10, 25, 50, 100, 250, 500 и так далее. Четверти в младшем разряде нет: двух с
 * половиной монет не бывает.
 *
 * Список кончается на половине миллиарда: следующий разряд уже не помещается в `Int`, а суммы
 * такого размера игра не считает.
 */
private val StepLadder: List<Int> = buildList {
    add(1)
    add(5)
    var decade = 10
    while (decade <= 100_000_000) {
        add(decade)
        add(decade / 10 * 25)
        add(decade / 10 * 50)
        decade *= 10
    }
}

/**
 * Шаг кнопок «плюс» и «минус» для суммы, которую игрок раскладывает.
 *
 * @param max сколько всего можно разложить.
 * @return Круглый шаг около десятой доли от [max]: 1, 5, 10, 25, 50, 100… — чтобы суммы
 * набирались круглыми, а не «по 37». Не меньше единицы.
 */
fun amountStepOf(max: Int): Int {
    val target = max / 10
    return StepLadder.lastOrNull { it <= target } ?: 1
}

/**
 * @param value сумма, которая набрана сейчас.
 * @param delta на сколько шагов сдвинуть: плюс — вверх, минус — вниз.
 * @param max сколько всего можно разложить.
 * @param step шаг сдвига; по умолчанию — [amountStepOf] от [max].
 * @return [value], сдвинутое на [delta] шагов и зажатое в `0..max`; сдвиг идёт от круглого
 * значения, так что 37 плюс шаг 10 даёт 40, а не 47.
 */
fun amountSteppedBy(value: Int, delta: Int, max: Int, step: Int = amountStepOf(max)): Int {
    val ceiling = max.coerceAtLeast(0)
    val from = value.coerceIn(0, ceiling)
    if (delta == 0) return from

    val size = step.coerceAtLeast(1)
    // Вверх считаем от круглого значения снизу, вниз — от круглого сверху: и то и другое
    // приводит некруглую сумму к круглой первым же нажатием, в ту сторону, куда её двигают.
    val rungs = if (delta > 0) from / size else (from + size - 1) / size
    val moved = (rungs.toLong() + delta) * size
    return moved.coerceIn(0L, ceiling.toLong()).toInt()
}

/**
 * @param raw положение ползунка так, как его отдаёт сам ползунок.
 * @param max сколько всего можно разложить.
 * @param step шаг, к которому притягивается ответ; по умолчанию — [amountStepOf] от [max].
 * @return Положение ползунка, приведённое к ближайшему шагу и зажатое в `0..max`; у самого края
 * ответ ровно `0` и ровно `max`, чтобы «всё» набиралось пальцем.
 */
fun amountSnappedTo(raw: Float, max: Int, step: Int = amountStepOf(max)): Int {
    val ceiling = max.coerceAtLeast(0)
    val clamped = raw.coerceIn(0f, ceiling.toFloat())
    val size = step.coerceAtLeast(1)
    val stepped = ((clamped / size).roundToLong() * size)
        .coerceIn(0L, ceiling.toLong())
        .toInt()

    // Сам [max] — такая же ступень, как круглые: иначе сумма, не кратная шагу, не набиралась бы
    // ползунком вовсе, и кнопка «Всё» осталась бы единственным способом разложить остаток.
    return if (abs(clamped - ceiling) < abs(clamped - stepped)) ceiling else stepped
}
