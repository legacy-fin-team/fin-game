package com.legacy.fingame

import java.util.concurrent.TimeUnit

/**
 * What a demo build is allowed to do on top of the game itself.
 *
 * The game is played in real time — the pet gets hungry and grows over hours and days — which is
 * exactly what nobody showing the game has: a demo lasts minutes. So a demo build hands whoever is
 * showing it a button that pushes the game's clock forward instead (see
 * [com.legacy.fingame.game.GameViewModel.fastForward]), and the pet lives through that time the same
 * way it would have lived through the wait.
 *
 * Whether the build is a demo one is decided by the build configuration and nothing else, so the
 * button cannot reach a player: it is there in the debug build and gone from the release one, unless
 * the build was asked otherwise with `-Pfingame.demoMode=...`.
 */
object DemoMode {

    /** Whether this build shows the demo tools at all. */
    val ENABLED: Boolean = BuildConfig.DEMO_MODE

    /**
     * How far one press of the demo's time button pushes the game's clock, in hours. Half a day, so
     * two presses make a day: the pet is visibly hungry after the first one and has grown up — and
     * has a new daily bonus waiting — after the second.
     */
    const val FAST_FORWARD_HOURS = 12L

    /** [FAST_FORWARD_HOURS] as the milliseconds the game's clock is pushed by. */
    val FAST_FORWARD_MILLIS: Long = TimeUnit.HOURS.toMillis(FAST_FORWARD_HOURS)
}
