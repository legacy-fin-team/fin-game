package com.legacy.fingame.utils

import android.util.Log
import com.legacy.fingame.game.economy.MoneyEntry
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.game.economy.SpendKind

/**
 * Журнал денег как одна строка в настройках приложения.
 *
 * Журнал — единственное в состоянии игрока, что не раскладывается по отдельным ключам: это список
 * переменной длины, и ключ на запись превратил бы настройки в файл на две тысячи строк. Поэтому он
 * пишется одной строкой: запись от записи отделяет [ENTRY_SEPARATOR], поле от поля — [FIELD_SEPARATOR].
 * Оба разделителя — управляющие символы (record separator и unit separator), которые не появляются
 * ни в одном обычном тексте, так что формат никогда не путает разделитель с содержимым — за
 * единственным исключением: причина ([MoneyEntry.reason]) чистится от них при кодировании (см.
 * [encode]), чтобы даже самый странный ввод не мог сломать формат.
 *
 * Запись — четыре поля (причина, изменение, игровой день, момент) или восемь: к ним покупка
 * добавляет товар, вариант, количество и категорию плана (у не-покупки они пустые). Журнал версии
 * до истории покупок писал только четыре поля — такие записи читаются как не-покупки.
 */
object MoneyLogCodec {

    private const val TAG = "MoneyLogCodec"

    /** Разделитель записей журнала — символ "record separator". */
    private const val ENTRY_SEPARATOR = '\u001E'

    /** Разделитель полей внутри одной записи — символ "unit separator". */
    private const val FIELD_SEPARATOR = '\u001F'

    /** Сколько полей в записи старого вида: причина, изменение, игровой день, момент. */
    private const val BASE_FIELDS = 4

    /** Сколько полей в записи нынешнего вида: к базовым — товар, вариант, количество, категория. */
    private const val FIELDS_PER_ENTRY = 8

    /**
     * @param log журнал, как его держит игра.
     * @return Журнал одной строкой, в том же порядке — новейшее первым. Пустой журнал даёт пустую
     * строку.
     */
    fun encode(log: MoneyLog): String = log.entries.joinToString(ENTRY_SEPARATOR.toString()) { entry ->
        listOf(
            clean(entry.reason),
            entry.delta,
            entry.gameDay,
            entry.timestampMillis,
            clean(entry.itemId.orEmpty()),
            clean(entry.variantId.orEmpty()),
            entry.quantity,
            entry.spendKind?.name.orEmpty()
        ).joinToString(FIELD_SEPARATOR.toString())
    }

    /** @return [text] без обоих разделителей: так он не сломает формат, что бы в нём ни было. */
    private fun clean(text: String): String =
        text.replace(ENTRY_SEPARATOR.toString(), "").replace(FIELD_SEPARATOR.toString(), "")

    /**
     * Читает обратно то, что написал [encode].
     *
     * Запись, которая не разбирается — не то число полей или нечисловое число, — отбрасывается, а
     * не роняет весь разбор: так же в этом хранилище отбрасывается испорченная строка инвентаря —
     * потерять строку журнала дешевле, чем потерять вместе с ней питомца и деньги.
     *
     * @param raw строка, записанная [encode], или null, когда журнала ещё не было.
     * @return Журнал; пустой, когда строки нет, она пустая/состоит из пробелов, или в ней вообще
     * не разобралась ни одна запись.
     */
    fun decode(raw: String?): MoneyLog {
        if (raw.isNullOrBlank()) return MoneyLog.EMPTY

        val entries = raw.split(ENTRY_SEPARATOR).mapNotNull { record -> decodeEntry(record) }
        return MoneyLog.of(entries)
    }

    /**
     * @param record одна запись, как её отделил [decode].
     * @return Запись, или null, когда полей не четыре и не восемь или число не разобралось.
     * Категория, которой больше нет в игре, читается как null — сама покупка от этого не теряется.
     */
    private fun decodeEntry(record: String): MoneyEntry? {
        val fields = record.split(FIELD_SEPARATOR)
        if (fields.size != BASE_FIELDS && fields.size != FIELDS_PER_ENTRY) return dropped(record)

        val delta = fields[1].toIntOrNull()
        val gameDay = fields[2].toLongOrNull()
        val timestampMillis = fields[3].toLongOrNull()
        if (delta == null || gameDay == null || timestampMillis == null) return dropped(record)

        val entry = MoneyEntry(
            reason = fields[0],
            delta = delta,
            gameDay = gameDay,
            timestampMillis = timestampMillis
        )
        if (fields.size == BASE_FIELDS) return entry

        val quantity = fields[6].toIntOrNull() ?: return dropped(record)
        return entry.copy(
            itemId = fields[4].ifEmpty { null },
            variantId = fields[5].ifEmpty { null },
            quantity = quantity,
            spendKind = SpendKind.entries.find { it.name == fields[7] }
        )
    }

    private fun dropped(record: String): MoneyEntry? {
        Log.w(TAG, "Dropped a malformed money log record: '$record'")
        return null
    }
}
