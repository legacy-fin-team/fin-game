package com.legacy.fingame.game.quests

/**
 * Какие квесты есть в игре — так, как их видит игровая логика. Откуда они прочитаны, ей всё
 * равно; в приложении это [QuestRegistry], в тестах и превью — [QuestCatalog.of].
 */
interface QuestCatalog {

    /** Все квесты в порядке данных. */
    val quests: List<Quest>

    /**
     * @param questId id квеста.
     * @return Квест, или null, когда его нет: id может прийти из сохранения, сделанного, когда
     * квест ещё был в данных.
     */
    fun findQuestById(questId: String): Quest?

    companion object {

        /**
         * @param quests квесты в нужном порядке.
         * @return Каталог ровно из этих квестов.
         */
        fun of(quests: List<Quest>): QuestCatalog = ListQuestCatalog(quests)

        /** Каталог без квестов — для тестов и превью, которым квесты не нужны. */
        val EMPTY: QuestCatalog = of(emptyList())
    }
}

/** [QuestCatalog] поверх готового списка. */
private class ListQuestCatalog(override val quests: List<Quest>) : QuestCatalog {

    private val byId: Map<String, Quest> = quests.associateBy { it.id }

    override fun findQuestById(questId: String): Quest? = byId[questId]
}
