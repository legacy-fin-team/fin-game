package com.legacy.fingame.ui.components

/** Asset paths (relative to assets/textures/) for all UI sprites used by game components. */
object Sprites {
    const val SETTINGS = "ui/settings.webp"
    const val LOCATIONS = "ui/locations.webp"
    const val QUESTS = "ui/quests.webp"
    const val INVENTORY = "ui/inventory.webp"
    const val SHOP = "ui/shop.webp"
    const val ARROW_LEFT = "ui/arrow_left.webp"
    const val ARROW_RIGHT = "ui/arrow_right.webp"
    const val CLOSE = "ui/close.webp"
    const val STAR_ON = "ui/star_on.webp"
    const val STAR_OFF = "ui/star_off.webp"
    const val PLUS = "ui/plus.webp"
    const val MINUS = "ui/minus.webp"
    const val COIN = "ui/coin.webp"
    fun pet(petId: String) = "animals/$petId/idle.webp"
    fun shopItem(itemId: String) = "shop/items/$itemId.webp"
    fun shopCategory(categoryId: String) = "shop/categories/$categoryId.webp"
}
