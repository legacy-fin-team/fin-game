package com.legacy.fingame.utils

import android.util.Log
import com.legacy.fingame.game.items.ItemSelection

/**
 * Цели игрока как одна строка в настройках приложения.
 *
 * Цели — список переменной длины, и порядок в нём — часть данных: в каком порядке игрок отмечал
 * товары звёздочкой, в таком карточки и стоят на главном экране, и новая встаёт в конец, не сдвигая
 * остальные. Набор строк (`StringSet`) порядок теряет, поэтому цели пишутся одной строкой: цель от
 * цели отделяет [GOAL_SEPARATOR], id товара от id варианта — [ID_SEPARATOR]. Оба — управляющие
 * символы (record separator и unit separator, как в [MoneyLogCodec]), которых нет в id из файлов
 * данных, так что формат не путает разделитель с содержимым.
 */
object GoalsCodec {

    private const val TAG = "GoalsCodec"

    /** Разделитель целей — символ "record separator". */
    private const val GOAL_SEPARATOR = '\u001E'

    /** Разделитель id товара и id варианта внутри одной цели — символ "unit separator". */
    private const val ID_SEPARATOR = '\u001F'

    /**
     * @param goals цели в том порядке, в каком игрок их отмечал.
     * @return Цели одной строкой, в том же порядке, каждая — один раз. Нет целей — пустая строка.
     */
    fun encode(goals: List<ItemSelection>): String =
        goals.distinct().joinToString(GOAL_SEPARATOR.toString()) { goal ->
            "${goal.itemId}$ID_SEPARATOR${goal.variantId}"
        }

    /**
     * Читает обратно то, что написал [encode].
     *
     * Запись, которая не разбирается — не два id или один из них пустой, — отбрасывается, а не
     * роняет весь разбор: потерять одну звёздочку дешевле, чем потерять вместе с ней питомца и
     * деньги. Повтор цели тоже отбрасывается: цель остаётся там, где встретилась первый раз.
     *
     * @param raw строка, записанная [encode], или null, когда целей ещё не сохраняли.
     * @return Цели в сохранённом порядке, без повторов; пусто, когда строки нет или она пустая.
     */
    fun decode(raw: String?): List<ItemSelection> {
        if (raw.isNullOrBlank()) return emptyList()

        return raw.split(GOAL_SEPARATOR).mapNotNull { record -> decodeGoal(record) }.distinct()
    }

    /**
     * @param record одна цель, как её отделил [decode].
     * @return Цель, или null, когда в записи не ровно два непустых id.
     */
    private fun decodeGoal(record: String): ItemSelection? {
        val ids = record.split(ID_SEPARATOR)
        if (ids.size != 2 || ids.any { id -> id.isBlank() }) {
            Log.w(TAG, "Dropped a malformed goal record: '$record'")
            return null
        }

        return ItemSelection(itemId = ids[0], variantId = ids[1])
    }
}
