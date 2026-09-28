package com.legacy.fingame.game.quests

/**
 * Тема финансовой грамотности, которой учит квест, — по ней взрослый видит в разделе «Прогресс»,
 * какие темы ребёнок уже прошёл (см. [com.legacy.fingame.game.adult.AdultProgress]).
 *
 * @property xmlName как тема записана в атрибуте `topic` файла `data/quests.xml`.
 * @property title название темы для взрослого.
 */
enum class QuestTopic(val xmlName: String, val title: String) {
    /** Планировать траты заранее: бюджет периода. */
    PLANNING("planning", "Планирование"),

    /** Отличать обязательное от желаемого. */
    NEEDS_WANTS("needs-wants", "Обязательное и желаемое"),

    /** Откладывать и копить. */
    SAVING("saving", "Накопление"),

    /** Вклад: деньги работают, пока лежат. */
    DEPOSIT("deposit", "Вклад"),

    /** Ставить цель и доходить до неё. */
    GOALS("goals", "Цели"),

    /** Честно обходиться с деньгами — своими и чужими. */
    HONESTY("honesty", "Честность");

    companion object {

        /**
         * @param value значение атрибута `topic`.
         * @return Тема с этим именем, или null, когда такой темы нет.
         */
        fun fromString(value: String): QuestTopic? =
            entries.find { it.xmlName.equals(value.trim(), ignoreCase = true) }
    }
}
