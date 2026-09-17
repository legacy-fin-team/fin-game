package com.legacy.fingame.game.animals

data class AnimalVariant(
    val id: String
)

data class Animal(
    val id: String,
    val variantsPath: String,
    val variants: List<AnimalVariant>
    // При необходимости новые свойства (теги/атрибуты) можно легко добавлять сюда
)
