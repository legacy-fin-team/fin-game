package com.legacy.fingame.game.items

enum class ItemCategory(val xmlName: String) {
    FOOD("food"),
    TOYS("toys"),
    CLOTHES("clothes"),
    DECOR("decor");

    companion object {
        /**
         * @param categoryId an if of the category.
         * @return [ItemCategory] if id is valid. Null in case it doesn't.
         */
        fun fromString(categoryId: String): ItemCategory? {
            return entries.find {
                it.xmlName.equals(categoryId, ignoreCase = true)
            }
        }
    }
}
