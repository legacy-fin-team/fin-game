package com.legacy.fingame.game.items

import com.legacy.fingame.game.economy.SpendKind
import com.legacy.fingame.game.scene.GameLayer

/**
 * A section of the shop, and with it the way its items behave once the player owns them.
 *
 * @property xmlName name the data files use to put an item into this category.
 * @property use what using an item of this category does to it; see [ItemUse].
 * @property defaultLayer layer of the game area an item of this category is drawn on while it is
 * worn, or null for categories whose items are never drawn there. An item may name another layer in
 * its own data (see [Item.layer]) — a decoration that stands in front of the pet, for one.
 * @property spendKind what buying an item of this category counts as in the period's plan: the one
 * place saying which shop section is a necessity and which one is not.
 */
enum class ItemCategory(
    val xmlName: String,
    val use: ItemUse,
    val defaultLayer: GameLayer?,
    val spendKind: SpendKind
) {
    FOOD("food", ItemUse.CONSUMED, null, SpendKind.MUST),
    TOYS("toys", ItemUse.REUSABLE, null, SpendKind.MUST),
    CLOTHES("clothes", ItemUse.WEARABLE, GameLayer.CLOTHES, SpendKind.WANT),
    DECOR("decor", ItemUse.WEARABLE, GameLayer.ENVIRONMENT_BACK, SpendKind.WANT);

    /**
     * What the button that puts an item of this category on the pet, or takes it off, says.
     *
     * Clothes are put on and taken off; a decoration is stood in the room and taken away from it —
     * the same toggle worded for what each category's items actually do, so the inventory asks the
     * category for the word instead of guessing it itself.
     *
     * Meaningful only for a category whose items are [ItemUse.WEARABLE]; a caller already checks
     * that through [Item.isWearable] before it ever asks for the label.
     *
     * @param worn whether the item is currently on the pet (or standing in the room).
     * @return The Russian label of the action.
     */
    fun wearActionTitle(worn: Boolean): String = when (this) {
        DECOR -> if (worn) "Убрать" else "Поставить"
        else -> if (worn) "Снять" else "Надеть"
    }

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
