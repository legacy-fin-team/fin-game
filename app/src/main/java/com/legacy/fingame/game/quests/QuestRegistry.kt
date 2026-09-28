package com.legacy.fingame.game.quests

import android.content.Context

/**
 * Квесты приложения, прочитанные из [QUESTS_FILE] один раз при создании — так же, как
 * [com.legacy.fingame.game.items.ItemRegistry] читает предметы.
 *
 * @param context контекст приложения, из него берутся assets.
 */
class QuestRegistry(context: Context) : QuestCatalog {

    companion object {
        /** Файл с квестами относительно `assets/`. */
        const val QUESTS_FILE = "data/quests.xml"
    }

    private val catalog: QuestCatalog = QuestCatalog.of(
        context.assets.open(QUESTS_FILE).use { stream -> QuestReader().readQuests(stream) }
    )

    override val quests: List<Quest> get() = catalog.quests

    override fun findQuestById(questId: String): Quest? = catalog.findQuestById(questId)
}
