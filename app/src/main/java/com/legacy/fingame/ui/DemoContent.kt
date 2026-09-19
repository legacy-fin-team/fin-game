package com.legacy.fingame.ui

/**
 * Hardcoded demo ids and titles used to wire up the UI. This is not a data model —
 * screens accept these values as parameters with these as their defaults, and the
 * real data will come from the data layer later.
 */
object DemoContent {
    /** Number of sub-locations. TODO: replace with the real sub-location count from the data layer built by the team. */
    val subLocationCount: Int = 4

    /** Sub-location display titles, in Russian. TODO: replace with real sub-location names from the data layer built by the team. */
    val subLocationTitles: List<String> = listOf(
        "Гостиная", "Кухня", "Спальня", "Двор"
    )

    /** Species id of the player's pet. TODO: replace with the real pet id from the data layer built by the team. */
    const val petId: String = "cat"

    /**
     * Variant id of the player's pet: an animal is identified by its species plus the variant it
     * was created with, and both are needed to resolve its sprites.
     * TODO: replace with the real variant id from the data layer built by the team.
     */
    const val petVariantId: String = "white"
}
