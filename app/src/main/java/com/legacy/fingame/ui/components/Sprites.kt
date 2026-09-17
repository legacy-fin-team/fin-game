package com.legacy.fingame.ui.components

/**
 * Asset paths (relative to `assets/textures/`) for all UI sprites used by game components.
 *
 * Every path here is resolved by [com.legacy.fingame.utils.SpriteLoader]. If the referenced file
 * does not exist yet, the loader falls back to `error.webp` — that is expected behavior while
 * assets are being produced, not a bug.
 */
object Sprites {
    // TODO: asset file names are provisional — confirm the final naming convention with the art team.

    /** Path to the settings icon sprite. */
    const val SETTINGS = "ui/settings.webp"

    /** Path to the locations icon sprite. */
    const val LOCATIONS = "ui/locations.webp"

    /** Path to the quests icon sprite. */
    const val QUESTS = "ui/quests.webp"

    /** Path to the inventory icon sprite. */
    const val INVENTORY = "ui/inventory.webp"

    /** Path to the shop icon sprite. */
    const val SHOP = "ui/shop.webp"

    /** Path to the left-arrow navigation sprite. */
    const val ARROW_LEFT = "ui/arrow_left.webp"

    /** Path to the right-arrow navigation sprite. */
    const val ARROW_RIGHT = "ui/arrow_right.webp"

    /** Path to the close ("X") icon sprite. */
    const val CLOSE = "ui/close.webp"

    /** Path to the filled/"on" star icon sprite. */
    const val STAR_ON = "ui/star_on.webp"

    /** Path to the empty/"off" star icon sprite. */
    const val STAR_OFF = "ui/star_off.webp"

    /** Path to the plus ("+") icon sprite. */
    const val PLUS = "ui/plus.webp"

    /** Path to the minus ("-") icon sprite. */
    const val MINUS = "ui/minus.webp"

    /** Path to the coin icon sprite, used by [BalanceChip]. */
    const val COIN = "ui/coin.webp"

    /**
     * Builds the asset path for a pet's idle sprite.
     *
     * @param petId identifier of the pet whose idle sprite should be resolved; used verbatim as a
     *   path segment, so it must match the pet's asset folder name.
     * @return path (relative to `assets/textures/`) to the pet's idle sprite.
     */
    fun pet(petId: String) = "animals/$petId/idle.webp"

    /**
     * Builds the asset path for a shop item's sprite.
     *
     * @param itemId identifier of the shop item whose sprite should be resolved; used verbatim as
     *   a path segment, so it must match the item's asset file name.
     * @return path (relative to `assets/textures/`) to the shop item's sprite.
     */
    fun shopItem(itemId: String) = "shop/items/$itemId.webp"

    /**
     * Builds the asset path for a shop category's sprite.
     *
     * @param categoryId identifier of the shop category whose sprite should be resolved; used
     *   verbatim as a path segment, so it must match the category's asset file name.
     * @return path (relative to `assets/textures/`) to the shop category's sprite.
     */
    fun shopCategory(categoryId: String) = "shop/categories/$categoryId.webp"
}
