package com.legacy.fingame.ui.screens

/** Подпись на карточке цели, когда денег на неё уже хватает. */
const val GoalReadyText = "Можно купить"

/** Заголовок карточки-подсказки, пока целей нет. */
const val GoalsHintTitle = "Цели"

/** Текст карточки-подсказки: откуда цели берутся. */
const val GoalsHintText = "Отметь ★ в магазине"

/**
 * Карточка-подсказка вслух, для TalkBack: звезда словами, а не «чёрная звезда», как прочёл бы
 * символ из [GoalsHintText].
 */
const val GoalsHintSpoken = "Цели. Отметь звёздочкой в магазине"

/**
 * Сколько накоплено из цены — строка под полоской цели.
 *
 * Накопленное не бывает больше цены: лишние монеты к этой цели уже не относятся, и `500 / 200`
 * читалось бы как ошибка. Пробелы вокруг черты неразрывные — строка никогда не рвётся посередине.
 *
 * @param balance текущие деньги игрока.
 * @param price цена товара-цели.
 * @return Строка вида `120 / 200`.
 */
fun goalAmountText(balance: Int, price: Int): String {
    val cost = price.coerceAtLeast(0)
    return "${balance.coerceIn(0, cost)}$NoBreakSpace/$NoBreakSpace$cost"
}

/**
 * Проценты справа от полоски цели.
 *
 * Округляются вниз и считаются в целых: 199 монет из 200 — это «99%», а не «100%», иначе игрок
 * увидел бы сотню и не смог купить. «100%» появляется ровно тогда же, когда «Можно купить».
 *
 * @param balance текущие деньги игрока.
 * @param price цена товара-цели.
 * @return Строка вида `40%`; для бесплатного товара — `100%`.
 */
fun goalPercentText(balance: Int, price: Int): String {
    val cost = price.coerceAtLeast(0)
    val percent = if (cost == 0) 100L else balance.coerceIn(0, cost).toLong() * 100 / cost
    return "$percent%"
}

/**
 * Хватает ли уже денег на цель.
 *
 * Ровно тогда же, когда [goalPercentText] говорит «100%»: считается в целых, а не по доле
 * [com.legacy.fingame.game.economy.goalProgress], которую `Float` на больших ценах мог бы округлить
 * до единицы раньше времени.
 *
 * @param balance текущие деньги игрока.
 * @param price цена товара-цели.
 * @return true, когда денег не меньше цены, и для бесплатного товара.
 */
fun goalIsReady(balance: Int, price: Int): Boolean = price <= 0 || balance >= price

/**
 * Нижняя строка карточки цели: сколько накоплено из цены, а когда денег хватает — «Можно купить»
 * вместо суммы.
 *
 * Одно из двух, а не оба рядом: «150 / 150» и «Можно купить» в одну строку не влезают на узком
 * телефоне с крупным шрифтом, а сумма рядом со «100%» ничего не добавляет.
 *
 * @param balance текущие деньги игрока.
 * @param price цена товара-цели.
 * @return [GoalReadyText] или строка вида `120 / 200` ([goalAmountText]).
 */
fun goalFooterText(balance: Int, price: Int): String =
    if (goalIsReady(balance = balance, price = price)) {
        GoalReadyText
    } else {
        goalAmountText(balance = balance, price = price)
    }

/**
 * Какая это цель из скольких — счётчик справа от имени, когда целей несколько.
 *
 * @param index место цели в списке, с нуля.
 * @param count сколько всего целей.
 * @return Строка вида `1/3`.
 */
fun goalCounterText(index: Int, count: Int): String = "${index + 1}/$count"
