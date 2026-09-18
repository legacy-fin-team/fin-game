package com.legacy.fingame.game.items

data class Item(
    val id: String,
    val price: Int,
    val type: String,
    val category: ItemCategory,
    val variants: Map<String, String> // id варианта -> полный путь до файла/папки
)
