package com.legacy.fingame.game.animals

/**
 * An animal the player can keep as a pet.
 *
 * @property id id of the animal.
 * @property name name of the animal shown to the player.
 * @property ageCount number of age (growth) stages the animal has. Stages are numbered from
 * [FIRST_AGE] to `ageCount - 1`, every animal has its own number of them.
 * @property variants map of paths to animal variants. The key is the animal variant id.
 * The value is the path to the variant folder relative to /assets/textures/. That folder holds
 * one folder per age stage.
 */
data class Animal(
    val id: String,
    val name: String,
    val ageCount: Int,
    val variants: Map<String, String>
) {
    companion object {
        /** The first (the youngest) age stage, the one every animal has. */
        const val FIRST_AGE = 0

        /** Number of age stages of an animal whose data doesn't declare them. */
        const val DEFAULT_AGE_COUNT = 1

        /** Name of the idle sprite file inside an age stage folder. */
        const val IDLE_SPRITE_FILE = "idle.webp"
    }

    /**
     * Coerces [age] to the valid age stages of this animal (between [FIRST_AGE] and `ageCount - 1`).
     */
    fun coerceAge(age: Int): Int = age.coerceIn(FIRST_AGE, (ageCount - 1).coerceAtLeast(FIRST_AGE))

    /**
     * Builds the path to the idle sprite of an animal variant at the given age stage.
     *
     * @param variantId id of an animal variant.
     * @param age age stage of the animal. Values outside of `FIRST_AGE until ageCount` are
     * coerced into that range, so callers don't have to know how many stages this animal has.
     * @return Path to the idle sprite relative to /assets/textures/,
     * e.g. 'animals/cat/orange/0/idle.webp'.
     */
    fun getIdleSpritePath(variantId: String, age: Int): String {
        val variantPath = variants.getValue(variantId)
        val ageStage = coerceAge(age)
        return "$variantPath/$ageStage/$IDLE_SPRITE_FILE"
    }
}
