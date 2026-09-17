package com.legacy.fingame.ui

/**
 * Hardcoded demo ids and titles used to wire up the UI. This is not a data model —
 * screens accept these values as parameters with these as their defaults, and the
 * real data will come from the data layer later.
 */
object DemoContent {
    /** Shop category ids. TODO: replace with real categories from the data layer built by the team. */
    val categoryIds: List<String> = listOf("food", "toys", "clothes", "decor")

    /** Shop item ids. TODO: replace with real items from the data layer built by the team. */
    val itemIds: List<String> = listOf(
        "item_01", "item_02", "item_03", "item_04",
        "item_05", "item_06", "item_07", "item_08",
        "item_09", "item_10"
    )

    /** Number of sub-locations. TODO: replace with the real sub-location count from the data layer built by the team. */
    val subLocationCount: Int = 4

    /** Sub-location display titles, in Russian. TODO: replace with real sub-location names from the data layer built by the team. */
    val subLocationTitles: List<String> = listOf(
        "Гостиная", "Кухня", "Спальня", "Двор"
    )

    /** Id of the player's pet. TODO: replace with the real pet id from the data layer built by the team. */
    const val petId: String = "cat"
}
