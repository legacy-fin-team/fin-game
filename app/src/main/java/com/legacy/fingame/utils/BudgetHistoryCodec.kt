package com.legacy.fingame.utils

import android.util.Log
import com.legacy.fingame.game.economy.BudgetHistory
import com.legacy.fingame.game.economy.BudgetResult

/**
 * История бюджета ([com.legacy.fingame.game.PlayerState.budgetHistory]) как одна строка в
 * настройках — по образцу [MoneyLogCodec]: запись от записи отделяет record separator
 * (`\u001E`), поле от поля — unit separator (`\u001F`). В записи только числа, так что чистить
 * нечего.
 *
 * Поля записи, всегда восемь: день начала периода, план и факт обязательных, план и факт
 * необязательных, план и факт сбережений, вклад.
 */
object BudgetHistoryCodec {

    private const val TAG = "BudgetHistoryCodec"

    private const val RECORD_SEPARATOR = '\u001E'
    private const val FIELD_SEPARATOR = '\u001F'

    private const val FIELDS_PER_RECORD = 8

    /**
     * @param history история, новейшее первым.
     * @return Одна строка; пустая, когда история пуста.
     */
    fun encode(history: List<BudgetResult>): String =
        history.joinToString(RECORD_SEPARATOR.toString()) { result ->
            listOf(
                result.startDay,
                result.plannedMust,
                result.actualMust,
                result.plannedWant,
                result.actualWant,
                result.plannedSavings,
                result.actualSavings,
                result.plannedDeposit
            ).joinToString(FIELD_SEPARATOR.toString())
        }

    /**
     * Читает то, что написал [encode]. Запись, которая не разбирается, отбрасывается, а не роняет
     * весь разбор.
     *
     * @param raw строка из настроек, или null, когда её не было.
     * @return История, новейший период первым, не длиннее [BudgetHistory.MAX].
     */
    fun decode(raw: String?): List<BudgetResult> {
        if (raw.isNullOrBlank()) return emptyList()
        return BudgetHistory.of(raw.split(RECORD_SEPARATOR).mapNotNull { decodeRecord(it) })
    }

    private fun decodeRecord(record: String): BudgetResult? {
        val fields = record.split(FIELD_SEPARATOR)
        val startDay = fields.firstOrNull()?.toLongOrNull()
        val numbers = fields.drop(1).map { it.toIntOrNull() }
        if (fields.size != FIELDS_PER_RECORD || startDay == null || numbers.any { it == null }) {
            Log.w(TAG, "Dropped a malformed budget history record: '$record'")
            return null
        }
        val n = numbers.map { it!! }
        return BudgetResult(
            plannedMust = n[0],
            actualMust = n[1],
            plannedWant = n[2],
            actualWant = n[3],
            plannedSavings = n[4],
            actualSavings = n[5],
            plannedDeposit = n[6],
            startDay = startDay
        )
    }
}
