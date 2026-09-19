package com.legacy.fingame.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp

/**
 * Smallest screen width, in dp, from which a device is treated as a tablet. This is the same
 * `sw600dp` breakpoint Android itself uses to tell tablets from phones, so the UI switches at
 * the point the platform does instead of at a number of our own.
 */
private const val TabletSmallestWidthDp = 600

/**
 * Factor applied to phone-sized button dimensions on tablet-sized screens. A tablet is held
 * further from the eyes and has room to spare, so the controls are enlarged to stay comfortable
 * to hit; the value is deliberately modest so the layouts keep their phone proportions.
 */
private const val TabletButtonScale = 1.3f

/**
 * Size tokens that depend on the screen the app is running on. The values are written for phones
 * and scaled up from there, so a call site only ever states the compact (phone) size and the
 * token decides what it becomes on a larger screen.
 *
 * Mirrors [GameColors]: tokens are exposed as [Composable]-getters, so they react to the current
 * configuration and stay correct after a fold, rotation or window resize.
 */
object GameDimens {
    /**
     * Whether the app is running on a tablet-sized screen ([TabletSmallestWidthDp] and wider).
     *
     * Reads the smallest width of the screen rather than the current one, so rotating a phone
     * into landscape does not make it look like a tablet.
     */
    val isTabletScreen: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalConfiguration.current.smallestScreenWidthDp >= TabletSmallestWidthDp

    /**
     * Adapts a button dimension to the current screen.
     *
     * @param compact the size the dimension has on a phone.
     * @return [compact] on phones, and the value enlarged by [TabletButtonScale] on tablets.
     */
    @Composable
    @ReadOnlyComposable
    fun buttonSize(compact: Dp): Dp = if (isTabletScreen) compact * TabletButtonScale else compact
}
