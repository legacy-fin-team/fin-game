package com.legacy.fingame.domain

/** Категория товара в магазине. */
enum class ItemCategory(val title: String, val emoji: String) {
    FOOD("Еда", "🍖"),
    TOYS("Игрушки", "🧸"),
    CLOTHES("Одежда", "👕"),
    DECOR("Декор", "🪴")
}

/** CONSUMABLE — расходуемый товар (кол-во +/-); UNIQUE — покупается один раз (Добавить/Убрать/Куплено). */
enum class PurchaseKind { CONSUMABLE, UNIQUE }

data class ShopItem(
    val id: String,
    val title: String,
    val price: Int,
    val category: ItemCategory,
    val kind: PurchaseKind,
    val emoji: String,
    val spritePath: String? = null
)

/** Цель накопления — привязана к конкретному товару в магазине. */
data class Goal(
    val itemId: String,
    val title: String,
    val targetPrice: Int,
    val emoji: String
)

/** Питомец, доступный игроку. */
data class Pet(
    val id: String,
    val name: String,
    val emoji: String,
    val spritePath: String? = null
)
