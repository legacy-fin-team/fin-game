package com.legacy.fingame.game.quests

/**
 * Одна карточка экрана квестов.
 *
 * @property quest квест из данных.
 * @property progress где в нём игрок, или null, когда квест не начинался.
 */
data class QuestEntry(val quest: Quest, val progress: QuestProgress?)

/** Какие карточки показывает экран квестов и в каком порядке. */
object QuestBoard {

    /**
     * Квесты игрока видны всегда; случайные — только когда хоть раз выпали. Сначала идущие, потом
     * те, что можно взять, в конце пройденные; внутри группы — порядок данных. Запись квеста,
     * которого больше нет в данных, не показывается.
     *
     * @param catalog квесты игры.
     * @param quests состояние квестов игрока.
     * @return Карточки по порядку.
     */
    fun entriesOf(catalog: QuestCatalog, quests: List<QuestProgress>): List<QuestEntry> {
        val byId = quests.associateBy { it.questId }
        return catalog.quests
            .mapNotNull { quest ->
                val progress = byId[quest.id]
                if (quest.kind == QuestKind.RANDOM && progress == null) {
                    null
                } else {
                    QuestEntry(quest, progress)
                }
            }
            .sortedBy { rankOf(it.progress) }
    }

    /** @return Место группы карточки в списке: идущие, свободные, пройденные. */
    private fun rankOf(progress: QuestProgress?): Int = when {
        progress == null -> 1
        progress.isActive -> 0
        else -> 2
    }
}
