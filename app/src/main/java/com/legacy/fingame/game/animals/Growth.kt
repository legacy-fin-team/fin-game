package com.legacy.fingame.game.animals

import java.util.concurrent.TimeUnit

/**
 * How the pet grows up: the rule that turns the time the player has had the pet for into the age
 * stage its sprites are drawn at.
 *
 * The stage is never stored — it is worked out from the moment the pet was taken in — so it cannot
 * drift away from the calendar, and a pet grows while the app is closed as well.
 *
 * How many stages a pet actually has is the animal's own business: this object counts stages from
 * [Animal.FIRST_AGE] up without an upper bound, and [Animal.getAgeStage] keeps the answer within
 * the stages that animal has — for its own sprite and for the clothes on it alike.
 */
object Growth {

    /** Value of [com.legacy.fingame.game.PlayerState.petBornAtMillis] meaning "no pet was taken in yet". */
    const val NOT_BORN = Long.MIN_VALUE

    /** How long the pet stays at one age stage before it grows into the next one. */
    val STAGE_MILLIS: Long = TimeUnit.DAYS.toMillis(1)

    /**
     * Tells which age stage a pet is at.
     *
     * A pet whose age cannot be told — none was taken in yet, or the device clock has been moved
     * back behind the day it was — is at [Animal.FIRST_AGE]: the youngest stage is the one every
     * animal has, so there is always something to draw.
     *
     * @param bornAtMillis moment the pet was taken in, in milliseconds, or [NOT_BORN].
     * @param nowMillis current moment, in milliseconds.
     * @return Age stage of the pet, counted from [Animal.FIRST_AGE].
     */
    fun ageAt(bornAtMillis: Long, nowMillis: Long): Int {
        if (bornAtMillis == NOT_BORN) return Animal.FIRST_AGE

        val lived = nowMillis - bornAtMillis
        if (lived < STAGE_MILLIS) return Animal.FIRST_AGE

        val stages = lived / STAGE_MILLIS
        return (Animal.FIRST_AGE + stages).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}
