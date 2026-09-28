package com.legacy.fingame.utils

import android.util.Log
import com.legacy.fingame.game.adult.RewardUsage
import com.legacy.fingame.game.adult.RewardUsageLog

/**
 * Журнал использованных наград ([com.legacy.fingame.game.PlayerState.rewardUsageLog]) одной
 * строкой — по образцу [QuestLogCodec]: запись от записи отделяет `\u001E`, поле от поля —
 * `\u001F`; оба вычищаются из текстов при кодировании.
 *
 * Поля записи, всегда пять: id предмета, название, имя питомца, игровой день, момент.
 */
object RewardUsageLogCodec {

    private const val TAG = "RewardUsageLogCodec"

    private const val RECORD_SEPARATOR = '\u001E'
    private const val FIELD_SEPARATOR = '\u001F'

    private const val FIELDS_PER_RECORD = 5

    /**
     * @param log журнал наград.
     * @return Одна строка в том же порядке; пустая, когда записей нет.
     */
    fun encode(log: RewardUsageLog): String =
        log.entries.joinToString(RECORD_SEPARATOR.toString()) { usage ->
            listOf(
                clean(usage.itemId),
                clean(usage.itemName),
                clean(usage.petName),
                usage.gameDay,
                usage.timestampMillis
            ).joinToString(FIELD_SEPARATOR.toString())
        }

    /**
     * Читает то, что написал [encode]. Запись, которая не разбирается, отбрасывается.
     *
     * @param raw строка из настроек, или null, когда её не было (сейв до журнала наград).
     * @return Журнал, новейшая первой; пустой, когда строки нет или она пустая.
     */
    fun decode(raw: String?): RewardUsageLog {
        if (raw.isNullOrBlank()) return RewardUsageLog.EMPTY
        return RewardUsageLog.of(raw.split(RECORD_SEPARATOR).mapNotNull { decodeRecord(it) })
    }

    private fun decodeRecord(record: String): RewardUsage? {
        val fields = record.split(FIELD_SEPARATOR)
        if (fields.size != FIELDS_PER_RECORD) return dropped(record)
        val gameDay = fields[3].toLongOrNull()
        val timestampMillis = fields[4].toLongOrNull()
        if (fields[0].isBlank() || gameDay == null || timestampMillis == null) return dropped(record)
        return RewardUsage(
            itemId = fields[0],
            itemName = fields[1],
            petName = fields[2],
            gameDay = gameDay,
            timestampMillis = timestampMillis
        )
    }

    private fun clean(text: String): String =
        text.filterNot { it == RECORD_SEPARATOR || it == FIELD_SEPARATOR }

    private fun dropped(record: String): RewardUsage? {
        Log.w(TAG, "Dropped a malformed reward usage record: '$record'")
        return null
    }
}
