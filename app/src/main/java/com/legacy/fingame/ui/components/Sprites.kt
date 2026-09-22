package com.legacy.fingame.ui.components

import com.legacy.fingame.game.animals.Animal

/**
 * Asset paths (relative to `assets/textures/`) for all UI sprites used by game components.
 *
 * Every path here is resolved by [com.legacy.fingame.utils.SpriteLoader]. If the referenced file
 * does not exist yet, the loader falls back to `error.webp` — that is expected behavior while
 * assets are being produced, not a bug.
 *
 * The sprites of the shop items themselves are not here: they are laid out per item rather than per
 * screen, so their paths are built by [com.legacy.fingame.game.items.ItemSprites].
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
    const val ARROW_LEFT = "ui/arrow-left.webp"

    /** Path to the right-arrow navigation sprite. */
    const val ARROW_RIGHT = "ui/arrow-right.webp"

    /** Path to the close ("X") icon sprite. */
    const val CLOSE = "ui/close.webp"

    /** Path to the filled/"on" star icon sprite. */
    const val STAR_ON = "ui/star-on.webp"

    /** Path to the empty/"off" star icon sprite. */
    const val STAR_OFF = "ui/star-off.webp"

    /** Path to the plus ("+") icon sprite. */
    const val PLUS = "ui/plus.webp"

    /** Path to the minus ("-") icon sprite. */
    const val MINUS = "ui/minus.webp"

    /** Path to the coin icon sprite, used by [BalanceChip]. */
    const val COIN = "ui/coin.webp"

    /**
     * Path to the budget icon sprite. The file does not exist yet — until the art team delivers
     * it, [SpriteButton] draws its `label` fallback instead of falling back to `error.webp`.
     */
    const val BUDGET = "ui/budget.webp"

    /**
     * Path to the log icon sprite. The file does not exist yet — until the art team delivers it,
     * [SpriteButton] draws its `label` fallback instead of falling back to `error.webp`.
     */
    const val LOG = "ui/log.webp"

    /**
     * Builds the asset path for the icon of a pet stat, the one [StatChip] shows instead of naming
     * the stat in words.
     *
     * @param statId identifier of the stat, as the data files write it (see
     *   [com.legacy.fingame.game.stats.StatKind.xmlName]); used verbatim as a path segment, so it
     *   must match the icon's file name.
     * @return path (relative to `assets/textures/`) to the stat's icon.
     */
    fun stat(statId: String) = "ui/stats/$statId.webp"

    /**
     * Builds the asset path for a pet sprite the way the animal data files lay them out.
     *
     * An animal is identified by two ids: the species and the variant it was created with, and it
     * grows through several age stages, so all three are used as path segments (for example `cat` +
     * `white` + `0` resolves to `animals/cat/white/0/idle.webp`).
     *
     * This builder assumes the standard `animals/` layout and is meant for previews and demo
     * content; the running app resolves the path through
     * [com.legacy.fingame.game.animals.AnimalRegistry.getIdleSpritePath], which uses the path
     * declared by the animal's data and knows how many age stages that animal has.
     *
     * @param petId species identifier of the animal, used verbatim as a path segment.
     * @param variantId variant identifier of the animal (its colouring or skin), used verbatim as
     *   a path segment.
     * @param age age stage of the animal, used verbatim as a path segment; defaults to the first
     *   stage every animal has.
     * @return path relative to `assets/textures/` pointing at the pet's idle sprite.
     */
    fun pet(petId: String, variantId: String, age: Int = Animal.FIRST_AGE) =
        "animals/$petId/$variantId/$age/${Animal.IDLE_SPRITE_FILE}"

    /**
     * Builds the asset path for the scenery of a sub-location, i.e. what is drawn on
     * [com.legacy.fingame.game.scene.GameLayer.BACKGROUND] while the pet is there.
     *
     * @param subLocationIndex index of the sub-location the pet is in, used verbatim as a path
     *   segment.
     * @return path (relative to `assets/textures/`) to the sub-location's background.
     */
    fun locationBackground(subLocationIndex: Int) = "locations/$subLocationIndex/background.webp"

    /**
     * Builds the asset path for a shop category's sprite.
     *
     * @param categoryId identifier of the shop category whose sprite should be resolved; used
     *   verbatim as a path segment, so it must match the category's asset file name.
     * @return path (relative to `assets/textures/`) to the shop category's sprite.
     */
    fun shopCategory(categoryId: String) = "shop/categories/$categoryId.webp"
}
