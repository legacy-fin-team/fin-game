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
    DECOR("decor", ItemUse.WEARABLE, GameLayer.ENVIRONMENT_BACK, SpendKind.WANT),

    /**
     * «Другое» — награды из жизни от взрослого, которые сама игра не использует; в магазине виден,
     * только когда не пуст. Предмет отсюда можно только «Использовать» один раз ([ItemUse.REDEEMED]).
     */
    OTHER("other", ItemUse.REDEEMED, null, SpendKind.WANT);

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
