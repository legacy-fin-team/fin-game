package com.legacy.fingame

data class AnimalVariant(
    val id: String,
    val path: String
)

data class Animal(
    val id: String,
    val name: String? = null,
    val variants: List<AnimalVariant>
)
