package com.legacy.fingame.game.rules

import com.legacy.fingame.game.items.Item
import com.legacy.fingame.game.items.ItemCatalog
import com.legacy.fingame.game.items.ItemCategory

/**
 * Каталог магазина с ценами, которые видит и платит игрок: необязательные товары дорожают, пока
 * питомцем не занимаются (см. [PetCareRules.priceOf]), обязательные — никогда.
 *
 * Цена считается при каждом запросе из текущей серии дней без заботы, поэтому карточка товара,
 * корзина, цели и сама покупка всегда сходятся в одной цифре.
 *
 * @param base каталог с обычными ценами.
 * @param tuning правила ухода.
 * @param neglectStreak откуда брать текущую серию дней без заботы.
 */
class CarePricedCatalog(
    private val base: ItemCatalog,
    private val tuning: PetCareTuning = PetCareTuning.DEFAULT,
    private val neglectStreak: () -> Int
) : ItemCatalog {

    override fun getItemsByCategory(category: ItemCategory): List<Item> {
        val streak = neglectStreak()
        return base.getItemsByCategory(category).map { priced(it, streak) }
    }

    override fun findItemById(itemId: String): Item? =
        base.findItemById(itemId)?.let { priced(it, neglectStreak()) }

    private fun priced(item: Item, streak: Int): Item {
        val price = PetCareRules.priceOf(item.price, item.category.spendKind, streak, tuning)
        return if (price == item.price) item else item.copy(price = price)
    }
}
