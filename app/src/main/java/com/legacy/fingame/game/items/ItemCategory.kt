package com.legacy.fingame.game.items

enum class ItemCategory(val xmlName: String) {
    FOOD("food"),
    TOYS("toys"),
    CLOTHES("clothes"),
    DECOR("decor");

    companion object {
        /**
         * Поиск категории по строке из XML (игнорирует регистр).
         */
        fun fromString(value: String): ItemCategory {
            return entries.find { it.xmlName.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Неизвестная категория предметов: $value")
        }
    }
}
