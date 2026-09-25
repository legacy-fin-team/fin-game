package com.legacy.fingame.game.animals

/**
 * The pet the player picked on the animal selection screen.
 *
 * An animal is identified by its id plus the id of the variant it was created with,
 * so both are needed to resolve its sprites.
 *
 * @property animalId id of the chosen animal.
 * @property variantId id of the chosen variant of that animal.
 */
data class AnimalSelection(
    val animalId: String,
    val variantId: String
)
