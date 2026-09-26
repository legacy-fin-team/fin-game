package com.legacy.fingame.utils

import android.util.Log
import com.legacy.fingame.game.quests.QuestChoice
import com.legacy.fingame.game.quests.QuestLog

/**
 * Журнал выборов в квестах ([com.legacy.fingame.game.PlayerState.questLog]) как одна строка в
 * настройках — по образцу [MoneyLogCodec]: запись от записи отделяет record separator
 * (`\u001E`), поле от поля — unit separator (`\u001F`); оба вычищаются из текстов при
 * кодировании.
 *
 * Поля записи, всегда семь: id квеста, шаг, надпись варианта, деньги, изменение прогресса,
 * игровой день, момент.
 */
object QuestLogCodec {

    private const val TAG = "QuestLogCodec"

    private const val RECORD_SEPARATOR = '\u001E'
    private const val FIELD_SEPARATOR = '\u001F'

    private const val FIELDS_PER_RECORD = 7

    /**
     * @param log журнал выборов.
     * @return Одна строка в том же порядке; пустая, когда выборов нет.
     */
    fun encode(log: QuestLog): String =
        log.entries.joinToString(RECORD_SEPARATOR.toString()) { choice ->
            listOf(
                clean(choice.questId),
                clean(choice.nodeId),
                clean(choice.optionLabel),
                choice.moneyDelta,
                choice.progressDelta,
                choice.gameDay,
                choice.timestampMillis
            ).joinToString(FIELD_SEPARATOR.toString())
        }

    /**
     * Читает то, что написал [encode]. Запись, которая не разбирается, отбрасывается, а не роняет
     * весь разбор.
     *
     * @param raw строка из настроек, или null, когда её не было.
     * @return Журнал, новейший выбор первым; пустой, когда строки нет или она пустая.
     */
    fun decode(raw: String?): QuestLog {
        if (raw.isNullOrBlank()) return QuestLog.EMPTY
        return QuestLog.of(raw.split(RECORD_SEPARATOR).mapNotNull { decodeRecord(it) })
    }

    private fun decodeRecord(record: String): QuestChoice? {
        val fields = record.split(FIELD_SEPARATOR)
        if (fields.size != FIELDS_PER_RECORD) return dropped(record)

        val moneyDelta = fields[3].toIntOrNull()
        val progressDelta = fields[4].toIntOrNull()
        val gameDay = fields[5].toLongOrNull()
        val timestampMillis = fields[6].toLongOrNull()
        if (fields[0].isBlank() || moneyDelta == null || progressDelta == null ||
            gameDay == null || timestampMillis == null
        ) {
            return dropped(record)
        }

        return QuestChoice(
            questId = fields[0],
            nodeId = fields[1],
            optionLabel = fields[2],
            moneyDelta = moneyDelta,
            progressDelta = progressDelta,
            gameDay = gameDay,
            timestampMillis = timestampMillis
        )
    }

    private fun clean(text: String): String =
        text.filterNot { it == RECORD_SEPARATOR || it == FIELD_SEPARATOR }

    private fun dropped(record: String): QuestChoice? {
        Log.w(TAG, "Dropped a malformed quest log record: '$record'")
        return null
    }
}
