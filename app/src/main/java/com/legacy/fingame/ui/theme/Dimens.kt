package com.legacy.fingame.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
 * Height, in dp, under which a screen counts as a short one. This is the 480dp breakpoint Android
 * itself splits compact screen heights from the rest at, so the layouts change shape exactly where
 * the platform says the height has run out — which on a phone is the moment it is turned on its
 * side.
 */
private const val ShortScreenHeightDp = 480

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
     * Widest a screen's own content is laid out, however wide the window is; what is left over goes
     * to the margins on either side.
     *
     * A line of text or a row of cards spread across a tablet stops being read in one glance, and a
     * game played on a phone should not turn into a spreadsheet on a bigger screen. The same number
     * for every screen is the point of it: the shop, the budget and the log line up with each other
     * instead of each ending somewhere else.
     */
    val ContentMaxWidth: Dp = 560.dp

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
     * Whether the app is running on a screen with next to no height to give: a phone held
     * sideways, a flattened freeform window — anything under [ShortScreenHeightDp].
     *
     * Reads the height the screen has right now and not the smallest one it can have, unlike
     * [isTabletScreen]: this is about the shape the window is in at the moment, so turning the
     * device does change the answer, which is the whole point of asking.
     *
     * A screen like that has width to spare and none to spare vertically, so the screens that
     * stack their content into a column lay it out in a row instead.
     */
    val isShortScreen: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalConfiguration.current.screenHeightDp < ShortScreenHeightDp

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
