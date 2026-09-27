package com.legacy.fingame.utils

import android.util.Log
import com.legacy.fingame.game.quests.Quest
import com.legacy.fingame.game.quests.QuestOutcome
import com.legacy.fingame.game.quests.QuestProgress
import com.legacy.fingame.game.quests.QuestStatus
import com.legacy.fingame.game.stats.StatKind

/**
 * Состояние квестов игрока как одна строка в настройках — по образцу [MoneyLogCodec].
 *
 * Запись от записи отделяет record separator (`\u001E`), поле от поля — unit separator (`\u001F`),
 * эффекты на статы внутри поля — group separator (`\u001D`), а стат от значения — `=`. Все три
 * управляющих символа вычищаются из текстов при кодировании, так что формат не ломается.
 *
 * Поля записи: id квеста, узел, момент доступности, прогресс, статус, есть ли выбор (`0`/`1`),
 * надпись варианта, текст результата, куда ведёт, эффекты, деньги, изменение прогресса и, самым
 * последним, флаг «взрослый включил квест снова» (`0`/`1`, [QuestProgress.enabledAgain]). Без
 * выбора шесть полей после него пустые/нулевые.
 *
 * Запись без последнего поля (старый сейв, где [QuestProgress.enabledAgain] ещё не было) читается
 * как есть — тринадцатое поле тогда считается ложью, ровно как оно и было раньше.
 */
object QuestStateCodec {

    private const val TAG = "QuestStateCodec"

    private const val RECORD_SEPARATOR = '\u001E'
    private const val FIELD_SEPARATOR = '\u001F'
    private const val EFFECT_SEPARATOR = '\u001D'
    private const val EFFECT_VALUE_SEPARATOR = '='

    private const val FIELDS_PER_RECORD = 13

    /** Сколько полей была запись до [QuestProgress.enabledAgain] — старые сейвы читаются и так. */
    private const val LEGACY_FIELDS_PER_RECORD = 12

    private const val NO_CHOICE = "0"
    private const val HAS_CHOICE = "1"
    private const val NOT_ENABLED_AGAIN = "0"
    private const val ENABLED_AGAIN = "1"

    /**
     * @param quests состояние квестов игрока.
     * @return Одна строка; пустая, когда квестов нет.
     */
    fun encode(quests: List<QuestProgress>): String =
        quests.joinToString(RECORD_SEPARATOR.toString()) { encodeRecord(it) }

    /**
     * Читает то, что написал [encode]. Запись, которая не разбирается, отбрасывается, а не роняет
     * весь разбор; вторая запись того же квеста — тоже.
     *
     * @param raw строка из настроек, или null, когда её не было.
     * @return Состояние квестов; пустое, когда строки нет или она пустая.
     */
    fun decode(raw: String?): List<QuestProgress> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(RECORD_SEPARATOR)
            .mapNotNull { decodeRecord(it) }
            .distinctBy { it.questId }
    }

    private fun encodeRecord(progress: QuestProgress): String {
        val choice = progress.lastChoice
        return listOf(
            clean(progress.questId),
            clean(progress.nodeId),
            progress.availableAtMillis.toString(),
            progress.progress.toString(),
            progress.status.name,
            if (choice == null) NO_CHOICE else HAS_CHOICE,
            clean(choice?.optionLabel.orEmpty()),
            clean(choice?.resultText.orEmpty()),
            clean(choice?.nextNodeId.orEmpty()),
            encodeEffects(choice?.statEffects.orEmpty()),
            (choice?.moneyDelta ?: 0).toString(),
            (choice?.progressDelta ?: 0).toString(),
            if (progress.enabledAgain) ENABLED_AGAIN else NOT_ENABLED_AGAIN
        ).joinToString(FIELD_SEPARATOR.toString())
    }

    private fun decodeRecord(record: String): QuestProgress? {
        val fields = record.split(FIELD_SEPARATOR)
        val enabledAgain = when {
            fields.size == FIELDS_PER_RECORD -> when (fields[LEGACY_FIELDS_PER_RECORD]) {
                NOT_ENABLED_AGAIN -> false
                ENABLED_AGAIN -> true
                else -> return dropped(record)
            }
            // Сейв прежней версии, без флага «включён взрослым снова»: читается как есть, флаг —
            // ложь, ровно как он и вёл себя до появления этого поля.
            fields.size == LEGACY_FIELDS_PER_RECORD -> false
            else -> return dropped(record)
        }

        val questId = fields[0]
        val nodeId = fields[1]
        val availableAtMillis = fields[2].toLongOrNull()
        val progress = fields[3].toIntOrNull()
        val status = QuestStatus.entries.find { it.name == fields[4] }
        if (questId.isBlank() || nodeId.isBlank() || availableAtMillis == null ||
            progress == null || status == null
        ) {
            return dropped(record)
        }

        val lastChoice = when (fields[5]) {
            NO_CHOICE -> null
            HAS_CHOICE -> decodeChoice(fields) ?: return dropped(record)
            else -> return dropped(record)
        }

        return QuestProgress(
            questId = questId,
            nodeId = nodeId,
            availableAtMillis = availableAtMillis,
            progress = progress.coerceIn(Quest.MIN_PROGRESS, Quest.MAX_PROGRESS),
            status = status,
            lastChoice = lastChoice,
            enabledAgain = enabledAgain
        )
    }

    private fun decodeChoice(fields: List<String>): QuestOutcome? {
        if (fields[6].isBlank() || fields[8].isBlank()) return null
        val effects = decodeEffects(fields[9]) ?: return null
        val moneyDelta = fields[10].toIntOrNull() ?: return null
        val progressDelta = fields[11].toIntOrNull() ?: return null
        return QuestOutcome(
            optionLabel = fields[6],
            resultText = fields[7],
            nextNodeId = fields[8],
            statEffects = effects,
            moneyDelta = moneyDelta,
            progressDelta = progressDelta
        )
    }

    private fun encodeEffects(effects: Map<StatKind, Int>): String =
        effects.entries.joinToString(EFFECT_SEPARATOR.toString()) { (stat, value) ->
            "${stat.xmlName}$EFFECT_VALUE_SEPARATOR$value"
        }

    /**
     * @return Эффекты; эффект на стат, которого в игре больше нет, пропускается; null, когда
     * запись испорчена (нет `=` или не число).
     */
    private fun decodeEffects(raw: String): Map<StatKind, Int>? {
        if (raw.isEmpty()) return emptyMap()
        val effects = mutableMapOf<StatKind, Int>()
        for (pair in raw.split(EFFECT_SEPARATOR)) {
            val separator = pair.indexOf(EFFECT_VALUE_SEPARATOR)
            if (separator <= 0) return null
            val value = pair.substring(separator + 1).toIntOrNull() ?: return null
            val stat = StatKind.fromString(pair.substring(0, separator)) ?: continue
            effects[stat] = value
        }
        return effects.toMap()
    }

    private fun clean(text: String): String = text.filterNot {
        it == RECORD_SEPARATOR || it == FIELD_SEPARATOR || it == EFFECT_SEPARATOR
    }

    private fun dropped(record: String): QuestProgress? {
        Log.w(TAG, "Dropped a malformed quest record: '$record'")
        return null
    }
}
