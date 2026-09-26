package com.legacy.fingame.game.quests

import android.util.Log
import com.legacy.fingame.game.stats.StatKind
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Читает квесты из `data/quests.xml`.
 *
 * Разбор — тем же DOM-парсером, что у [com.legacy.fingame.game.items.ItemReader]: он работает и в
 * приложении, и в JVM-тестах. Квест со сломанными данными пропускается целиком с записью в лог,
 * остальные читаются как обычно: полквеста хуже, чем ни одного.
 */
class QuestReader {

    companion object {
        private const val TAG = "QuestReader"

        /** Пробелы и переводы строк внутри текста схлопываются в один пробел. */
        private val WHITESPACE = Regex("\\s+")
    }

    /**
     * @param inputStream поток XML-файла с квестами.
     * @return Исправные квесты в порядке файла.
     */
    fun readQuests(inputStream: InputStream): List<Quest> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
        document.documentElement.normalize()

        val quests = mutableListOf<Quest>()
        val questElements = document.getElementsByTagName("quest")
        for (i in 0 until questElements.length) {
            val element = questElements.item(i) as? Element ?: continue
            val quest = readQuest(element) ?: continue
            if (quests.any { it.id == quest.id }) {
                Log.e(TAG, "At least two quests share the same id: '${quest.id}'")
                continue
            }
            quests += quest
        }

