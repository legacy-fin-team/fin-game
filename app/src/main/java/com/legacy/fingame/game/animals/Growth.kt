package com.legacy.fingame.game.animals

import java.util.concurrent.TimeUnit

/**
 * How the pet grows up: the rule that turns how much the pet has grown into the age stage its
 * sprites are drawn at.
 *
 * How much the pet has grown is kept by [com.legacy.fingame.game.rules.PetCare]: every day of the
 * pet's life ([DAY_MILLIS]) a well cared for pet grows by one [DAY_MILLIS], a neglected one slower
 * or not at all (see [com.legacy.fingame.game.rules.PetCareRules.growthMultiplier]). Growing never
 * runs faster than that, so a pet takes at least [FULL_GROWTH_DAYS] days to grow up — exactly that
 * many when it is well cared for every single day.
 *
 * The animals of the game are drawn in [ADULT_AGE] + 1 stages (baby, young, grown up, see
 * `assets/data/animals.xml`), and those stages are spread evenly over [FULL_GROWTH_DAYS]: each takes
 * [STAGE_MILLIS] of growing. An animal with more stages than that simply stays at [ADULT_AGE] —
 * [Animal.coerceAge] keeps the answer within the stages that animal has, for its own sprite and
 * for the clothes on it alike.
 */
object Growth {

    /** Value of [com.legacy.fingame.game.PlayerState.petBornAtMillis] meaning "no pet was taken in yet". */
    const val NOT_BORN = Long.MIN_VALUE

    /**
     * Length of the pet's own day its care is judged by, and how much a well cared for pet grows in
     * such a day.
     */
    val DAY_MILLIS: Long = TimeUnit.DAYS.toMillis(1)

    /** Days of good care it takes the pet to grow up completely: three weeks. */
    const val FULL_GROWTH_DAYS = 21

    /** Age stage of a grown up pet, counted from [Animal.FIRST_AGE]: the last one it is drawn at. */
    const val ADULT_AGE = Animal.FIRST_AGE + 2

    /** How much growing takes the pet from being taken in to [ADULT_AGE]. */
    val FULL_GROWTH_MILLIS: Long = FULL_GROWTH_DAYS * DAY_MILLIS

    /**
     * How much growing takes the pet from one age stage to the next one: [FULL_GROWTH_DAYS] split
     * evenly between the stages, i.e. ten and a half days. A well cared for pet therefore turns
     * young on its 11th day and grows up on its 21st.
     */
    val STAGE_MILLIS: Long = FULL_GROWTH_MILLIS / (ADULT_AGE - Animal.FIRST_AGE)

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
