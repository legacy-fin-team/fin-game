package com.legacy.fingame.game.animals

import java.util.concurrent.TimeUnit

/**
 * How the pet grows up: the rule that turns how much the pet has grown into the age stage its
 * sprites are drawn at.
 *
 * How much the pet has grown is kept by [com.legacy.fingame.game.rules.PetCare]: a well cared for
 * pet grows by one [STAGE_MILLIS] a day, a neglected one slower or not at all (see
 * [com.legacy.fingame.game.rules.PetCareRules.growthMultiplier]).
 *
 * How many stages a pet actually has is the animal's own business: this object counts stages from
 * [Animal.FIRST_AGE] up without an upper bound, and [Animal.coerceAge] keeps the answer within
 * the stages that animal has — for its own sprite and for the clothes on it alike.
 */
object Growth {

    /** Value of [com.legacy.fingame.game.PlayerState.petBornAtMillis] meaning "no pet was taken in yet". */
    const val NOT_BORN = Long.MIN_VALUE

    /**
     * How much growing takes the pet from one age stage to the next one: a day of growing at the
     * normal pace. It is also the length of the pet's own day its care is judged by.
     */
    val STAGE_MILLIS: Long = TimeUnit.DAYS.toMillis(1)

    /**
     * Tells which age stage a pet is at.
     *
     * @param growthMillis how much the pet has grown, see
     * [com.legacy.fingame.game.rules.PetCare.growthMillis]; zero or less is the youngest stage.
     * @return Age stage of the pet, counted from [Animal.FIRST_AGE].
     */
    fun ageOf(growthMillis: Long): Int {
        if (growthMillis < STAGE_MILLIS) return Animal.FIRST_AGE
        val stages = growthMillis / STAGE_MILLIS
        return (Animal.FIRST_AGE + stages).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}
