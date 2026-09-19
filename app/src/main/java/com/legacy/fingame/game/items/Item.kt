package com.legacy.fingame.game.items

data class Item(
    val id: String,
    val price: Int,
    val category: ItemCategory,
    val variants: Map<String, String>
)
