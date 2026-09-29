package com.legacy.fingame.game.stats

/**
 * A stat of the pet: one of the bars the player keeps topped up by using items on the pet.
 *
 * Every stat works the same way — a bar between [PetStats.MIN_VALUE] and [PetStats.MAX_VALUE] where
 * full is good and empty is bad — so the system grows by adding an entry here and nothing else:
 * [PetStats] iterates the entries, the items name them in their data, and the screens draw whatever
 * the pet has. [StatKind.HUNGER] therefore counts how *fed* the pet is: the bar empties as the pet
 * gets hungry.
 *
 * @property xmlName name the item data files use to name this stat in an item's effect.
 * @property decayPerTick how much of this stat the pet loses every [PetStats.TICK_MILLIS], i.e. how
 * fast the player has to keep coming back for this particular bar.
 */
enum class StatKind(val xmlName: String, val decayPerTick: Int) {
    /** How healthy the pet is; falls like the other bars. */
    HEALTH("health", 1),

    /** How fed the pet is: a full bar is a pet that is not hungry at all. */
    HUNGER("hunger", 1),

    /** How happy the pet is; kept up by playing with it and by what it wears. */
    PLEASURE("pleasure", 1);

    companion object {

        /**
         * @param statId name of a stat, as the data files write it.
         * @return The stat with that name, or null when the pet has no such stat — a stat named by
         * data that changed is simply not part of the game any more.
         */
        fun fromString(statId: String): StatKind? = entries.find {
            it.xmlName.equals(statId, ignoreCase = true)
        }
    }
}
