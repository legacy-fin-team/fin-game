package com.legacy.fingame.ui

// Hardcoded demo ids for UI wiring only. Not a data model — real data comes from
// the data layer later; screens accept these as parameters with these as defaults.
object DemoContent {
    val categoryIds: List<String> = listOf("food", "toys", "clothes", "decor")

    val itemIds: List<String> = listOf(
        "item_01", "item_02", "item_03", "item_04",
        "item_05", "item_06", "item_07", "item_08",
        "item_09", "item_10"
    )

    val subLocationCount: Int = 4

    val subLocationTitles: List<String> = listOf(
        "Гостиная", "Кухня", "Спальня", "Двор"
    )

    const val petId: String = "cat"
}