        Log.i(TAG, "Loaded quests: ${quests.size}.")
        return quests.toList()
    }

    /**
     * @param element тег `<quest>`.
     * @return Квест, или null, когда в его данных что-то сломано.
     */
    private fun readQuest(element: Element): Quest? {
        val id = element.getAttribute("id").trim()
        if (id.isEmpty()) {
            Log.e(TAG, "Tag <quest> does not have 'id' attribute.")
            return null
        }

        val title = element.getAttribute("title").trim()
        if (title.isEmpty()) {
            Log.e(TAG, "Quest '$id' does not have 'title' attribute.")
            return null
        }

        val kind = QuestKind.fromString(element.getAttribute("kind"))
        if (kind == null) {
            Log.e(TAG, "Quest '$id' does not have proper 'kind' attribute.")
            return null
        }

        val minBalance = intAttribute(element, "min-balance")
        if (minBalance == null || minBalance < 0) {
            Log.e(TAG, "Quest '$id' does not have proper 'min-balance' attribute.")
            return null
        }

        val hasProgress = when (element.getAttribute("progress").trim().lowercase()) {
            "", "false" -> false
            "true" -> true
            else -> {
                Log.e(TAG, "Quest '$id' does not have proper 'progress' attribute.")
                return null
            }
        }

        val nodes = linkedMapOf<String, QuestNode>()
        val nodeElements = element.getElementsByTagName("node")
        for (i in 0 until nodeElements.length) {
            val nodeElement = nodeElements.item(i) as? Element ?: continue
            val node = readNode(nodeElement, id) ?: return null
            if (nodes.containsKey(node.id)) {
                Log.e(TAG, "Quest '$id' has two nodes with the same id: '${node.id}'")
                return null
            }
            nodes[node.id] = node
        }
        if (nodes.isEmpty()) {
            Log.e(TAG, "Quest '$id' does not have any <node>.")
            return null
        }

        val firstNodeId = element.getAttribute("start").trim()
        if (firstNodeId !in nodes) {
            Log.e(TAG, "Quest '$id' starts at node '$firstNodeId' it does not have.")
            return null
        }

        for (node in nodes.values) {
            for (option in node.options) {
                if (option.nextNodeId != Quest.END_NODE && option.nextNodeId !in nodes) {
                    Log.e(
                        TAG,
                        "Option '${option.label}' of quest '$id' leads to node " +
                            "'${option.nextNodeId}' the quest does not have."
                    )
                    return null
                }
            }
        }

        return Quest(
            id = id,
            title = title,
            description = childText(element, "description").orEmpty(),
            kind = kind,
            firstNodeId = firstNodeId,
            nodes = nodes.toMap(),
            hasProgress = hasProgress,
            minBalance = minBalance,
            imagePath = optionalAttribute(element, "image")
        )
    }

    /**
     * @param element тег `<node>`.
     * @param questId id квеста, для лога.
     * @return Узел, или null, когда он сломан.
     */
    private fun readNode(element: Element, questId: String): QuestNode? {
        val id = element.getAttribute("id").trim()
        if (id.isEmpty() || id == Quest.END_NODE) {
            Log.e(TAG, "A node of quest '$questId' has no id or is called '${Quest.END_NODE}'.")
            return null
        }

        val text = childText(element, "text")
        if (text == null) {
            Log.e(TAG, "Node '$id' of quest '$questId' does not have <text>.")
            return null
        }

        val delayMinutes = intAttribute(element, "delay-minutes")
        if (delayMinutes == null || delayMinutes < 0) {
            Log.e(TAG, "Node '$id' of quest '$questId' has improper 'delay-minutes'.")
            return null
        }

        val options = mutableListOf<QuestOption>()
        val optionElements = element.getElementsByTagName("option")
        for (i in 0 until optionElements.length) {
            val optionElement = optionElements.item(i) as? Element ?: continue
            options += readOption(optionElement, questId, id) ?: return null
        }
        if (options.isEmpty()) {
            Log.e(TAG, "Node '$id' of quest '$questId' does not have any <option>.")
            return null
        }

        return QuestNode(
            id = id,
            text = text,
            options = options.toList(),
            delayMinutes = delayMinutes,
            imagePath = optionalAttribute(element, "image")
        )
    }

    /**
     * @param element тег `<option>`.
     * @param questId id квеста, для лога.
     * @param nodeId id узла, для лога.
     * @return Вариант, или null, когда он сломан.
     */
    private fun readOption(element: Element, questId: String, nodeId: String): QuestOption? {
        val label = element.getAttribute("label").trim()
        val nextNodeId = element.getAttribute("next").trim()
        val resultText = childText(element, "result")
        if (label.isEmpty() || nextNodeId.isEmpty() || resultText == null) {
            Log.e(
                TAG,
                "An option of node '$nodeId' of quest '$questId' misses its label, " +
                    "its <result> or where it leads."
            )
            return null
        }

        val moneyDelta = intAttribute(element, "money")
        val progressDelta = intAttribute(element, "progress")
        if (moneyDelta == null || progressDelta == null) {
            Log.e(TAG, "Option '$label' of quest '$questId' has improper 'money' or 'progress'.")
            return null
        }

        return QuestOption(
            label = label,
            resultText = resultText,
            nextNodeId = nextNodeId,
            statEffects = readEffects(element, questId),
            moneyDelta = moneyDelta,
            progressDelta = progressDelta
        )
    }

    /**
     * Эффекты варианта на полоски питомца. Эффект на неизвестный стат, повтор стата или не число
     * пропускаются с записью в лог — вариант остаётся, как у предметов в `ItemReader`.
     *
     * @param element тег `<option>`.
     * @param questId id квеста, для лога.
     * @return Эффекты по статам.
     */
    private fun readEffects(element: Element, questId: String): Map<StatKind, Int> {
        val effects = mutableMapOf<StatKind, Int>()
        val effectElements = element.getElementsByTagName("effect")
        for (i in 0 until effectElements.length) {
            val effectElement = effectElements.item(i) as? Element ?: continue
            val statName = effectElement.getAttribute("stat")
            val stat = StatKind.fromString(statName)
            val value = effectElement.getAttribute("value").trim().toIntOrNull()
            if (stat == null || value == null || effects.containsKey(stat)) {
                Log.e(TAG, "Quest '$questId' has an improper <effect> on stat '$statName'.")
                continue
            }
            effects[stat] = value
        }
        return effects.toMap()
    }

    /**
     * @param parent тег, в котором ищется дочерний.
     * @param tag имя дочернего тега.
     * @return Текст первого такого тега без лишних пробелов, или null, когда тега нет или он пуст.
     */
    private fun childText(parent: Element, tag: String): String? {
        val children = parent.getElementsByTagName(tag)
        if (children.length == 0) return null
        return children.item(0).textContent
            .replace(WHITESPACE, " ")
            .trim()
            .takeIf { it.isNotEmpty() }
    }

    /**
     * @return Число из атрибута; 0, когда атрибута нет; null, когда там не число.
     */
    private fun intAttribute(element: Element, name: String): Int? {
        val raw = element.getAttribute(name).trim()
        if (raw.isEmpty()) return 0
        return raw.toIntOrNull()
    }

    /**
     * @return Значение атрибута без пробелов по краям, или null, когда атрибута нет или он пуст.
     */
    private fun optionalAttribute(element: Element, name: String): String? =
        element.getAttribute(name).trim().takeIf { it.isNotEmpty() }
}
